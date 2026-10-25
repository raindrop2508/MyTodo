package com.gordon.mypotato.viewmodel

import android.os.CountDownTimer
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.PomodoroPhase
import com.gordon.mypotato.domain.PomodoroPrompt
import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.PomodoroTimerLogic
import com.gordon.mypotato.domain.SessionStatus
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PomodoroUiState(
    val task: Task? = null,
    val step: TaskStep? = null,
    val currentPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val timeLeftMs: Long = 0,
    val totalTimeMs: Long = 0,
    val isRunning: Boolean = false,
    val cycleCount: Int = 0,
    val sessionId: Long = -1,
    val sessionStartedAtSec: Long = 0,
    val pauseStartTimeMs: Long = 0,
    val totalPausedDurationSec: Long = 0,
    val prompt: PomodoroPrompt? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isValidTask: Boolean = true
) {
    val isIdle: Boolean
        get() = sessionId == -1L && !isRunning && prompt == null

    val hasActiveSession: Boolean
        get() = sessionId != -1L && prompt == null
}

class PomodoroViewModel(
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val focusMinutes: Int = 25,
    private val shortBreakMinutes: Int = 5,
    private val longBreakMinutes: Int = 15,
    private val longBreakInterval: Int = 4
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var currentTaskId: Long = -1
    private var currentStepId: Long? = null
    private val sessionMutex = Mutex()

    fun loadTask(taskId: Long) {
        currentTaskId = taskId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                taskRepository.getTasks(),
                taskRepository.getStepsByTaskId(taskId)
            ) { tasks, steps ->
                val task = tasks.find { it.id == taskId }
                val step = steps.find { it.id == currentStepId }
                val isValid = task?.isLongTask() == true
                val phase = _uiState.value.currentPhase
                val durationMs = if (isValid) getPhaseDurationMs(phase) else 0L
                val preserveTimerUi =
                    _uiState.value.sessionId != -1L || _uiState.value.prompt != null

                _uiState.value.copy(
                    task = task,
                    step = step,
                    timeLeftMs = if (preserveTimerUi) _uiState.value.timeLeftMs else durationMs,
                    totalTimeMs = if (preserveTimerUi) _uiState.value.totalTimeMs else durationMs,
                    isLoading = false,
                    isValidTask = isValid,
                    errorMessage = null,
                    prompt = _uiState.value.prompt,
                    currentPhase = _uiState.value.currentPhase,
                    isRunning = _uiState.value.isRunning,
                    cycleCount = _uiState.value.cycleCount,
                    sessionId = _uiState.value.sessionId,
                    sessionStartedAtSec = _uiState.value.sessionStartedAtSec,
                    pauseStartTimeMs = _uiState.value.pauseStartTimeMs,
                    totalPausedDurationSec = _uiState.value.totalPausedDurationSec
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun setStep(stepId: Long?) {
        currentStepId = stepId
        if (currentTaskId != -1L) {
            loadTask(currentTaskId)
        }
    }

    /**
     * 从持久化活动会话恢复 UI，并按状态续跑或保持暂停。
     *
     * 仅在用户冷启动确认「继续」后调用；不在 App 启动时静默恢复。
     */
    fun resumeActiveSession(sessionId: Long) {
        if (sessionId <= 0L) return

        viewModelScope.launch {
            sessionMutex.withLock {
                val session = pomodoroRepository.getSessionById(sessionId) ?: return@withLock
                if (!session.isActive()) return@withLock

                currentTaskId = session.taskId
                currentStepId = session.stepId

                val phase = session.getPhase()
                val plannedMs = session.plannedDurationMs.takeIf { it > 0L }
                    ?: getPhaseDurationMs(phase)
                val nowMs = System.currentTimeMillis()
                val remainingMs = PomodoroTimerLogic.estimateRemainingMs(session, nowMs)

                _uiState.value = _uiState.value.copy(
                    currentPhase = phase,
                    timeLeftMs = remainingMs,
                    totalTimeMs = plannedMs,
                    isRunning = false,
                    cycleCount = session.cycles,
                    sessionId = session.id,
                    sessionStartedAtSec = session.startedAt,
                    pauseStartTimeMs = if (session.isPaused()) {
                        session.pauseStartedAtEpochMs.takeIf { it > 0L } ?: nowMs
                    } else {
                        0L
                    },
                    totalPausedDurationSec = session.pausedDurationSec.coerceAtLeast(0L),
                    prompt = null,
                    isLoading = true,
                    errorMessage = null
                )

                if (remainingMs <= 0L) {
                    finishPhaseWithPromptLocked()
                } else if (session.isInProgress()) {
                    val targetEnd = nowMs + remainingMs
                    pomodoroRepository.updateTimerState(
                        id = session.id,
                        status = SessionStatus.IN_PROGRESS,
                        pausedDurationSec = session.pausedDurationSec.coerceAtLeast(0L),
                        targetEndEpochMs = targetEnd,
                        remainingMsWhenPaused = 0,
                        pauseStartedAtEpochMs = 0
                    )
                    _uiState.value = _uiState.value.copy(
                        isRunning = true,
                        timeLeftMs = remainingMs,
                        pauseStartTimeMs = 0
                    )
                    startCountDown(remainingMs)
                }
            }

            loadTask(currentTaskId)
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        if (!_uiState.value.isValidTask) return
        if (_uiState.value.prompt != null) return

        viewModelScope.launch {
            sessionMutex.withLock {
                startTimerLocked()
            }
        }
    }

    private suspend fun startTimerLocked() {
        val currentState = _uiState.value
        if (currentState.isRunning || !currentState.isValidTask || currentState.prompt != null) return

        val phase = currentState.currentPhase
        val timeLeftMs = currentState.timeLeftMs.takeIf { it > 0 }
            ?: getPhaseDurationMs(phase)
        val nowMs = System.currentTimeMillis()

        if (currentState.sessionId == -1L) {
            interruptOtherActiveSessions()
            val targetEnd = nowMs + timeLeftMs
            val plannedMs = getPhaseDurationMs(phase)
            val session = PomodoroSession(
                id = 0,
                taskId = currentTaskId,
                stepId = currentStepId,
                startedAt = nowMs / 1000,
                endedAt = null,
                focusDurationSec = if (phase == PomodoroPhase.FOCUS) plannedMs / 1000 else 0,
                breakDurationSec = if (phase != PomodoroPhase.FOCUS) plannedMs / 1000 else 0,
                pausedDurationSec = 0,
                cycles = currentState.cycleCount,
                status = SessionStatus.IN_PROGRESS.value,
                phase = phase.value,
                plannedDurationMs = plannedMs,
                targetEndEpochMs = targetEnd,
                remainingMsWhenPaused = 0,
                pauseStartedAtEpochMs = 0
            )
            val newSessionId = pomodoroRepository.addSession(session)
            _uiState.value = currentState.copy(
                sessionId = newSessionId,
                sessionStartedAtSec = session.startedAt,
                timeLeftMs = timeLeftMs,
                totalTimeMs = plannedMs,
                totalPausedDurationSec = 0,
                pauseStartTimeMs = 0,
                isRunning = true,
                prompt = null
            )
            startCountDown(timeLeftMs)
        } else if (currentState.pauseStartTimeMs != 0L) {
            val pauseDurationMs = nowMs - currentState.pauseStartTimeMs
            val newPausedDurationSec =
                currentState.totalPausedDurationSec + (pauseDurationMs / 1000)
            val targetEnd = nowMs + timeLeftMs
            pomodoroRepository.updateTimerState(
                id = currentState.sessionId,
                status = SessionStatus.IN_PROGRESS,
                pausedDurationSec = newPausedDurationSec,
                targetEndEpochMs = targetEnd,
                remainingMsWhenPaused = 0,
                pauseStartedAtEpochMs = 0
            )
            _uiState.value = currentState.copy(
                pauseStartTimeMs = 0,
                totalPausedDurationSec = newPausedDurationSec,
                isRunning = true,
                timeLeftMs = timeLeftMs
            )
            startCountDown(timeLeftMs)
        } else {
            val targetEnd = nowMs + timeLeftMs
            pomodoroRepository.updateTimerState(
                id = currentState.sessionId,
                status = SessionStatus.IN_PROGRESS,
                pausedDurationSec = currentState.totalPausedDurationSec,
                targetEndEpochMs = targetEnd,
                remainingMsWhenPaused = 0,
                pauseStartedAtEpochMs = 0
            )
            _uiState.value = currentState.copy(isRunning = true, timeLeftMs = timeLeftMs)
            startCountDown(timeLeftMs)
        }
    }

    fun pauseTimer() {
        if (!_uiState.value.isRunning) return
        if (_uiState.value.prompt != null) return
        countDownTimer?.cancel()
        countDownTimer = null

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val sessionId = currentState.sessionId
                val nowMs = System.currentTimeMillis()
                if (sessionId != -1L) {
                    pomodoroRepository.updateTimerState(
                        id = sessionId,
                        status = SessionStatus.PAUSED,
                        pausedDurationSec = currentState.totalPausedDurationSec,
                        targetEndEpochMs = null,
                        remainingMsWhenPaused = currentState.timeLeftMs,
                        pauseStartedAtEpochMs = nowMs
                    )
                }
                _uiState.value = currentState.copy(
                    isRunning = false,
                    pauseStartTimeMs = nowMs
                )
            }
        }
    }

    /**
     * 专注进行中/暂停时「结束本次」：计入已走时长，回到空闲并清零轮次。
     */
    fun endWorkSession() {
        if (_uiState.value.prompt != null) return
        if (_uiState.value.currentPhase != PomodoroPhase.FOCUS) return
        if (_uiState.value.sessionId == -1L) return

        countDownTimer?.cancel()
        countDownTimer = null

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                if (currentState.sessionId != -1L) {
                    completeFocusSessionLocked(currentState)
                }
                applyIdleFocusLocked(cycleCount = 0)
            }
        }
    }

    /**
     * 休息进行中/暂停时「提前结束休息」：打断休息会话，弹出下一轮/结束。
     */
    fun endBreakEarly() {
        if (_uiState.value.prompt != null) return
        if (_uiState.value.currentPhase == PomodoroPhase.FOCUS) return
        if (_uiState.value.sessionId == -1L) return

        countDownTimer?.cancel()
        countDownTimer = null

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                if (currentState.sessionId != -1L) {
                    pomodoroRepository.interruptSession(
                        currentState.sessionId,
                        System.currentTimeMillis() / 1000
                    )
                }
                _uiState.value = currentState.copy(
                    isRunning = false,
                    sessionId = -1,
                    sessionStartedAtSec = 0,
                    pauseStartTimeMs = 0,
                    totalPausedDurationSec = 0,
                    timeLeftMs = 0,
                    prompt = PomodoroPrompt.AFTER_BREAK
                )
            }
        }
    }

    /** 专注到点弹窗：去休息并立即开跑。 */
    fun confirmStartBreak() {
        if (_uiState.value.prompt != PomodoroPrompt.AFTER_FOCUS) return

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val breakPhase = PomodoroTimerLogic.determineNextPhase(
                    currentPhase = PomodoroPhase.FOCUS,
                    completedFocusCycles = currentState.cycleCount,
                    longBreakInterval = longBreakInterval
                )
                val durationMs = getPhaseDurationMs(breakPhase)
                _uiState.value = currentState.copy(
                    currentPhase = breakPhase,
                    timeLeftMs = durationMs,
                    totalTimeMs = durationMs,
                    isRunning = false,
                    sessionId = -1,
                    sessionStartedAtSec = 0,
                    pauseStartTimeMs = 0,
                    totalPausedDurationSec = 0,
                    prompt = null
                )
                startTimerLocked()
            }
        }
    }

    /** 休息结束后弹窗：下一轮专注并立即开跑。 */
    fun confirmStartNextFocus() {
        if (_uiState.value.prompt != PomodoroPrompt.AFTER_BREAK) return

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val durationMs = getPhaseDurationMs(PomodoroPhase.FOCUS)
                _uiState.value = currentState.copy(
                    currentPhase = PomodoroPhase.FOCUS,
                    timeLeftMs = durationMs,
                    totalTimeMs = durationMs,
                    isRunning = false,
                    sessionId = -1,
                    sessionStartedAtSec = 0,
                    pauseStartTimeMs = 0,
                    totalPausedDurationSec = 0,
                    prompt = null
                )
                startTimerLocked()
            }
        }
    }

    /** 弹窗「结束本次」：回到空闲，轮次清零。 */
    fun confirmEndToIdle() {
        if (_uiState.value.prompt == null) return

        viewModelScope.launch {
            sessionMutex.withLock {
                applyIdleFocusLocked(cycleCount = 0)
            }
        }
    }

    private fun startCountDown(timeLeftMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.value = _uiState.value.copy(
                    timeLeftMs = millisUntilFinished,
                    isRunning = true
                )
            }

            override fun onFinish() {
                viewModelScope.launch {
                    sessionMutex.withLock {
                        finishPhaseWithPromptLocked()
                    }
                }
            }
        }.start()
    }

    private suspend fun finishPhaseWithPromptLocked() {
        val currentState = _uiState.value
        val phase = currentState.currentPhase
        countDownTimer?.cancel()
        countDownTimer = null

        if (phase == PomodoroPhase.FOCUS) {
            completeFocusSessionLocked(currentState)
            val newCycleCount = currentState.cycleCount + 1
            _uiState.value = currentState.copy(
                timeLeftMs = 0,
                isRunning = false,
                cycleCount = newCycleCount,
                sessionId = -1,
                sessionStartedAtSec = 0,
                pauseStartTimeMs = 0,
                totalPausedDurationSec = 0,
                prompt = PomodoroPrompt.AFTER_FOCUS
            )
        } else {
            if (currentState.sessionId != -1L) {
                pomodoroRepository.completeSession(
                    id = currentState.sessionId,
                    status = SessionStatus.COMPLETED,
                    endedAt = System.currentTimeMillis() / 1000,
                    focusDurationSec = 0,
                    breakDurationSec = currentState.totalTimeMs / 1000,
                    pausedDurationSec = currentState.totalPausedDurationSec,
                    cycles = currentState.cycleCount
                )
            }
            _uiState.value = currentState.copy(
                timeLeftMs = 0,
                isRunning = false,
                sessionId = -1,
                sessionStartedAtSec = 0,
                pauseStartTimeMs = 0,
                totalPausedDurationSec = 0,
                prompt = PomodoroPrompt.AFTER_BREAK
            )
        }
    }

    private suspend fun completeFocusSessionLocked(currentState: PomodoroUiState) {
        val sessionId = currentState.sessionId
        if (sessionId == -1L) return

        val endedAtSec = System.currentTimeMillis() / 1000
        val startedAtSec = currentState.sessionStartedAtSec.takeIf { it > 0 }
            ?: (endedAtSec - (currentState.totalTimeMs - currentState.timeLeftMs) / 1000)
        val plannedFocusSec = currentState.totalTimeMs / 1000
        val actualFocusSec = PomodoroTimerLogic.calculateActualFocusSec(
            startedAtSec = startedAtSec,
            endedAtSec = endedAtSec,
            pausedDurationSec = currentState.totalPausedDurationSec,
            plannedFocusSec = plannedFocusSec
        )
        pomodoroRepository.completeSession(
            sessionId,
            SessionStatus.COMPLETED,
            endedAtSec,
            actualFocusSec,
            0,
            currentState.totalPausedDurationSec,
            currentState.cycleCount + 1
        )
        if (currentTaskId != -1L) {
            taskRepository.addFocusDuration(currentTaskId, currentStepId, actualFocusSec)
        }
    }

    private fun applyIdleFocusLocked(cycleCount: Int) {
        val durationMs = getPhaseDurationMs(PomodoroPhase.FOCUS)
        _uiState.value = _uiState.value.copy(
            currentPhase = PomodoroPhase.FOCUS,
            timeLeftMs = durationMs,
            totalTimeMs = durationMs,
            isRunning = false,
            cycleCount = cycleCount,
            sessionId = -1,
            sessionStartedAtSec = 0,
            pauseStartTimeMs = 0,
            totalPausedDurationSec = 0,
            prompt = null
        )
    }

    private suspend fun interruptOtherActiveSessions() {
        val active = pomodoroRepository.getAllActiveSessions()
        val nowSec = System.currentTimeMillis() / 1000
        active.forEach { session ->
            pomodoroRepository.interruptSession(session.id, nowSec)
        }
    }

    private fun getPhaseDurationMs(phase: PomodoroPhase): Long {
        return when (phase) {
            PomodoroPhase.FOCUS -> focusMinutes.toLong() * 60 * 1000
            PomodoroPhase.SHORT_BREAK -> shortBreakMinutes.toLong() * 60 * 1000
            PomodoroPhase.LONG_BREAK -> longBreakMinutes.toLong() * 60 * 1000
        }
    }

    fun markStepAsCompleted() {
        currentStepId?.let { stepId ->
            viewModelScope.launch {
                taskRepository.updateStepStatus(stepId, StepStatus.COMPLETED)
            }
        }
    }

    fun markTaskAsCompleted() {
        if (currentTaskId != -1L) {
            viewModelScope.launch {
                taskRepository.updateTaskStatus(currentTaskId, TaskStatus.COMPLETED)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        countDownTimer = null
    }

    @VisibleForTesting
    internal fun replaceUiStateForTest(state: PomodoroUiState) {
        _uiState.value = state
    }

    @VisibleForTesting
    internal fun setTaskContextForTest(taskId: Long, stepId: Long? = null) {
        currentTaskId = taskId
        currentStepId = stepId
    }
}

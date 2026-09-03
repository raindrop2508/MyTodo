package com.gordon.mypotato.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.PomodoroPhase
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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isValidTask: Boolean = true
)

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
                // Do not silently restore active countdown; cold-start dialog settles orphans.
                val keepRunningUi = _uiState.value.sessionId != -1L && _uiState.value.isRunning

                _uiState.value.copy(
                    task = task,
                    step = step,
                    timeLeftMs = if (keepRunningUi) _uiState.value.timeLeftMs else durationMs,
                    totalTimeMs = if (keepRunningUi) _uiState.value.totalTimeMs else durationMs,
                    isLoading = false,
                    isValidTask = isValid,
                    errorMessage = if (!isValid) "只有长时任务可以启动番茄钟" else null
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

    fun startTimer() {
        if (_uiState.value.isRunning) return
        if (!_uiState.value.isValidTask) return

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
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
                        isRunning = true
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
        }
    }

    fun pauseTimer() {
        if (!_uiState.value.isRunning) return
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

    fun resetTimer() {
        countDownTimer?.cancel()
        countDownTimer = null

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val sessionId = currentState.sessionId
                if (sessionId != -1L) {
                    pomodoroRepository.interruptSession(
                        sessionId,
                        System.currentTimeMillis() / 1000
                    )
                }
                val phase = currentState.currentPhase
                val durationMs = getPhaseDurationMs(phase)
                _uiState.value = currentState.copy(
                    timeLeftMs = durationMs,
                    totalTimeMs = durationMs,
                    isRunning = false,
                    sessionId = -1,
                    sessionStartedAtSec = 0,
                    pauseStartTimeMs = 0,
                    totalPausedDurationSec = 0
                )
            }
        }
    }

    fun skipToNextPhase() {
        countDownTimer?.cancel()
        countDownTimer = null
        viewModelScope.launch {
            sessionMutex.withLock {
                handlePhaseCompleteLocked()
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
                        handlePhaseCompleteLocked()
                    }
                }
            }
        }.start()
    }

    private suspend fun handlePhaseCompleteLocked() {
        val currentState = _uiState.value
        val phase = currentState.currentPhase
        countDownTimer?.cancel()
        countDownTimer = null

        if (phase == PomodoroPhase.FOCUS) {
            completeFocusSessionLocked(currentState)
        } else if (currentState.sessionId != -1L) {
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

        val newCycleCount =
            if (phase == PomodoroPhase.FOCUS) currentState.cycleCount + 1 else currentState.cycleCount
        val nextPhase = PomodoroTimerLogic.determineNextPhase(
            currentPhase = phase,
            completedFocusCycles = newCycleCount,
            longBreakInterval = longBreakInterval
        )
        val nextDuration = getPhaseDurationMs(nextPhase)

        _uiState.value = currentState.copy(
            currentPhase = nextPhase,
            timeLeftMs = nextDuration,
            totalTimeMs = nextDuration,
            isRunning = false,
            cycleCount = newCycleCount,
            sessionId = -1,
            sessionStartedAtSec = 0,
            pauseStartTimeMs = 0,
            totalPausedDurationSec = 0
        )
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
    }

    fun endSessionManually() {
        countDownTimer?.cancel()
        countDownTimer = null

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val sessionId = currentState.sessionId
                if (sessionId != -1L && currentState.currentPhase == PomodoroPhase.FOCUS) {
                    completeFocusSessionLocked(currentState)
                } else if (sessionId != -1L) {
                    pomodoroRepository.interruptSession(
                        sessionId,
                        System.currentTimeMillis() / 1000
                    )
                }
                val phase = currentState.currentPhase
                val durationMs = getPhaseDurationMs(phase)
                _uiState.value = currentState.copy(
                    isRunning = false,
                    sessionId = -1,
                    sessionStartedAtSec = 0,
                    pauseStartTimeMs = 0,
                    totalPausedDurationSec = 0,
                    timeLeftMs = durationMs,
                    totalTimeMs = durationMs
                )
            }
        }
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
}

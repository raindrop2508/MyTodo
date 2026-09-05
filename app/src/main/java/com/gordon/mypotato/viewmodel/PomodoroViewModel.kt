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
                // Task/step Flow 刷新时保留活动会话倒计时（含暂停）；冷启动不静默续跑由孤儿弹窗处理。
                val preserveTimerUi = _uiState.value.sessionId != -1L

                _uiState.value.copy(
                    task = task,
                    step = step,
                    timeLeftMs = if (preserveTimerUi) _uiState.value.timeLeftMs else durationMs,
                    totalTimeMs = if (preserveTimerUi) _uiState.value.totalTimeMs else durationMs,
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

    /**
     * 启动番茄钟倒计时。
     *
     * 方法根据当前会话状态分三种情形处理：全新会话、从暂停中恢复、已有会话但非暂停态继续。
     * 所有数据库写入与 UI 状态变更均在 [sessionMutex] 互斥锁内串行执行，避免并发竞态。
     * 目标结束时刻（targetEndEpochMs）一律按「当前墙钟时间 + 剩余时长」重新计算并覆盖写入，
     * 不恢复数据库中的旧值（设计决策：冷启动不静默恢复倒计时）。
     *
     * @throws 无显式异常；协程内异常由 viewModelScope 承载，调用方无需 try-catch。
     */
    fun startTimer() {
        // 卫语句：已在运行或任务非长时任务，直接返回
        if (_uiState.value.isRunning) return
        if (!_uiState.value.isValidTask) return

        viewModelScope.launch {
            sessionMutex.withLock {
                val currentState = _uiState.value
                val phase = currentState.currentPhase
                // 剩余时长优先用内存已有值，否则取当前阶段的默认计划时长
                val timeLeftMs = currentState.timeLeftMs.takeIf { it > 0 }
                    ?: getPhaseDurationMs(phase)
                val nowMs = System.currentTimeMillis()

                // 情形一：sessionId 为 -1，说明尚无 DB 会话记录，需新建会话
                if (currentState.sessionId == -1L) {
                    // 先打断库内其他活动会话，保证全库最多一条活动记录
                    interruptOtherActiveSessions()
                    val targetEnd = nowMs + timeLeftMs
                    val plannedMs = getPhaseDurationMs(phase)
                    val session = PomodoroSession(
                        id = 0,
                        taskId = currentTaskId,
                        stepId = currentStepId,
                        startedAt = nowMs / 1000,
                        endedAt = null,
                        // 专注阶段记录计划专注秒，休息阶段记录休息秒
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
                }
                // 情形二：pauseStartTimeMs 非 0，说明此前已暂停，需从暂停中恢复
                else if (currentState.pauseStartTimeMs != 0L) {
                    // 计算本次暂停时长并累加到累计暂停秒数
                    val pauseDurationMs = nowMs - currentState.pauseStartTimeMs
                    val newPausedDurationSec =
                        currentState.totalPausedDurationSec + (pauseDurationMs / 1000)
                    // 暂停期间目标结束时间已失效，需基于当前时间顺延重算
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
                }
                // 情形三：已有会话且不在暂停态（DB 为 IN_PROGRESS 但内存倒计时未运行），
                // 重新激活计时并刷新目标结束时间
                else {
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

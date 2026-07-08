package com.gordon.mypotato.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.PomodoroSession
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

enum class PomodoroPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

data class PomodoroUiState(
    val task: Task? = null,
    val step: TaskStep? = null,
    val currentPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val timeLeftMs: Long = 0,
    val totalTimeMs: Long = 0,
    val isRunning: Boolean = false,
    val cycleCount: Int = 0,
    val sessionId: Long = -1,
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

    fun loadTask(taskId: Long) {
        if (taskId == currentTaskId) return
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

                _uiState.value.copy(
                    task = task,
                    step = step,
                    timeLeftMs = if (isValid) getPhaseDurationMs(PomodoroPhase.FOCUS) else 0,
                    totalTimeMs = if (isValid) getPhaseDurationMs(PomodoroPhase.FOCUS) else 0,
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

        val currentState = _uiState.value
        val phase = currentState.currentPhase
        val timeLeftMs = currentState.timeLeftMs

        countDownTimer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.value = _uiState.value.copy(
                    timeLeftMs = millisUntilFinished,
                    isRunning = true
                )
            }

            override fun onFinish() {
                handlePhaseComplete()
            }
        }.start()

        if (currentState.sessionId == -1L && phase == PomodoroPhase.FOCUS) {
            createSession()
        }

        _uiState.value = _uiState.value.copy(isRunning = true)
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        val phase = _uiState.value.currentPhase
        _uiState.value = _uiState.value.copy(
            timeLeftMs = getPhaseDurationMs(phase),
            totalTimeMs = getPhaseDurationMs(phase),
            isRunning = false,
            sessionId = -1
        )
    }

    fun skipToNextPhase() {
        countDownTimer?.cancel()
        handlePhaseComplete()
    }

    private fun handlePhaseComplete() {
        val currentState = _uiState.value
        val phase = currentState.currentPhase

        if (phase == PomodoroPhase.FOCUS) {
            completeFocusSession()
        }

        val newCycleCount = if (phase == PomodoroPhase.FOCUS) currentState.cycleCount + 1 else currentState.cycleCount
        val nextPhase = determineNextPhase(newCycleCount)

        _uiState.value = _uiState.value.copy(
            currentPhase = nextPhase,
            timeLeftMs = getPhaseDurationMs(nextPhase),
            totalTimeMs = getPhaseDurationMs(nextPhase),
            isRunning = false,
            cycleCount = newCycleCount,
            sessionId = -1
        )
    }

    private fun completeFocusSession() {
        val sessionId = _uiState.value.sessionId
        if (sessionId != -1L) {
            viewModelScope.launch {
                pomodoroRepository.updateSessionStatus(sessionId, SessionStatus.COMPLETED)
            }
        }
    }

    private fun createSession() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val session = PomodoroSession(
                id = 0,
                taskId = currentTaskId,
                stepId = currentStepId,
                startedAt = System.currentTimeMillis() / 1000,
                endedAt = null,
                focusDurationSec = focusMinutes.toLong() * 60,
                breakDurationSec = 0,
                cycles = currentState.cycleCount,
                status = SessionStatus.IN_PROGRESS.value
            )
            val newSessionId = pomodoroRepository.addSession(session)
            _uiState.value = _uiState.value.copy(sessionId = newSessionId)
        }
    }

    private fun determineNextPhase(cycleCount: Int): PomodoroPhase {
        return if (cycleCount > 0 && cycleCount % longBreakInterval == 0) {
            PomodoroPhase.LONG_BREAK
        } else {
            PomodoroPhase.SHORT_BREAK
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
    }
}
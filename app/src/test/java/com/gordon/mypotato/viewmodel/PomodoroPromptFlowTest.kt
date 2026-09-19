package com.gordon.mypotato.viewmodel

import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskQuery
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.PomodoroPhase
import com.gordon.mypotato.domain.PomodoroPrompt
import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.SessionStatus
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroPromptFlowTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_isIdleWhenNoSessionOrPrompt() {
        val idle = PomodoroUiState(isLoading = false)
        assertTrue(idle.isIdle)
        assertTrue(!idle.hasActiveSession)
    }

    @Test
    fun confirmEndToIdle_clearsPromptAndResetsCycle() = runTest {
        val pomodoroRepo = FakePomodoroRepository()
        val taskRepo = FakeTaskRepository()
        val vm = PomodoroViewModel(taskRepo, pomodoroRepo, focusMinutes = 25)
        vm.replaceUiStateForTest(
            PomodoroUiState(
                currentPhase = PomodoroPhase.FOCUS,
                timeLeftMs = 0,
                totalTimeMs = 25 * 60 * 1000L,
                cycleCount = 3,
                prompt = PomodoroPrompt.AFTER_FOCUS,
                isLoading = false,
                isValidTask = true
            )
        )

        vm.confirmEndToIdle()

        val state = vm.uiState.value
        assertNull(state.prompt)
        assertEquals(0, state.cycleCount)
        assertEquals(PomodoroPhase.FOCUS, state.currentPhase)
        assertTrue(state.isIdle)
    }

    @Test
    fun confirmStartBreak_entersBreakAndStartsSession() = runTest {
        val pomodoroRepo = FakePomodoroRepository()
        val taskRepo = FakeTaskRepository(
            tasks = mutableListOf(sampleTask(1))
        )
        val vm = PomodoroViewModel(
            taskRepo,
            pomodoroRepo,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakInterval = 4
        )
        vm.setTaskContextForTest(1)
        vm.replaceUiStateForTest(
            PomodoroUiState(
                currentPhase = PomodoroPhase.FOCUS,
                timeLeftMs = 0,
                totalTimeMs = 25 * 60 * 1000L,
                cycleCount = 1,
                prompt = PomodoroPrompt.AFTER_FOCUS,
                isLoading = false,
                isValidTask = true
            )
        )

        vm.confirmStartBreak()

        val state = vm.uiState.value
        assertNull(state.prompt)
        assertEquals(PomodoroPhase.SHORT_BREAK, state.currentPhase)
        assertTrue(state.sessionId > 0L)
        assertEquals(1, pomodoroRepo.sessions.size)
        assertEquals(PomodoroPhase.SHORT_BREAK.value, pomodoroRepo.sessions.first().phase)
    }

    @Test
    fun confirmStartNextFocus_startsFocusSessionKeepingCycle() = runTest {
        val pomodoroRepo = FakePomodoroRepository()
        val taskRepo = FakeTaskRepository(tasks = mutableListOf(sampleTask(1)))
        val vm = PomodoroViewModel(taskRepo, pomodoroRepo, focusMinutes = 25)
        vm.setTaskContextForTest(1)
        vm.replaceUiStateForTest(
            PomodoroUiState(
                currentPhase = PomodoroPhase.SHORT_BREAK,
                timeLeftMs = 0,
                totalTimeMs = 5 * 60 * 1000L,
                cycleCount = 2,
                prompt = PomodoroPrompt.AFTER_BREAK,
                isLoading = false,
                isValidTask = true
            )
        )

        vm.confirmStartNextFocus()

        val state = vm.uiState.value
        assertNull(state.prompt)
        assertEquals(PomodoroPhase.FOCUS, state.currentPhase)
        assertEquals(2, state.cycleCount)
        assertTrue(state.sessionId > 0L)
    }

    @Test
    fun endBreakEarly_setsAfterBreakPrompt() = runTest {
        val session = activeBreakSession(id = 9, taskId = 1)
        val pomodoroRepo = FakePomodoroRepository(mutableListOf(session))
        val vm = PomodoroViewModel(FakeTaskRepository(), pomodoroRepo)
        vm.replaceUiStateForTest(
            PomodoroUiState(
                currentPhase = PomodoroPhase.SHORT_BREAK,
                timeLeftMs = 60_000,
                totalTimeMs = 300_000,
                cycleCount = 1,
                sessionId = 9,
                isRunning = true,
                isLoading = false,
                isValidTask = true
            )
        )

        vm.endBreakEarly()

        val state = vm.uiState.value
        assertEquals(PomodoroPrompt.AFTER_BREAK, state.prompt)
        assertEquals(-1L, state.sessionId)
        assertEquals(
            SessionStatus.INTERRUPTED.value,
            pomodoroRepo.sessions.first().status
        )
    }

    private fun sampleTask(id: Long): Task = Task(
        id = id,
        title = "任务",
        content = null,
        note = null,
        taskType = TaskType.LONG.value,
        status = TaskStatus.TODO.value,
        isUrgent = false,
        isImportant = false,
        categoryId = 0,
        createdAt = 0,
        plannedStartAt = null,
        finishedAt = null,
        totalDurationSec = 0
    )

    private fun activeBreakSession(id: Long, taskId: Long): PomodoroSession = PomodoroSession(
        id = id,
        taskId = taskId,
        stepId = null,
        startedAt = 100,
        endedAt = null,
        focusDurationSec = 0,
        breakDurationSec = 300,
        pausedDurationSec = 0,
        cycles = 1,
        status = SessionStatus.IN_PROGRESS.value,
        phase = PomodoroPhase.SHORT_BREAK.value,
        plannedDurationMs = 300_000,
        targetEndEpochMs = System.currentTimeMillis() + 300_000,
        remainingMsWhenPaused = 0,
        pauseStartedAtEpochMs = 0
    )

    private class FakePomodoroRepository(
        val sessions: MutableList<PomodoroSession> = mutableListOf()
    ) : PomodoroRepository {
        override fun getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
            flowOf(sessions.filter { it.taskId == taskId })

        override suspend fun getSessionById(id: Long): PomodoroSession? =
            sessions.find { it.id == id }

        override suspend fun getAllActiveSessions(): List<PomodoroSession> =
            sessions.filter { it.isActive() }

        override suspend fun addSession(session: PomodoroSession): Long {
            val id = (sessions.maxOfOrNull { it.id } ?: 0L) + 1
            sessions.add(session.copy(id = id))
            return id
        }

        override suspend fun updateSession(session: PomodoroSession) {
            val index = sessions.indexOfFirst { it.id == session.id }
            if (index >= 0) sessions[index] = session
        }

        override suspend fun updateSessionStatus(id: Long, status: SessionStatus) {
            updateSession(sessions.first { it.id == id }.copy(status = status.value))
        }

        override suspend fun updateTimerState(
            id: Long,
            status: SessionStatus,
            pausedDurationSec: Long,
            targetEndEpochMs: Long?,
            remainingMsWhenPaused: Long,
            pauseStartedAtEpochMs: Long
        ) {
            val current = sessions.first { it.id == id }
            updateSession(
                current.copy(
                    status = status.value,
                    pausedDurationSec = pausedDurationSec,
                    targetEndEpochMs = targetEndEpochMs,
                    remainingMsWhenPaused = remainingMsWhenPaused,
                    pauseStartedAtEpochMs = pauseStartedAtEpochMs
                )
            )
        }

        override suspend fun completeSession(
            id: Long,
            status: SessionStatus,
            endedAt: Long,
            focusDurationSec: Long,
            breakDurationSec: Long,
            pausedDurationSec: Long,
            cycles: Int
        ) {
            val current = sessions.first { it.id == id }
            updateSession(
                current.copy(
                    status = status.value,
                    endedAt = endedAt,
                    focusDurationSec = focusDurationSec,
                    breakDurationSec = breakDurationSec,
                    pausedDurationSec = pausedDurationSec,
                    cycles = cycles
                )
            )
        }

        override suspend fun interruptSession(id: Long, endedAt: Long) {
            val current = sessions.first { it.id == id }
            updateSession(
                current.copy(
                    status = SessionStatus.INTERRUPTED.value,
                    endedAt = endedAt,
                    targetEndEpochMs = null,
                    remainingMsWhenPaused = 0,
                    pauseStartedAtEpochMs = 0
                )
            )
        }

        override suspend fun deleteSession(id: Long) {
            sessions.removeAll { it.id == id }
        }
    }

    private class FakeTaskRepository(
        val tasks: MutableList<Task> = mutableListOf(),
        val steps: MutableList<TaskStep> = mutableListOf()
    ) : TaskRepository {
        override fun getTasks(): Flow<List<Task>> = MutableStateFlow(tasks)
        override suspend fun getTaskById(id: Long): Task? = tasks.find { it.id == id }
        override fun getTasksByQuery(query: TaskQuery): Flow<List<Task>> = flowOf(tasks)
        override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = flowOf(tasks)
        override fun getTasksByType(taskType: TaskType): Flow<List<Task>> = flowOf(tasks)
        override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = flowOf(tasks)
        override fun getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>> =
            flowOf(tasks)
        override fun getStepsByTaskId(taskId: Long): Flow<List<TaskStep>> =
            flowOf(steps.filter { it.taskId == taskId })
        override suspend fun addTask(task: Task): Long = task.id
        override suspend fun updateTask(task: Task) {
            val i = tasks.indexOfFirst { it.id == task.id }
            if (i >= 0) tasks[i] = task
        }
        override suspend fun deleteTask(id: Long) {}
        override suspend fun updateTaskStatus(id: Long, status: TaskStatus) {}
        override suspend fun addStep(step: TaskStep): Long = step.id
        override suspend fun updateStep(step: TaskStep) {}
        override suspend fun updateStepStatus(id: Long, status: StepStatus) {}
        override suspend fun addFocusDuration(taskId: Long, stepId: Long?, durationSec: Long) {}
        override suspend fun deleteStep(id: Long) {}
    }
}

package com.gordon.mypotato.domain

import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskQuery
import com.gordon.mypotato.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OrphanPomodoroSettlementTest {

    @Test
    fun preparePrompt_returnsNullWhenNoActive() = runBlocking {
        val pomodoroRepo = FakePomodoroRepository()
        val settlement = OrphanPomodoroSettlement(pomodoroRepo, FakeTaskRepository())
        assertNull(settlement.preparePrompt())
    }

    @Test
    fun preparePrompt_keepsLatestAndInterruptsOlder() = runBlocking {
        val older = activeSession(id = 1, startedAt = 100, taskId = 1)
        val latest = activeSession(id = 2, startedAt = 200, taskId = 1)
        val pomodoroRepo = FakePomodoroRepository(mutableListOf(older, latest))
        val taskRepo = FakeTaskRepository(
            tasks = mutableListOf(sampleTask(1, "写作"))
        )
        val settlement = OrphanPomodoroSettlement(pomodoroRepo, taskRepo)

        val prompt = settlement.preparePrompt(nowEpochMs = 250_000)
        assertNotNull(prompt)
        assertEquals(2L, prompt!!.session.id)
        assertEquals(1, prompt.cleanedOlderCount)
        assertEquals("写作", prompt.taskTitle)
        assertEquals(1, pomodoroRepo.sessions.count { it.isActive() })
        assertEquals(
            SessionStatus.INTERRUPTED.value,
            pomodoroRepo.sessions.first { it.id == 1L }.status
        )
    }

    @Test
    fun keepFocusDuration_marksSessionCompletedWithoutTouchingTask() = runBlocking {
        val session = activeSession(id = 5, startedAt = 100, taskId = 9)
        val pomodoroRepo = FakePomodoroRepository(mutableListOf(session))
        val taskRepo = FakeTaskRepository(
            tasks = mutableListOf(sampleTask(9, "学习", TaskStatus.IN_PROGRESS))
        )
        val settlement = OrphanPomodoroSettlement(pomodoroRepo, taskRepo)

        settlement.keepFocusDuration(session, elapsedFocusSec = 120)

        val updated = pomodoroRepo.sessions.first { it.id == 5L }
        assertEquals(SessionStatus.COMPLETED.value, updated.status)
        assertEquals(120L, updated.focusDurationSec)
        assertEquals(TaskStatus.IN_PROGRESS.value, taskRepo.tasks.first().status)
    }

    @Test
    fun discard_marksInterrupted() = runBlocking {
        val session = activeSession(id = 7, startedAt = 100, taskId = 1)
        val pomodoroRepo = FakePomodoroRepository(mutableListOf(session))
        val settlement = OrphanPomodoroSettlement(pomodoroRepo, FakeTaskRepository())

        settlement.discard(session)

        val updated = pomodoroRepo.sessions.first { it.id == 7L }
        assertEquals(SessionStatus.INTERRUPTED.value, updated.status)
        assertFalse(updated.isActive())
    }

    private fun activeSession(id: Long, startedAt: Long, taskId: Long): PomodoroSession {
        return PomodoroSession(
            id = id,
            taskId = taskId,
            stepId = null,
            startedAt = startedAt,
            endedAt = null,
            focusDurationSec = 1500,
            breakDurationSec = 0,
            pausedDurationSec = 0,
            cycles = 0,
            status = SessionStatus.IN_PROGRESS.value,
            phase = PomodoroPhase.FOCUS.value,
            plannedDurationMs = 1_500_000,
            targetEndEpochMs = startedAt * 1000 + 1_500_000,
            remainingMsWhenPaused = 0,
            pauseStartedAtEpochMs = 0
        )
    }

    private fun sampleTask(
        id: Long,
        title: String,
        status: TaskStatus = TaskStatus.TODO
    ): Task {
        return Task(
            id = id,
            title = title,
            content = null,
            note = null,
            taskType = TaskType.LONG.value,
            status = status.value,
            isUrgent = false,
            isImportant = false,
            categoryId = 0,
            createdAt = 0,
            plannedStartAt = null,
            finishedAt = null,
            totalDurationSec = 0
        )
    }

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
                    cycles = cycles,
                    targetEndEpochMs = null,
                    remainingMsWhenPaused = 0,
                    pauseStartedAtEpochMs = 0
                )
            )
        }

        override suspend fun interruptSession(id: Long, endedAt: Long) {
            val current = sessions.first { it.id == id }
            updateSession(
                current.copy(
                    status = SessionStatus.INTERRUPTED.value,
                    endedAt = endedAt,
                    focusDurationSec = 0,
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
        private val steps: List<TaskStep> = emptyList()
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
        override suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
            val task = tasks.firstOrNull { it.id == id } ?: return
            updateTask(task.copy(status = status.value))
        }
        override suspend fun addStep(step: TaskStep): Long = step.id
        override suspend fun updateStep(step: TaskStep) {}
        override suspend fun updateStepStatus(id: Long, status: StepStatus) {}
        override suspend fun deleteStep(id: Long) {}
    }
}

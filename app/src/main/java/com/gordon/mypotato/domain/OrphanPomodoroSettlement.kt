package com.gordon.mypotato.domain

import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * 冷启动时对残留活动番茄钟会话的收尾协调。
 */
class OrphanPomodoroSettlement(
    private val pomodoroRepository: PomodoroRepository,
    private val taskRepository: TaskRepository
) {

    data class Prompt(
        val session: PomodoroSession,
        val taskTitle: String,
        val stepTitle: String?,
        val elapsedFocusSec: Long,
        val cleanedOlderCount: Int,
        val isFocusPhase: Boolean
    )

    /**
     * 清理旧活动行，返回需要用户确认的最新一条；无活动会话返回 null。
     */
    suspend fun preparePrompt(nowEpochMs: Long = System.currentTimeMillis()): Prompt? {
        val active = pomodoroRepository.getAllActiveSessions()
        if (active.isEmpty()) return null

        val sorted = active.sortedWith(
            compareByDescending<PomodoroSession> { it.startedAt }
                .thenByDescending { it.id }
        )
        val candidate = sorted.first()
        val older = sorted.drop(1)
        val nowSec = nowEpochMs / 1000
        older.forEach { pomodoroRepository.interruptSession(it.id, nowSec) }

        val task = taskRepository.getTaskById(candidate.taskId)
        val stepTitle = candidate.stepId?.let { stepId ->
            taskRepository.getStepsByTaskId(candidate.taskId).first()
                .find { it.id == stepId }
                ?.title
        }
        val elapsed = PomodoroTimerLogic.estimateElapsedFocusSec(candidate, nowEpochMs)
        val isFocus = candidate.getPhase() == PomodoroPhase.FOCUS

        return Prompt(
            session = candidate,
            taskTitle = task?.title ?: "未知任务",
            stepTitle = stepTitle,
            elapsedFocusSec = elapsed,
            cleanedOlderCount = older.size,
            isFocusPhase = isFocus
        )
    }

    suspend fun keepFocusDuration(session: PomodoroSession, elapsedFocusSec: Long) {
        val endedAt = System.currentTimeMillis() / 1000
        pomodoroRepository.completeSession(
            id = session.id,
            status = SessionStatus.COMPLETED,
            endedAt = endedAt,
            focusDurationSec = elapsedFocusSec.coerceAtLeast(0L),
            breakDurationSec = session.breakDurationSec,
            pausedDurationSec = session.pausedDurationSec,
            cycles = session.cycles
        )
    }

    suspend fun discard(session: PomodoroSession) {
        pomodoroRepository.interruptSession(
            id = session.id,
            endedAt = System.currentTimeMillis() / 1000
        )
    }
}

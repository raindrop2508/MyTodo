package com.gordon.mypotato.domain

/**
 * 番茄钟会话实体
 *
 * 记录一次番茄钟计时会话，关联任务和步骤，支持工作时段、休息时段、轮次数等信息。
 */
data class PomodoroSession(
    val id: Long,
    val taskId: Long,
    val stepId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val focusDurationSec: Long,
    val breakDurationSec: Long,
    val pausedDurationSec: Long,
    val cycles: Int,
    val status: Int,
    val phase: Int = PomodoroPhase.FOCUS.value,
    val plannedDurationMs: Long = 0L,
    val targetEndEpochMs: Long? = null,
    val remainingMsWhenPaused: Long = 0L,
    val pauseStartedAtEpochMs: Long = 0L
) {
    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED.value

    fun isInProgress(): Boolean = status == SessionStatus.IN_PROGRESS.value

    fun isInterrupted(): Boolean = status == SessionStatus.INTERRUPTED.value

    fun isPaused(): Boolean = status == SessionStatus.PAUSED.value

    fun isActive(): Boolean = isInProgress() || isPaused()

    /**
     * 获取实际专注时长。完成态下 [focusDurationSec] 已存实际值，不再二次扣暂停。
     */
    fun getActualFocusDurationSec(): Long {
        return if (isCompleted()) {
            focusDurationSec.coerceAtLeast(0L)
        } else {
            PomodoroTimerLogic.estimateElapsedFocusSec(this)
        }
    }

    fun getPhase(): PomodoroPhase = PomodoroPhase.fromValue(phase)
}

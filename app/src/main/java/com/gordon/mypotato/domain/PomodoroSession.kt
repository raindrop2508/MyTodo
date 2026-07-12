package com.gordon.mypotato.domain

/**
 * 番茄钟会话实体
 *
 * 记录一次番茄钟计时会话，关联任务和步骤，支持工作时段、休息时段、轮次数等信息。
 *
 * @property id 会话唯一标识
 * @property taskId 关联任务 ID
 * @property stepId 关联步骤 ID（在步骤上下文启动时填写，可选）
 * @property startedAt 会话开始时间戳（秒）
 * @property endedAt 会话结束时间戳（秒，可选）
 * @property focusDurationSec 工作时段时长（秒），默认为 0
 * @property breakDurationSec 休息时段时长（秒），默认为 0
 * @property cycles 完成的番茄钟轮次数，默认为 0
 * @property status 会话状态：0=进行中(IN_PROGRESS)，1=完成(COMPLETED)，2=中断(INTERRUPTED)，默认为 0
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
    val status: Int
) {
    /**
     * 判断会话是否已完成
     */
    fun isCompleted(): Boolean {
        return status == SessionStatus.COMPLETED.value
    }

    /**
     * 判断会话是否正在进行中
     */
    fun isInProgress(): Boolean {
        return status == SessionStatus.IN_PROGRESS.value
    }

    /**
     * 判断会话是否已中断
     */
    fun isInterrupted(): Boolean {
        return status == SessionStatus.INTERRUPTED.value
    }

    /**
     * 判断会话是否已暂停
     */
    fun isPaused(): Boolean {
        return status == SessionStatus.PAUSED.value
    }

    /**
     * 获取实际专注时长（总专注时长 - 暂停时长）
     */
    fun getActualFocusDurationSec(): Long {
        return focusDurationSec - pausedDurationSec
    }
}
package com.gordon.mypotato.domain

/**
 * 任务步骤实体
 *
 * 表示任务下的一个子步骤，支持排序、完成状态追踪等功能。
 *
 * @property id 步骤唯一标识
 * @property taskId 所属任务 ID，关联 Task
 * @property title 步骤名称（必填）
 * @property sortOrder 排序序号，用于步骤排序
 * @property status 步骤状态：0=未完成(TODO)，1=已完成(COMPLETED)，默认为 0
 * @property completedAt 步骤完成时间戳（秒，可选）
 * @property spentDurationSec 步骤累计时长（秒），默认为 0
 * @property createdAt 创建时间戳（秒）
 */
data class TaskStep(
    val id: Long,
    val taskId: Long,
    val title: String,
    val sortOrder: Int,
    val status: Int,
    val completedAt: Long?,
    val spentDurationSec: Long,
    val createdAt: Long
) {
    /**
     * 判断步骤是否已完成
     */
    fun isCompleted(): Boolean {
        return status == StepStatus.COMPLETED.value
    }
}
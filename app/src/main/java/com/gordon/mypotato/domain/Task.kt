package com.gordon.mypotato.domain

/**
 * 任务实体
 *
 * 表示一个待办任务，支持四象限分类、单次/长时任务类型、状态流转等功能。
 *
 * @property id 任务唯一标识
 * @property title 任务名称（必填）
 * @property content 任务内容描述（可选）
 * @property note 备注信息（可选）
 * @property taskType 任务类型：0=单次任务(ONCE)，1=长时任务(LONG)
 * @property status 任务状态：0=未开始(TODO)，1=进行中(IN_PROGRESS)，2=已完成(COMPLETED)，3=已归档(ARCHIVED)
 * @property isUrgent 是否紧急（四象限维度），默认为 false
 * @property isImportant 是否重要（四象限维度），默认为 false
 * @property categoryId 所属分类 ID，关联 Category，0 表示未分类
 * @property createdAt 创建时间戳（秒）
 * @property plannedStartAt 计划开始时间戳（秒，可选）
 * @property finishedAt 实际完成时间戳（秒，可选）
 * @property totalDurationSec 累计用时（秒），长时任务有效，默认为 0
 */
data class Task(
    val id: Long,
    val title: String,
    val content: String?,
    val note: String?,
    val taskType: Int,
    val status: Int,
    val isUrgent: Boolean,
    val isImportant: Boolean,
    val categoryId: Long,
    val createdAt: Long,
    val plannedStartAt: Long?,
    val finishedAt: Long?,
    val totalDurationSec: Long
) {
    /**
     * 判断是否可以启动番茄钟
     * 仅长时任务可以启动番茄钟
     */
    fun canStartPomodoro(): Boolean {
        return taskType == TaskType.LONG.value
    }

    /**
     * 判断是否为长时任务
     */
    fun isLongTask(): Boolean {
        return taskType == TaskType.LONG.value
    }

    /**
     * 判断是否为单次任务
     */
    fun isOnceTask(): Boolean {
        return taskType == TaskType.ONCE.value
    }

    /**
     * 判断任务是否已完成
     */
    fun isCompleted(): Boolean {
        return status == TaskStatus.COMPLETED.value
    }
}
package com.gordon.mypotato.domain

/**
 * 任务状态枚举
 *
 * 定义任务的四种状态：未开始、进行中、已完成、已归档。
 *
 * @property value 枚举值，用于数据库存储和网络传输
 */
enum class TaskStatus(val value: Int) {
    /** 未开始 */
    TODO(0),
    /** 进行中 */
    IN_PROGRESS(1),
    /** 已完成 */
    COMPLETED(2),
    /** 已归档（可选） */
    ARCHIVED(3);

    companion object {
        /**
         * 根据数值获取对应的枚举值
         * @param value 枚举数值
         * @return 对应的 TaskStatus，默认为 TODO
         */
        fun fromValue(value: Int): TaskStatus {
            return values().firstOrNull { it.value == value } ?: TODO
        }
    }
}
package com.gordon.mypotato.domain

/**
 * 任务类型枚举
 *
 * 定义任务的两种类型：单次任务和长时任务。
 * 仅长时任务可以启动番茄钟。
 *
 * @property value 枚举值，用于数据库存储和网络传输
 */
enum class TaskType(val value: Int) {
    /** 单次任务（如：买东西、回消息），不可启动番茄钟 */
    ONCE(0),
    /** 长时任务（如：背单词），可启动番茄钟 */
    LONG(1);

    companion object {
        /**
         * 根据数值获取对应的枚举值
         * @param value 枚举数值
         * @return 对应的 TaskType，默认为 ONCE
         */
        fun fromValue(value: Int): TaskType {
            return values().firstOrNull { it.value == value } ?: ONCE
        }
    }
}
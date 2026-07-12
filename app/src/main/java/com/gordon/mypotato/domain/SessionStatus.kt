package com.gordon.mypotato.domain

/**
 * 番茄钟会话状态枚举
 *
 * 定义番茄钟会话的三种状态：进行中、完成、中断。
 *
 * @property value 枚举值，用于数据库存储和网络传输
 */
enum class SessionStatus(val value: Int) {
    /** 进行中 */
    IN_PROGRESS(0),
    /** 完成 */
    COMPLETED(1),
    /** 中断 */
    INTERRUPTED(2),
    /** 暂停 */
    PAUSED(3);

    companion object {
        /**
         * 根据数值获取对应的枚举值
         * @param value 枚举数值
         * @return 对应的 SessionStatus，默认为 IN_PROGRESS
         */
        fun fromValue(value: Int): SessionStatus {
            return values().firstOrNull { it.value == value } ?: IN_PROGRESS
        }
    }
}
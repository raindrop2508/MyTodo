package com.gordon.mypotato.domain

/**
 * 步骤状态枚举
 *
 * 定义任务步骤的两种状态：未完成和已完成。
 *
 * @property value 枚举值，用于数据库存储和网络传输
 */
enum class StepStatus(val value: Int) {
    /** 未完成 */
    TODO(0),
    /** 已完成 */
    COMPLETED(1);

    companion object {
        /**
         * 根据数值获取对应的枚举值
         * @param value 枚举数值
         * @return 对应的 StepStatus，默认为 TODO
         */
        fun fromValue(value: Int): StepStatus {
            return values().firstOrNull { it.value == value } ?: TODO
        }
    }
}
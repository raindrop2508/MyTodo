package com.gordon.mypotato.domain

/**
 * 番茄钟计时阶段。
 */
enum class PomodoroPhase(val value: Int) {
    FOCUS(0),
    SHORT_BREAK(1),
    LONG_BREAK(2);

    companion object {
        fun fromValue(value: Int): PomodoroPhase {
            return values().firstOrNull { it.value == value } ?: FOCUS
        }
    }
}

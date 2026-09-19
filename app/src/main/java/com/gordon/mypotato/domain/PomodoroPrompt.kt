package com.gordon.mypotato.domain

/**
 * 阶段结束后需用户确认的弹窗类型（无连续模式）。
 */
enum class PomodoroPrompt {
    /** 专注自然结束后：去休息 / 结束本次 */
    AFTER_FOCUS,

    /** 休息自然结束或提前结束后：下一轮专注 / 结束本次 */
    AFTER_BREAK
}

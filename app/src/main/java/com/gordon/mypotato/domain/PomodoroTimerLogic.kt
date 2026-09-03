package com.gordon.mypotato.domain

/**
 * 番茄钟阶段流转与专注时长计算（纯逻辑，便于单测）。
 */
object PomodoroTimerLogic {

    fun determineNextPhase(
        currentPhase: PomodoroPhase,
        completedFocusCycles: Int,
        longBreakInterval: Int
    ): PomodoroPhase {
        return when (currentPhase) {
            PomodoroPhase.FOCUS -> {
                if (completedFocusCycles > 0 && completedFocusCycles % longBreakInterval == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK,
            PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }
    }

    /**
     * 实际专注秒数：墙钟流逝减去暂停，并夹在 [0, plannedFocusSec]。
     */
    fun calculateActualFocusSec(
        startedAtSec: Long,
        endedAtSec: Long,
        pausedDurationSec: Long,
        plannedFocusSec: Long
    ): Long {
        val wallElapsedSec = (endedAtSec - startedAtSec).coerceAtLeast(0L)
        val raw = wallElapsedSec - pausedDurationSec.coerceAtLeast(0L)
        return raw.coerceIn(0L, plannedFocusSec.coerceAtLeast(0L))
    }

    /**
     * 从活动会话推算已专注秒数（冷启动收尾弹窗用）。
     */
    fun estimateElapsedFocusSec(
        session: PomodoroSession,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Long {
        if (session.phase != PomodoroPhase.FOCUS.value) return 0L

        val plannedFocusSec = if (session.plannedDurationMs > 0L) {
            session.plannedDurationMs / 1000
        } else {
            session.focusDurationSec
        }

        val pausedSec = session.pausedDurationSec.coerceAtLeast(0L)
        val status = SessionStatus.fromValue(session.status)

        return when (status) {
            SessionStatus.PAUSED -> {
                val remainingMs = session.remainingMsWhenPaused.coerceAtLeast(0L)
                val plannedMs = session.plannedDurationMs.takeIf { it > 0L }
                    ?: (plannedFocusSec * 1000)
                val elapsedMs = (plannedMs - remainingMs).coerceAtLeast(0L)
                (elapsedMs / 1000).coerceIn(0L, plannedFocusSec.coerceAtLeast(0L))
            }
            SessionStatus.IN_PROGRESS -> {
                val nowSec = nowEpochMs / 1000
                var effectivePaused = pausedSec
                if (session.pauseStartedAtEpochMs > 0L) {
                    // Should not happen for IN_PROGRESS, but be defensive
                    effectivePaused += ((nowEpochMs - session.pauseStartedAtEpochMs) / 1000)
                        .coerceAtLeast(0L)
                }
                calculateActualFocusSec(
                    startedAtSec = session.startedAt,
                    endedAtSec = nowSec,
                    pausedDurationSec = effectivePaused,
                    plannedFocusSec = plannedFocusSec
                )
            }
            else -> 0L
        }
    }

    fun formatDurationSec(totalSec: Long): String {
        val safe = totalSec.coerceAtLeast(0L)
        val minutes = safe / 60
        val seconds = safe % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

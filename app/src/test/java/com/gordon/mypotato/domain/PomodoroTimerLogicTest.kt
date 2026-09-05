package com.gordon.mypotato.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroTimerLogicTest {

    @Test
    fun determineNextPhase_focusToShortBreak() {
        val next = PomodoroTimerLogic.determineNextPhase(
            currentPhase = PomodoroPhase.FOCUS,
            completedFocusCycles = 1,
            longBreakInterval = 4
        )
        assertEquals(PomodoroPhase.SHORT_BREAK, next)
    }

    @Test
    fun determineNextPhase_focusToLongBreakWhenIntervalReached() {
        val next = PomodoroTimerLogic.determineNextPhase(
            currentPhase = PomodoroPhase.FOCUS,
            completedFocusCycles = 4,
            longBreakInterval = 4
        )
        assertEquals(PomodoroPhase.LONG_BREAK, next)
    }

    @Test
    fun determineNextPhase_shortBreakReturnsFocus() {
        val next = PomodoroTimerLogic.determineNextPhase(
            currentPhase = PomodoroPhase.SHORT_BREAK,
            completedFocusCycles = 1,
            longBreakInterval = 4
        )
        assertEquals(PomodoroPhase.FOCUS, next)
    }

    @Test
    fun determineNextPhase_longBreakReturnsFocus() {
        val next = PomodoroTimerLogic.determineNextPhase(
            currentPhase = PomodoroPhase.LONG_BREAK,
            completedFocusCycles = 4,
            longBreakInterval = 4
        )
        assertEquals(PomodoroPhase.FOCUS, next)
    }

    @Test
    fun calculateActualFocusSec_doesNotDoubleSubtractPause() {
        // 开始 1000，结束 1100（墙钟 100s），其中暂停 20s → 实际 80s
        val actual = PomodoroTimerLogic.calculateActualFocusSec(
            startedAtSec = 1000,
            endedAtSec = 1100,
            pausedDurationSec = 20,
            plannedFocusSec = 1500
        )
        assertEquals(80L, actual)
    }

    @Test
    fun calculateActualFocusSec_neverNegative() {
        val actual = PomodoroTimerLogic.calculateActualFocusSec(
            startedAtSec = 1000,
            endedAtSec = 1010,
            pausedDurationSec = 50,
            plannedFocusSec = 1500
        )
        assertEquals(0L, actual)
    }

    @Test
    fun calculateActualFocusSec_clampedToPlanned() {
        val actual = PomodoroTimerLogic.calculateActualFocusSec(
            startedAtSec = 1000,
            endedAtSec = 5000,
            pausedDurationSec = 0,
            plannedFocusSec = 1500
        )
        assertEquals(1500L, actual)
    }

    @Test
    fun estimateElapsedFocusSec_fromPausedRemaining() {
        val session = PomodoroSession(
            id = 1,
            taskId = 10,
            stepId = null,
            startedAt = 1_000,
            endedAt = null,
            focusDurationSec = 1500,
            breakDurationSec = 0,
            pausedDurationSec = 0,
            cycles = 0,
            status = SessionStatus.PAUSED.value,
            phase = PomodoroPhase.FOCUS.value,
            plannedDurationMs = 1_500_000,
            targetEndEpochMs = null,
            remainingMsWhenPaused = 900_000,
            pauseStartedAtEpochMs = 2_000_000
        )
        // planned 1500s - remaining 900s = 600s elapsed
        assertEquals(600L, PomodoroTimerLogic.estimateElapsedFocusSec(session, nowEpochMs = 2_000_000))
    }

    @Test
    fun estimateElapsedFocusSec_breakPhaseIsZero() {
        val session = PomodoroSession(
            id = 1,
            taskId = 10,
            stepId = null,
            startedAt = 1_000,
            endedAt = null,
            focusDurationSec = 0,
            breakDurationSec = 300,
            pausedDurationSec = 0,
            cycles = 1,
            status = SessionStatus.IN_PROGRESS.value,
            phase = PomodoroPhase.SHORT_BREAK.value,
            plannedDurationMs = 300_000,
            targetEndEpochMs = 2_000_000,
            remainingMsWhenPaused = 0,
            pauseStartedAtEpochMs = 0
        )
        assertEquals(0L, PomodoroTimerLogic.estimateElapsedFocusSec(session))
    }

    @Test
    fun formatDurationSec() {
        assertEquals("5:05", PomodoroTimerLogic.formatDurationSec(305))
        assertEquals("0:00", PomodoroTimerLogic.formatDurationSec(0))
    }
}

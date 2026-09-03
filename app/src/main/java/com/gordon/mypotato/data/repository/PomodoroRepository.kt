package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.SessionStatus
import kotlinx.coroutines.flow.Flow

interface PomodoroRepository {

    fun getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>

    suspend fun getSessionById(id: Long): PomodoroSession?

    suspend fun getAllActiveSessions(): List<PomodoroSession>

    suspend fun addSession(session: PomodoroSession): Long

    suspend fun updateSession(session: PomodoroSession)

    suspend fun updateSessionStatus(id: Long, status: SessionStatus)

    suspend fun updateTimerState(
        id: Long,
        status: SessionStatus,
        pausedDurationSec: Long,
        targetEndEpochMs: Long?,
        remainingMsWhenPaused: Long,
        pauseStartedAtEpochMs: Long
    )

    suspend fun completeSession(
        id: Long,
        status: SessionStatus,
        endedAt: Long,
        focusDurationSec: Long,
        breakDurationSec: Long,
        pausedDurationSec: Long,
        cycles: Int
    )

    suspend fun interruptSession(id: Long, endedAt: Long)

    suspend fun deleteSession(id: Long)
}

package com.gordon.mypotato.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {

    @Query("SELECT * FROM pomodoro_session WHERE task_id = :taskId ORDER BY started_at DESC")
    fun getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_session WHERE id = :id")
    suspend fun getSessionById(id: Long): PomodoroSessionEntity?

    @Query(
        """
        SELECT * FROM pomodoro_session
        WHERE status IN (:inProgressStatus, :pausedStatus)
        ORDER BY started_at DESC, id DESC
        """
    )
    suspend fun getActiveSessions(inProgressStatus: Int, pausedStatus: Int): List<PomodoroSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PomodoroSessionEntity): Long

    @Update
    suspend fun update(session: PomodoroSessionEntity)

    @Query("UPDATE pomodoro_session SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query(
        """
        UPDATE pomodoro_session
        SET status = :status,
            paused_duration_sec = :pausedDurationSec,
            target_end_epoch_ms = :targetEndEpochMs,
            remaining_ms_when_paused = :remainingMsWhenPaused,
            pause_started_at_epoch_ms = :pauseStartedAtEpochMs
        WHERE id = :id
        """
    )
    suspend fun updateTimerState(
        id: Long,
        status: Int,
        pausedDurationSec: Long,
        targetEndEpochMs: Long?,
        remainingMsWhenPaused: Long,
        pauseStartedAtEpochMs: Long
    )

    @Query(
        """
        UPDATE pomodoro_session
        SET status = :status,
            ended_at = :endedAt,
            focus_duration_sec = :focusDurationSec,
            break_duration_sec = :breakDurationSec,
            paused_duration_sec = :pausedDurationSec,
            cycles = :cycles,
            target_end_epoch_ms = NULL,
            remaining_ms_when_paused = 0,
            pause_started_at_epoch_ms = 0
        WHERE id = :id
        """
    )
    suspend fun completeSession(
        id: Long,
        status: Int,
        endedAt: Long,
        focusDurationSec: Long,
        breakDurationSec: Long,
        pausedDurationSec: Long,
        cycles: Int
    )

    @Query(
        """
        UPDATE pomodoro_session
        SET status = :status,
            ended_at = :endedAt,
            focus_duration_sec = 0,
            target_end_epoch_ms = NULL,
            remaining_ms_when_paused = 0,
            pause_started_at_epoch_ms = 0
        WHERE id = :id
        """
    )
    suspend fun interruptSession(id: Long, status: Int, endedAt: Long)

    @Query("DELETE FROM pomodoro_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pomodoro_session WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}

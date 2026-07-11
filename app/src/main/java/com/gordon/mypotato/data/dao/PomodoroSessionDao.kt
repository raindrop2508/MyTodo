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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PomodoroSessionEntity): Long

    @Update
    suspend fun update(session: PomodoroSessionEntity)

    @Query("UPDATE pomodoro_session SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("DELETE FROM pomodoro_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pomodoro_session WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}
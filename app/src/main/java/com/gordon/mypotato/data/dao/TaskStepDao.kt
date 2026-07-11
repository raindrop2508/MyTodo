package com.gordon.mypotato.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gordon.mypotato.data.entity.TaskStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskStepDao {

    @Query("SELECT * FROM task_step WHERE task_id = :taskId ORDER BY sort_order ASC")
    fun getStepsByTaskId(taskId: Long): Flow<List<TaskStepEntity>>

    @Query("SELECT * FROM task_step WHERE id = :id")
    suspend fun getStepById(id: Long): TaskStepEntity?

    @Query("SELECT * FROM task_step WHERE task_id = :taskId AND status = :status ORDER BY sort_order ASC")
    fun getStepsByTaskIdAndStatus(taskId: Long, status: Int): Flow<List<TaskStepEntity>>

    @Query("SELECT SUM(spent_duration_sec) FROM task_step WHERE task_id = :taskId AND status = :status")
    suspend fun sumSpentDurationByTaskIdAndStatus(taskId: Long, status: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: TaskStepEntity): Long

    @Update
    suspend fun update(step: TaskStepEntity)

    @Query("UPDATE task_step SET status = :status, completed_at = :completedAt, spent_duration_sec = :spentDurationSec WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, completedAt: Long?, spentDurationSec: Long)

    @Query("DELETE FROM task_step WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task_step WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}
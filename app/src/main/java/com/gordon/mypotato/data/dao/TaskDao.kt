package com.gordon.mypotato.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gordon.mypotato.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task ORDER BY created_at DESC")
    fun getTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM task WHERE category_id = :categoryId ORDER BY created_at DESC")
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE task_type = :taskType ORDER BY created_at DESC")
    fun getTasksByType(taskType: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE status = :status ORDER BY created_at DESC")
    fun getTasksByStatus(status: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE is_urgent = :isUrgent AND is_important = :isImportant ORDER BY created_at DESC")
    fun getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    fun searchTasks(keyword: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM task 
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:taskType IS NULL OR task_type = :taskType)
          AND (:status IS NULL OR status = :status)
          AND (:isUrgent IS NULL OR is_urgent = :isUrgent)
          AND (:isImportant IS NULL OR is_important = :isImportant)
          AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%')
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getTasksByQuery(
        categoryId: Long?,
        taskType: Int?,
        status: Int?,
        isUrgent: Boolean?,
        isImportant: Boolean?,
        keyword: String?,
        offset: Int,
        limit: Int
    ): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE task SET status = :status, finished_at = :finishedAt, total_duration_sec = :totalDurationSec WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, finishedAt: Long?, totalDurationSec: Long)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE task SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun updateCategoryId(oldCategoryId: Long, newCategoryId: Long)
}
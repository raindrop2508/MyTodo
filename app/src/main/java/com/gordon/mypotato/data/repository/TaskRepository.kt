package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    fun getTasks(): Flow<List<Task>>

    suspend fun getTaskById(id: Long): Task?

    fun getTasksByQuery(query: TaskQuery): Flow<List<Task>>

    fun getTasksByCategory(categoryId: Long): Flow<List<Task>>

    fun getTasksByType(taskType: TaskType): Flow<List<Task>>

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    fun getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>>

    fun getStepsByTaskId(taskId: Long): Flow<List<TaskStep>>

    suspend fun addTask(task: Task): Long

    suspend fun updateTask(task: Task)

    suspend fun updateTaskStatus(id: Long, status: TaskStatus)

    suspend fun deleteTask(id: Long)

    suspend fun addStep(step: TaskStep): Long

    suspend fun updateStep(step: TaskStep)

    suspend fun updateStepStatus(id: Long, status: StepStatus)

    suspend fun deleteStep(id: Long)
}
package com.gordon.mypotato.data.repository

import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.dao.TaskStepDao
import com.gordon.mypotato.data.mapper.toDomain
import com.gordon.mypotato.data.mapper.toDomainList
import com.gordon.mypotato.data.mapper.toEntity
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val taskDao: TaskDao,
    private val taskStepDao: TaskStepDao
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getTasks().map { it.toDomainList() }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override fun getTasksByQuery(query: TaskQuery): Flow<List<Task>> {
        return taskDao.getTasksByQuery(
            categoryId = query.categoryId,
            taskType = query.taskType?.value,
            status = query.status?.value,
            isUrgent = query.isUrgent,
            isImportant = query.isImportant,
            keyword = query.keyword,
            offset = query.offset,
            limit = query.limit
        ).map { it.toDomainList() }
    }

    override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> {
        return taskDao.getTasksByCategory(categoryId).map { it.toDomainList() }
    }

    override fun getTasksByType(taskType: TaskType): Flow<List<Task>> {
        return taskDao.getTasksByType(taskType.value).map { it.toDomainList() }
    }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status.value).map { it.toDomainList() }
    }

    override fun getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>> {
        return taskDao.getTasksByQuadrant(isUrgent, isImportant).map { it.toDomainList() }
    }

    override fun getStepsByTaskId(taskId: Long): Flow<List<TaskStep>> {
        return taskStepDao.getStepsByTaskId(taskId).map { it.toDomainList() }
    }

    override suspend fun addTask(task: Task): Long {
        val newTask = task.copy(createdAt = System.currentTimeMillis() / 1000)
        return taskDao.insert(newTask.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
    }

    override suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
        val now = System.currentTimeMillis() / 1000
        val task = taskDao.getTaskById(id) ?: return

        val newFinishedAt = if (status == TaskStatus.COMPLETED) now else null
        val newTotalDuration = if (status == TaskStatus.COMPLETED) {
            calculateTotalDuration(id, now)
        } else {
            task.totalDurationSec
        }

        taskDao.updateStatus(id, status.value, newFinishedAt, newTotalDuration)
    }

    private suspend fun calculateTotalDuration(taskId: Long, finishedAt: Long): Long {
        val task = taskDao.getTaskById(taskId) ?: return 0
        val completedStepsDuration = taskStepDao.sumSpentDurationByTaskIdAndStatus(
            taskId, StepStatus.COMPLETED.value
        ) ?: 0
        val totalTimeFromCreation = finishedAt - task.createdAt
        return maxOf(completedStepsDuration, totalTimeFromCreation)
    }

    override suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
    }

    override suspend fun addStep(step: TaskStep): Long {
        val newStep = step.copy(createdAt = System.currentTimeMillis() / 1000)
        return taskStepDao.insert(newStep.toEntity())
    }

    override suspend fun updateStep(step: TaskStep) {
        taskStepDao.update(step.toEntity())
    }

    override suspend fun updateStepStatus(id: Long, status: StepStatus) {
        val now = System.currentTimeMillis() / 1000
        val step = taskStepDao.getStepById(id) ?: return

        val newCompletedAt = if (status == StepStatus.COMPLETED) now else null
        val newSpentDuration = if (status == StepStatus.COMPLETED) {
            now - step.createdAt
        } else {
            step.spentDurationSec
        }

        taskStepDao.updateStatus(id, status.value, newCompletedAt, newSpentDuration)
        updateTaskTotalDuration(step.taskId)
    }

    private suspend fun updateTaskTotalDuration(taskId: Long) {
        val totalDuration = taskStepDao.sumSpentDurationByTaskIdAndStatus(
            taskId, StepStatus.COMPLETED.value
        ) ?: 0
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.update(task.copy(totalDurationSec = totalDuration))
    }

    override suspend fun deleteStep(id: Long) {
        taskStepDao.deleteById(id)
    }
}
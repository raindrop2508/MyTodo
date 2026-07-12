package com.gordon.mypotato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import kotlinx.coroutines.launch

/**
* 基本的类处理的相关ViewModel，便于在不同页面中复用
* */
abstract class BaseTaskViewModel(
    protected val taskRepository: TaskRepository
) : ViewModel() {

    /**
     * 添加任务并包括添加长时任务的步骤
     */
    open fun addTask(task: Task, stepTitles: List<String> = emptyList()) {
        viewModelScope.launch {
            val taskId = taskRepository.addTask(task)
            if (task.isLongTask()) {
                stepTitles.forEachIndexed { index, title ->
                    val step = TaskStep(
                        id = 0,
                        taskId = taskId,
                        title = title,
                        sortOrder = index,
                        status = StepStatus.TODO.value,
                        completedAt = null,
                        spentDurationSec = 0,
                        createdAt = System.currentTimeMillis() / 1000
                    )
                    taskRepository.addStep(step)
                }
            }
        }
    }

    /**
     * 切换任务完成状态（按当前状态取反）
     */
    open fun toggleTaskStatus(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            task?.let {
                val newStatus = if (it.isCompleted()) {
                    TaskStatus.TODO
                } else {
                    TaskStatus.COMPLETED
                }
                taskRepository.updateTaskStatus(taskId, newStatus)
            }
        }
    }

    /**
     * 设置任务完成状态（直接使用目标值）
     */
    open fun setTaskStatus(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isCompleted) {
                TaskStatus.COMPLETED
            } else {
                TaskStatus.TODO
            }
            taskRepository.updateTaskStatus(taskId, newStatus)
        }
    }

    /**
     * 删除任务
     */
    open fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    /**
     * 根据分类 ID 获取分类名称
     */
    fun getCategoryName(categoryId: Long, categories: Map<Long, Category>): String {
        return categories[categoryId]?.name ?: "未分类"
    }
}

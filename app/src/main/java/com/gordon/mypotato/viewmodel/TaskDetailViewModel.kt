package com.gordon.mypotato.viewmodel

import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: Task? = null,
    val category: Category? = null,
    val steps: List<TaskStep> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val canStartPomodoro: Boolean = false
)

class TaskDetailViewModel(
    taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : BaseTaskViewModel(taskRepository) {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var currentTaskId: Long = -1

    fun loadTask(taskId: Long) {
        if (taskId == currentTaskId) return
        currentTaskId = taskId
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // 组合任务基本信息、分类和步骤流
            combine(
                taskRepository.getTasks(),
                categoryRepository.getCategories(),
                taskRepository.getStepsByTaskId(taskId)
            ) { tasks, categories, steps ->
                val task = tasks.find { it.id == taskId }
                val category = categories.find { it.id == task?.categoryId }
                
                _uiState.value.copy(
                    task = task,
                    category = category,
                    steps = steps,
                    isLoading = false,
                    canStartPomodoro = task?.isLongTask() == true
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    /**
     * 切换步骤完成状态
     */
    fun toggleStepStatus(stepId: Long) {
        viewModelScope.launch {
            val step = _uiState.value.steps.find { it.id == stepId }
            step?.let {
                val newStatus = if (it.status == StepStatus.COMPLETED.value) {
                    StepStatus.TODO
                } else {
                    StepStatus.COMPLETED
                }
                taskRepository.updateStepStatus(stepId, newStatus)
            }
        }
    }

    /**
     * 为当前任务添加新步骤
     */
    fun addStep(title: String) {
        if (title.isBlank() || currentTaskId == -1L) return
        viewModelScope.launch {
            val nextSortOrder = (_uiState.value.steps.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val newStep = TaskStep(
                id = 0,
                taskId = currentTaskId,
                title = title,
                sortOrder = nextSortOrder,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = System.currentTimeMillis() / 1000
            )
            taskRepository.addStep(newStep)
        }
    }

    /**
     * 删除当前任务
     */
    override fun deleteTask(taskId: Long) {
        super.deleteTask(taskId)
        _uiState.value = _uiState.value.copy(isDeleted = true)
    }
}

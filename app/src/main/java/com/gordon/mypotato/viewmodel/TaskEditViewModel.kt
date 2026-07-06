package com.gordon.mypotato.viewmodel

import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val task: Task? = null,
    val category: Category? = null,
    val categories: List<Category> = emptyList(),
    val steps: List<TaskStep> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

class TaskEditViewModel(
    taskRepository: TaskRepository = com.gordon.mypotato.data.repository.FakeTaskRepository.getInstance(),
    private val categoryRepository: CategoryRepository = com.gordon.mypotato.data.repository.FakeCategoryRepository.getInstance(taskRepository)
) : BaseTaskViewModel(taskRepository) {

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    private var currentTaskId: Long = -1

    /**
     * 功能：加载任务详情和步骤
     * 入参：taskId 任务 ID
     * 出参：无
     * 异常：无
     */
    fun loadTask(taskId: Long) {
        if (taskId == currentTaskId) return
        currentTaskId = taskId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

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
                    categories = categories,
                    steps = steps,
                    isLoading = false,
                    isSaved = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    /**
     * 功能：更新任务信息
     * 入参：updatedTask 更新后的任务对象
     * 出参：无
     * 异常：无
     */
    fun updateTask(updatedTask: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(updatedTask)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    /**
     * 功能：保存步骤变更（新增、修改、删除）
     * 入参：stepEdits 编辑后的步骤列表，deletedStepIds 已删除的步骤 ID 列表
     * 出参：无
     * 异常：无
     */
    fun saveSteps(stepEdits: List<EditableStepItem>, deletedStepIds: List<Long>) {
        viewModelScope.launch {
            deletedStepIds.forEach { stepId ->
                taskRepository.deleteStep(stepId)
            }

            stepEdits.forEachIndexed { index, item ->
                if (item.title.isNotBlank()) {
                    if (item.id == 0L) {
                        val newStep = TaskStep(
                            id = 0,
                            taskId = currentTaskId,
                            title = item.title,
                            sortOrder = index,
                            status = StepStatus.TODO.value,
                            completedAt = null,
                            spentDurationSec = 0,
                            createdAt = System.currentTimeMillis() / 1000
                        )
                        taskRepository.addStep(newStep)
                    } else {
                        val existingStep = _uiState.value.steps.find { it.id == item.id }
                        existingStep?.let {
                            val updatedStep = it.copy(
                                title = item.title,
                                sortOrder = index
                            )
                            taskRepository.updateStep(updatedStep)
                        }
                    }
                }
            }
        }
    }

    /**
     * 功能：根据分类名称获取分类 ID
     * 入参：categoryName 分类名称
     * 出参：返回分类 ID，未找到返回 0（未分类）
     * 异常：无
     */
    fun getCategoryIdByName(categoryName: String): Long {
        return _uiState.value.categories.find { it.name == categoryName }?.id ?: 0L
    }

    /**
     * 功能：根据分类 ID 获取分类名称
     * 入参：categoryId 分类 ID
     * 出参：返回分类名称，未找到返回"未分类"
     * 异常：无
     */
    fun getCategoryNameById(categoryId: Long): String {
        return _uiState.value.categories.find { it.id == categoryId }?.name ?: "未分类"
    }

    /**
     * 功能：构建更新后的任务对象
     * 入参：title 标题，content 内容，note 备注，taskType 任务类型，categoryId 分类 ID，isUrgent 是否紧急，isImportant 是否重要
     * 出参：返回更新后的 Task 对象
     * 异常：无
     */
    fun buildUpdatedTask(
        title: String,
        content: String?,
        note: String?,
        taskType: Int,
        categoryId: Long,
        isUrgent: Boolean,
        isImportant: Boolean
    ): Task? {
        val currentTask = _uiState.value.task ?: return null
        return currentTask.copy(
            title = title,
            content = content,
            note = note,
            taskType = taskType,
            categoryId = categoryId,
            isUrgent = isUrgent,
            isImportant = isImportant
        )
    }
}

/**
 * 功能：可编辑步骤项数据模型，用于编辑页步骤管理
 * 入参：无
 * 出参：无
 * 异常：无
 */
data class EditableStepItem(
    val id: Long,
    var title: String
)
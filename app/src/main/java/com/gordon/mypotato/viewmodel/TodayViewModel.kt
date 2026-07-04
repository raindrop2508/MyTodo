package com.gordon.mypotato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class PriorityFilter {
    ALL,
    UI,
    I,
    U,
    N,
}

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val filter: PriorityFilter = PriorityFilter.ALL,
    val isLoading: Boolean = false
)

class TodayViewModel(
    private val taskRepository: TaskRepository = com.gordon.mypotato.data.repository.FakeTaskRepository(),
    private val categoryRepository: CategoryRepository = com.gordon.mypotato.data.repository.FakeCategoryRepository(taskRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    private val _filter = MutableStateFlow(PriorityFilter.ALL)

    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()  // 只读状态流

    init {
        collectData()
    }

    private fun collectData() {
        viewModelScope.launch {   // 构建数据流，combine 将独立的数据源组合成一个流
            combine(
                taskRepository.getTasks(),
                categoryRepository.getCategories(),
                _filter
            ) { tasks, categories, filter ->
                val filteredTasks = filterTasks(tasks, filter)
                val categoryMap = categories.associateBy { it.id }   // 以每个类的 id 为key,转换为map
                TodayUiState(    // 返回值
                    tasks = filteredTasks,
                    categories = categoryMap,
                    filter = filter,
                    isLoading = false
                )
            }.collect {        // 触发开关
                _uiState.value = it     // 数据类返回的数据类赋值给uiState   每次调用 collect，都会更新 uiState
            }
        }
    }

    private fun filterTasks(tasks: List<Task>, filter: PriorityFilter): List<Task> {
        return when (filter) {
            PriorityFilter.ALL -> tasks
            PriorityFilter.UI -> tasks.filter { it.isUrgent && it.isImportant }
            PriorityFilter.I -> tasks.filter { !it.isUrgent && it.isImportant }
            PriorityFilter.U -> tasks.filter { it.isUrgent && !it.isImportant }
            PriorityFilter.N -> tasks.filter { !it.isUrgent && !it.isImportant }
        }.sortedBy { it.status }
    }

    fun setFilter(filter: PriorityFilter) {
        _filter.value = filter
    }

    suspend fun toggleTaskStatus(taskId: Long) {
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

    suspend fun addTask(task: Task): Long {
        return taskRepository.addTask(task)
    }

    suspend fun deleteTask(taskId: Long) {
        taskRepository.deleteTask(taskId)
    }

    fun getCategoryName(categoryId: Long): String {
        return _uiState.value.categories[categoryId]?.name ?: "未分类"
    }
}
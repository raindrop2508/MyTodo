package com.gordon.mypotato.viewmodel

import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TodayViewModel(
    taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : BaseTaskViewModel(taskRepository) {

    private val _uiState = MutableStateFlow(TodayUiState(isLoading = true))
    private val _filter = MutableStateFlow(PriorityFilter.ALL)

    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        collectData()
    }

    private fun collectData() {
        viewModelScope.launch {
            combine(
                taskRepository.getTasks(),
                categoryRepository.getCategories(),
                _filter
            ) { tasks, categories, filter ->
                val filteredTasks = filterTasks(tasks, filter)
                val categoryMap = categories.associateBy { it.id }
                TodayUiState(
                    tasks = filteredTasks,
                    categories = categoryMap,
                    filter = filter,
                    isLoading = false,
                    errorMessage = null
                )
            }.collect {
                _uiState.value = it
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

    fun getCategoryName(categoryId: Long): String {
        return getCategoryName(categoryId, _uiState.value.categories)
    }
}

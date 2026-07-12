package com.gordon.mypotato.viewmodel

import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.TaskQuery
import com.gordon.mypotato.data.repository.TaskRepository
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class FilterDimension {
    CATEGORY, QUADRANT, STATUS
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val dimension: FilterDimension = FilterDimension.CATEGORY,
    val selectedCategoryId: Long? = null,
    val selectedUrgent: Boolean? = null,
    val selectedImportant: Boolean? = null,
    val selectedStatus: TaskStatus? = null,
    val keyword: String = "",
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    taskRepository: TaskRepository,
    val categoryRepository: CategoryRepository
) : BaseTaskViewModel(taskRepository) {

    private val _dimension = MutableStateFlow(FilterDimension.CATEGORY)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedUrgent = MutableStateFlow<Boolean?>(null)
    private val _selectedImportant = MutableStateFlow<Boolean?>(null)
    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)
    private val _keyword = MutableStateFlow("")

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        collectData()
    }

    private fun collectData() {
        viewModelScope.launch {
            val filterFlow = combine(
                _selectedCategoryId,
                _selectedUrgent,
                _selectedImportant,
                _selectedStatus,
                _keyword
            ) { categoryId, urgent, important, status, keyword ->
                TaskQuery(
                    categoryId = categoryId,
                    isUrgent = urgent,
                    isImportant = important,
                    status = status,
                    keyword = keyword.ifBlank { null }
                )
            }

            combine(
                filterFlow.flatMapLatest { taskRepository.getTasksByQuery(it) },
                categoryRepository.getCategories(),
                _dimension,
                filterFlow
            ) { tasks, categories, dimension, query ->
                TasksUiState(
                    tasks = tasks.sortedBy { it.status },
                    categories = categories.associateBy { it.id },
                    dimension = dimension,
                    selectedCategoryId = query.categoryId,
                    selectedUrgent = query.isUrgent,
                    selectedImportant = query.isImportant,
                    selectedStatus = query.status,
                    keyword = query.keyword ?: ""
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun setDimension(dimension: FilterDimension) {
        _dimension.value = dimension
        // Reset filters when dimension changes
        _selectedCategoryId.value = null
        _selectedUrgent.value = null
        _selectedImportant.value = null
        _selectedStatus.value = null
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setQuadrantFilter(urgent: Boolean?, important: Boolean?) {
        _selectedUrgent.value = urgent
        _selectedImportant.value = important
    }

    fun setStatusFilter(status: TaskStatus?) {
        _selectedStatus.value = status
    }

    fun setKeyword(keyword: String) {
        _keyword.value = keyword
    }

    fun getCategoryName(categoryId: Long): String {
        return getCategoryName(categoryId, _uiState.value.categories)
    }
}

# TasksViewModel Integration and TasksFragment Refactoring

This plan outlines the integration of `TasksViewModel` into `TasksFragment`, replacing the local mock data and filtering logic with a centralized, repository-backed approach. This is part of Stage B: Core Flow Enablement.

## User Review Required

- **Filtering Logic**: The `TasksViewModel` will use `TaskRepository.getTasksByQuery` for filtering. This might behave slightly differently from the current local filtering (e.g., keyword search now includes content, not just title).
- **Code Reuse**: I propose creating a common trait/delegate or base class for `addTask` and `toggleTaskStatus` to avoid duplication between `TodayViewModel` and `TasksViewModel`.

## Proposed Changes

### ViewModel Layer

#### [NEW] [TasksViewModel.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/viewmodel/TasksViewModel.kt)

- Define `TasksUiState` to hold tasks, categories, and current filter states (dimension, categoryId, quadrant, status, keyword).
- Implement `TasksViewModel` using `TaskRepository` and `CategoryRepository`.
- Use `getTasksByQuery` for reactive filtering.
- Implement `addTask` and `toggleTaskStatus` (or reuse via base class/delegate).

#### [TasksViewModel.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/viewmodel/TasksViewModel.kt)
```kotlin
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
```

---

### UI Layer

#### [TasksFragment.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt)

- Replace `MutableList<TaskUiModel>` and `buildMockTasks()` with `TasksViewModel`.
- Update `setupChips`, `setupDimensionTabs`, and `setupSearch` to call ViewModel methods.
- Update `renderTasks` (or rename to `collectTasks`) to observe `uiState`.
- Update `onTaskCheckChanged` to call `viewModel.toggleTaskStatus`.
- Update `setupFab` to call `viewModel.addTask` (consistent with `TodayFragment`).
- Update `TasksAdapter` and `TasksViewHolder` to use `com.gordon.mypotato.domain.Task` and `Category`.

---

## Verification Plan

### Automated Tests
- I will verify if there are existing tests for `TasksFragment`. (None expected in Stage B).
- I will manually verify the flows.

### Manual Verification
1. **Filtering**:
    - Switch between "Category", "Quadrant", and "Status" dimensions.
    - Click different chips and verify the list updates correctly.
    - Input text in the search bar and verify the list filters by title/content.
2. **Task Actions**:
    - Click FAB, fill form, and save. Verify the new task appears in the list.
    - Toggle a task's checkbox and verify it moves to the bottom (sorted by status).
3. **Navigation**:
    - Click a task and verify it navigates to `TaskDetailActivity` with correct extras.

# TodayViewModel 实现计划

## 1. 需求分析

根据 Stage B4：补齐各页面 ViewModel 的要求，为 TodayFragment 创建 ViewModel，实现以下职责：
- 加载今日任务列表
- 四象限筛选（UI/I/U/N）
- 切换任务完成状态
- 添加新任务
- 删除任务

## 2. 架构设计

### 2.1 ViewModel 设计

**类名：** `TodayViewModel`

**构造参数：**
- `taskRepository: TaskRepository` - 任务数据访问接口
- `categoryRepository: CategoryRepository` - 分类数据访问接口（用于获取分类名称映射）

**UI 状态：**
```kotlin
data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val filter: PriorityFilter = PriorityFilter.ALL,
    val isLoading: Boolean = false
)
```

**核心方法：**
| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getTasks()` | `StateFlow<TodayUiState>` | 获取任务列表状态流 |
| `setFilter(filter: PriorityFilter)` | `Unit` | 设置筛选条件 |
| `toggleTaskStatus(taskId: Long)` | `suspend Unit` | 切换任务完成状态 |
| `addTask(task: Task)` | `suspend Long` | 添加新任务 |
| `deleteTask(taskId: Long)` | `suspend Unit` | 删除任务 |

### 2.2 数据流

```
Repository (Flow) → ViewModel (StateFlow) → Fragment (collect)
```

## 3. 文件修改清单

### 3.1 新建文件

| 文件路径 | 说明 |
|---------|------|
| `app/src/main/java/com/gordon/mypotato/viewmodel/TodayViewModel.kt` | TodayFragment 对应的 ViewModel |

### 3.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt` | 移除本地 Mock 数据，接入 ViewModel |

## 4. 实施步骤

### 步骤 1：创建 TodayViewModel

**文件：** `viewmodel/TodayViewModel.kt`

**内容要点：**
- 使用 `MutableStateFlow<TodayUiState>` 持有 UI 状态
- 在 `init` 块中收集 `taskRepository.getTasks()` 和 `categoryRepository.getCategories()`
- 实现四象限筛选逻辑（基于 `isUrgent` 和 `isImportant`）
- 实现 `toggleTaskStatus` 在 `viewModelScope` 中调用 `updateTaskStatus`
- 实现 `addTask` 在 `viewModelScope` 中调用 `addTask`

### 步骤 2：修改 TodayFragment

**修改要点：**
- 移除 `buildMockTasks()` 和 `TodayTask` 内部类
- 使用 `by viewModels` 获取 `TodayViewModel` 实例
- 在 `onViewCreated` 中收集 ViewModel 的 `StateFlow`
- 修改 `onTaskCheckChanged` 调用 `viewModel.toggleTaskStatus`
- 修改 `onTaskClicked` 使用 `Task` 实体的 `id` 和 `title`
- 修改 `AddTaskBottomSheetHelper.Callback.onTaskCreate` 调用 `viewModel.addTask`
- 修改 `renderTasks` 使用 ViewModel 的数据

### 步骤 3：分类名称映射

ViewModel 需要维护分类 ID 到分类名称的映射，以便在 UI 中显示分类标签。

## 5. 关键技术点

1. **StateFlow 响应式状态管理**
   - `MutableStateFlow<TodayUiState>` 作为单一状态源
   - `asStateFlow()` 暴露不可变的 StateFlow 给 UI

2. **ViewModelScope 协程管理**
   - 使用 `viewModelScope.launch` 收集 Flow
   - 使用 `viewModelScope.launch` 执行 suspend 写操作

3. **四象限筛选逻辑**
   - ALL: 显示所有任务
   - UI: `isUrgent=true` && `isImportant=true`
   - I: `isUrgent=false` && `isImportant=true`
   - U: `isUrgent=true` && `isImportant=false`
   - N: `isUrgent=false` && `isImportant=false`

4. **任务状态切换**
   - 已完成 → 待办
   - 待办/进行中 → 已完成

## 6. 风险与注意事项

1. **Repository 依赖注入**
   - 当前无 DI 框架，ViewModel 使用默认构造函数创建 Repository 实例
   - 后续可引入 Hilt 进行真正的依赖注入

2. **分类数据同步**
   - 需要同时收集任务和分类数据
   - 使用 `combine` 操作符合并两个 Flow

3. **任务状态转换逻辑**
   - 需要处理 `TaskStatus` 枚举的转换
   - 确保状态流转符合业务规则

## 7. 验证标准

- TodayFragment 能正确显示从 FakeRepository 加载的任务数据
- 四象限筛选功能正常工作
- 勾选完成后状态即时更新
- 添加新任务后列表自动刷新
- 删除任务后列表自动刷新
- 任务卡片显示正确的分类名称
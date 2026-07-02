# Stage B：核心流程跑通计划

> 文档版本：v1.1  
> 更新日期：2026-07-02  
> 适用范围：MyPotato Stage B 阶段执行  
> 前置条件：Stage A（UI 骨架）已完成

---

## 一、阶段目标

Stage B 的核心目标是 **引入 Repository 模式，通过 FakeRepository（内存实现）打通三条主流程**，实现页面间数据互通，为后续 Room 落地（Stage C）奠定接口契约基础。

**三条主流程：**
1. 新建任务 → 保存 → 列表刷新
2. 编辑任务 → 保存 → 详情刷新
3. 标记完成 → 状态即时可见

**关键约束：**
- 不引入 Room Entity/DAO/Database（属 Stage C）
- 不涉及深色模式适配（属 Stage E）
- 统计页保持延后状态

---

## 二、工作清单（按依赖顺序）

### B1：定义领域模型（Domain Models） 已完成 26-06-29

**描述：**
定义统一的业务实体数据类，作为后续 Repository 与页面迁移的契约基础。页面中分散的 `TodayTask`、`TaskUiModel`、`StepMock` 等重复定义，统一在 `B5` 阶段完成迁移与删除。

**核心实体：**

| 实体 | 核心字段 | 类型约束 |
|------|---------|---------|
| `Task` | id(Long), title, content(String?), note(String?), taskType(Int), status(Int), isUrgent(Boolean), isImportant(Boolean), categoryId(Long), createdAt(Long), plannedStartAt(Long?), finishedAt(Long?), totalDurationSec(Long) | ID 用 Long；时间戳用 Long(秒)；`plannedStartAt` 为可选开始时间 |
| `TaskStep` | id(Long), taskId(Long), title, sortOrder(Int), status(Int), completedAt(Long?), spentDurationSec(Long), createdAt(Long) | N:1 Task |
| `Category` | id(Long), name, colorHex, iconName | 1:N Task |

**任务类型枚举（Int）：**
- 0：单次任务（ONCE）
- 1：长时任务（LONG）

**状态枚举（Int）：**
- 0：待办（TODO）
- 1：进行中（IN_PROGRESS）
- 2：已完成（COMPLETED）
- 3：已归档（ARCHIVED，可选预留）

**影响文件：**
- 新建 `domain/Task.kt`
- 新建 `domain/TaskStep.kt`
- 新建 `domain/Category.kt`
- 新建 `domain/TaskType.kt`
- 新建 `domain/TaskStatus.kt`
- 新建 `domain/StepStatus.kt`
- 页面内分散模型定义保留至 `B5` 统一迁移

**验证标准：**
- 所有领域模型文件创建完成
- 字段类型符合 Long ID、Int 枚举、Long 时间戳约束
- `plannedStartAt` 为 `Long?`，用于表示可选的计划开始时间
- 无 Room 注解（属 Stage C）

---

### B2：定义 Repository 接口契约

**描述：**
按聚合根拆分定义多个 Repository 接口，方法覆盖当前 UI 需求，作为数据访问的统一入口。接口层使用 Flow 返回、suspend 异步方法、枚举类型保证类型安全。

**接口拆分方案：**

| Repository 接口 | 职责 | 说明 |
|----------------|------|------|
| `TaskRepository` | 任务 CRUD、筛选、状态流转 | 包含 TaskStep 操作（步骤无独立生命周期） |
| `CategoryRepository` | 分类 CRUD | 独立聚合根 |
| `PomodoroRepository` | 番茄钟会话 CRUD | Stage B 仅定义接口，Stage D 实现 |

**TaskRepository 接口方法：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getTasks(): Flow<List<Task>>` | Flow | 获取所有任务，支持响应式刷新 |
| `getTaskById(id: Long): suspend Task?` | suspend Task? | 根据 ID 获取单个任务，异步返回 |
| `getTasksByQuery(query: TaskQuery): Flow<List<Task>>` | Flow | 多条件组合查询 |
| `getTasksByCategory(categoryId: Long): Flow<List<Task>>` | Flow | 按分类筛选任务（便捷方法） |
| `getTasksByType(taskType: TaskType): Flow<List<Task>>` | Flow | 按任务类型筛选（ONCE/LONG） |
| `getTasksByStatus(status: TaskStatus): Flow<List<Task>>` | Flow | 按状态筛选 |
| `getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>>` | Flow | 按四象限筛选 |
| `addTask(task: Task): suspend Long` | suspend Long | 添加任务，返回新 ID |
| `updateTask(task: Task): suspend Unit` | suspend Unit | 更新任务 |
| `updateTaskStatus(id: Long, status: TaskStatus): suspend Unit` | suspend Unit | 更新任务状态 |
| `deleteTask(id: Long): suspend Unit` | suspend Unit | 删除任务及关联步骤 |
| `getStepsByTaskId(taskId: Long): Flow<List<TaskStep>>` | Flow | 获取任务步骤 |
| `addStep(step: TaskStep): suspend Long` | suspend Long | 添加步骤 |
| `updateStep(step: TaskStep): suspend Unit` | suspend Unit | 更新步骤 |
| `updateStepStatus(id: Long, status: StepStatus): suspend Unit` | suspend Unit | 更新步骤状态 |
| `deleteStep(id: Long): suspend Unit` | suspend Unit | 删除步骤 |

**CategoryRepository 接口方法：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getCategories(): Flow<List<Category>>` | Flow | 获取所有分类 |
| `getCategoryById(id: Long): suspend Category?` | suspend Category? | 根据 ID 获取单个分类 |
| `addCategory(category: Category): suspend Long` | suspend Long | 添加分类，返回新 ID |
| `updateCategory(category: Category): suspend Unit` | suspend Unit | 更新分类信息 |
| `deleteCategory(id: Long): suspend Unit` | suspend Unit | 删除分类，关联任务的 categoryId 置为 0 |

**PomodoroRepository 接口方法（Stage B 仅定义）：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `addSession(session: PomodoroSession): suspend Long` | suspend Long | 添加番茄钟会话 |
| `updateSession(session: PomodoroSession): suspend Unit` | suspend Unit | 更新番茄钟会话 |
| `updateSessionStatus(id: Long, status: SessionStatus): suspend Unit` | suspend Unit | 更新会话状态 |
| `deleteSession(id: Long): suspend Unit` | suspend Unit | 删除番茄钟会话 |
| `getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>` | Flow | 获取指定任务的番茄钟会话列表 |
| `getSessionById(id: Long): suspend PomodoroSession?` | suspend PomodoroSession? | 根据 ID 获取单个番茄钟会话 |

**组合查询数据类：**

```kotlin
data class TaskQuery(
    val categoryId: Long? = null,
    val taskType: TaskType? = null,
    val status: TaskStatus? = null,
    val isUrgent: Boolean? = null,
    val isImportant: Boolean? = null,
    val keyword: String? = null,
    val offset: Int = 0,
    val limit: Int = 50
)
```

**影响文件：**
- 新建 `data/repository/TaskQuery.kt`（组合查询数据类）
- 新建 `data/repository/TaskRepository.kt`（接口）
- 新建 `data/repository/CategoryRepository.kt`（接口）
- 新建 `data/repository/PomodoroRepository.kt`（接口，Stage B 仅定义）

**验证标准：**
- 所有 Repository 接口文件创建完成
- 查询方法返回类型统一使用 `Flow`
- 写操作和单次查询使用 `suspend` 函数
- 接口层使用枚举类型（`TaskStatus`, `TaskType`, `StepStatus`, `SessionStatus`）
- 包含 `TaskQuery` 组合查询方法
- 方法覆盖当前 UI 所有数据操作需求
- 接口不依赖 Room 相关类

---

### B3：实现 FakeRepository（内存实现）

**描述：**
基于拆分后的 Repository 接口实现内存版 `FakeRepository`，集中管理所有 Mock 数据。实现层负责枚举↔Int 的映射，与领域模型中的 Int 字段保持一致。

**实现要点：**
- 使用 `MutableList` 内存持有数据
- 实现 ID 自增生成器
- 使用 `MutableStateFlow` 持有数据，数据变更后通过 `stateFlow.value = newValue` 通知观察者
- 实现枚举↔Int 的双向映射（`TaskStatus`, `TaskType`, `StepStatus`, `SessionStatus`）
- 预置默认分类（学习/工作/生活/健康/购物）
- 预置示例任务数据用于演示
- 删除任务时级联删除关联步骤和番茄钟会话
- 删除分类时将关联任务的 `categoryId` 置为 0（未分类）

**影响文件：**
- 新建 `data/repository/FakeTaskRepository.kt`（实现 `TaskRepository`）
- 新建 `data/repository/FakeCategoryRepository.kt`（实现 `CategoryRepository`）

**验证标准：**
- `FakeTaskRepository` 实现 `TaskRepository` 接口所有方法
- `FakeCategoryRepository` 实现 `CategoryRepository` 接口所有方法
- 数据变更后 Flow 能正确通知所有订阅者
- 枚举↔Int 映射正确，与领域模型保持一致
- 默认分类与示例任务数据加载完成
- 删除任务时级联删除关联步骤
- 删除分类时关联任务的 `categoryId` 正确置为 0

---

### B4：补齐各页面 ViewModel

**描述：**
为所有页面创建 ViewModel，持有 UI 状态并通过 `StateFlow` 暴露数据。ViewModel 通过 `viewModelScope` 收集 Repository 返回的 `Flow` 数据。

**需创建的 ViewModel：**

| ViewModel | 页面 | 职责 |
|-----------|------|------|
| `TodayViewModel` | TodayFragment | 加载今日任务、四象限筛选、切换完成状态 |
| `TasksViewModel` | TasksFragment | 加载任务列表、多维筛选（类型/日期/分类）、搜索 |
| `TaskDetailViewModel` | TaskDetailActivity | 加载任务详情、步骤列表、标记完成、启动番茄钟 |
| `TaskEditViewModel` | TaskEditActivity | 任务/步骤的增删改、表单验证 |
| `PomodoroViewModel` | PomodoroActivity | 计时状态机、会话记录（内存）、任务上下文 |
| `SettingsViewModel` | SettingsFragment | 读取/保存设置项（主题、语言、番茄钟时长） |

**实现要点：**
- ViewModel 通过构造注入获取 Repository 实例（`TaskRepository`, `CategoryRepository`）
- 所有数据请求通过 Repository，禁止直接访问数据源
- 使用 `MutableStateFlow` 持有 UI 状态，通过 `StateFlow` 暴露给 UI
- 在 `viewModelScope.launch` 中收集 Repository 返回的 `Flow` 数据
- 在 `viewModelScope.launch` 中调用 Repository 的 `suspend` 方法执行写操作
- 使用 `viewModelScope` 管理协程生命周期，避免内存泄漏

**影响文件：**
- 新建 `viewmodel/TodayViewModel.kt`
- 新建 `viewmodel/TasksViewModel.kt`
- 新建 `viewmodel/TaskDetailViewModel.kt`
- 新建 `viewmodel/TaskEditViewModel.kt`
- 新建 `viewmodel/PomodoroViewModel.kt`
- 新建 `viewmodel/SettingsViewModel.kt`

**验证标准：**
- 所有页面 ViewModel 创建完成
- ViewModel 通过 Repository 获取数据
- UI 能正确收集（`collect`）ViewModel 的 `StateFlow`
- 写操作在 `viewModelScope` 中正确执行
- 协程生命周期管理正确，无内存泄漏

---

### B5：迁移 Mock 数据至统一数据源

**描述：**
删除各页面分散的 Mock 数据生成方法，统一从 `FakeRepository` 获取数据。

**需迁移的页面：**

| 页面 | 当前 Mock 位置 | 迁移目标 |
|------|---------------|---------|
| TodayFragment | `buildMockTasks()` | `TodayViewModel` → `FakeRepository.getTasks()` |
| TasksFragment | `buildMockTasks()` | `TasksViewModel` → `FakeRepository.getTasks()` |
| TaskDetailActivity | `buildMockSteps()` | `TaskDetailViewModel` → `FakeRepository.getStepsByTaskId()` |
| TaskEditActivity | 占位步骤 | `TaskEditViewModel` → `FakeRepository` |

**影响文件：**
- 修改 `TodayFragment.kt`
- 修改 `TasksFragment.kt`
- 修改 `TaskDetailActivity.kt`
- 修改 `TaskEditActivity.kt`

**验证标准：**
- 所有页面不再包含独立的 Mock 数据生成方法
- 页面数据统一来自 Repository
- 删除 `TodayTask`、`TaskUiModel`、`StepMock` 等重复模型

---

### B6：打通三条主流程闭环

**描述：**
确保数据变更后相关页面能即时刷新。

**流程 1：新建任务 → 保存 → 列表刷新**
- Today/Tasks 页点击 FAB 打开 BottomSheet
- 填写表单后调用 `TaskRepository.addTask()`（在 `viewModelScope` 中执行）
- 列表页自动收到 Flow 更新通知并刷新

**流程 2：编辑任务 → 保存 → 详情刷新**
- TaskDetail 点击编辑跳转到 TaskEdit
- 保存后调用 `TaskRepository.updateTask()`（在 `viewModelScope` 中执行）
- 返回详情页时数据已更新

**流程 3：标记完成 → 状态即时可见**
- Today/Tasks/Detail 页勾选完成
- 调用 `TaskRepository.updateTaskStatus()`（在 `viewModelScope` 中执行）
- 所有相关页面同步刷新状态

**影响文件：**
- 修改 `AddTaskBottomSheetHelper.kt`
- 修改 `TaskEditActivity.kt`
- 修改 `TaskDetailActivity.kt`
- 修改 `TodayFragment.kt`
- 修改 `TasksFragment.kt`

**验证标准：**
- 新建任务后 Today/Tasks 列表即时刷新
- 编辑任务后返回详情页数据已更新
- 勾选完成后所有页面状态同步

---

### B7：导航参数规范化

**描述：**
引入 Safe Args 或统一参数协议，确保 Intent 传参类型安全。

**需规范的导航：**

| 导航路径 | 当前问题 | 改进方案 |
|---------|---------|---------|
| Today → TaskDetail | 手动传 taskId | Safe Args |
| Tasks → TaskDetail | 手动传 taskId | Safe Args |
| TaskDetail → TaskEdit | taskId 可空 | Safe Args |
| TaskDetail → Pomodoro | 未传任务参数 | 传递 taskId + taskTitle |

**影响文件：**
- 修改 `navigation/nav_graph.xml`（添加 Safe Args）
- 修改 `TaskDetailActivity.kt`（传递参数给 Pomodoro）
- 修改 `PomodoroActivity.kt`（接收任务参数）

**验证标准：**
- 所有 Intent 传参使用 Safe Args 或统一协议
- TaskDetail → Pomodoro 能正确接收任务上下文

---

### B8：规则一致性校验

**描述：**
确保单次任务在所有入口无法进入番茄钟。

**校验点：**

| 入口 | 校验位置 | 校验逻辑 |
|------|---------|---------|
| TaskDetail 计时按钮 | `TaskDetailViewModel` | 检查 `taskType == LONG`，否则禁用按钮或 Toast 提示 |
| PomodoroActivity 启动 | `PomodoroViewModel` | 兜底校验，无有效长时任务时返回或显示错误 |

**影响文件：**
- 修改 `TaskDetailViewModel.kt`
- 修改 `TaskDetailActivity.kt`（UI 层禁用/提示）
- 修改 `PomodoroViewModel.kt`
- 修改 `PomodoroActivity.kt`

**验证标准：**
- 单次任务的计时按钮禁用或有明确提示
- 强制传入单次任务 ID 时 Pomodoro 能正确处理

---

### B9：设置项接入（番茄钟时长）

**描述：**
Pomodoro 页读取设置页的 `focusMinutes`，替代硬编码的 25 分钟。

**实现要点：**
- `SettingsViewModel` 保存设置到 SharedPreferences
- `PomodoroViewModel` 读取 `focusMinutes` 作为默认时长
- 支持工作时长/休息时长/长休息时长/长休息间隔

**影响文件：**
- 修改 `SettingsViewModel.kt`
- 修改 `PomodoroViewModel.kt`
- 修改 `PomodoroActivity.kt`

**验证标准：**
- 设置页修改番茄钟时长后，下次启动生效
- 默认值为 25 分钟（保持原有行为）

---

## 三、阶段产出与验收标准

### 产出物

| 产出 | 说明 |
|------|------|
| Domain Models | Task/TaskStep/Category 数据类 |
| Repository 接口 | `TaskRepository.kt` |
| FakeRepository | `FakeRepository.kt`（内存实现） |
| ViewModel 层 | 6 个 ViewModel 文件 |
| 数据迁移完成 | 所有页面统一从 Repository 获取数据 |
| 流程闭环 | 新建/编辑/完成三条主流程打通 |

### 验收标准

| 验收项 | 通过条件 |
|--------|---------|
| 数据互通 | Today 页勾选完成，Tasks 页即时刷新 |
| 新建任务 | 填写表单保存后，Today/Tasks 列表出现新任务 |
| 编辑任务 | 修改后返回详情页，数据已更新 |
| 规则校验 | 单次任务无法进入番茄钟，有明确提示 |
| 参数传递 | TaskDetail → Pomodoro 任务上下文正确 |
| 时长设置 | 修改设置后，番茄钟使用新时长 |
| 单一数据源 | 无页面内独立 Mock 数据生成方法 |

---

## 四、不包含在 Stage B 的工作

以下内容明确属于后续阶段，**不在 Stage B 范围内**：

| 内容 | 所属阶段 | 说明 |
|------|---------|------|
| Room Entity/DAO/Database | Stage C | 数据持久化 |
| RoomRepository | Stage C | Room 实现的 Repository |
| Flow 响应式刷新（Room 版） | Stage C | 当前用 FakeRepository + StateFlow |
| 番茄钟会话落库 | Stage D | PomodoroSession 实体，`PomodoroRepository` 接口实现 |
| 统计页开发 | Stage D | 需数据库落地后确定 |
| 深色模式适配 | Stage E | 移除强制浅色、填充 values-night |
| i18n 多语言 | Stage E | values-zh-rCN / values-en |
| 单元测试/UI 测试 | Stage E | 关键流程测试 |

---

## 五、实施建议

1. **分批迁移，每页验证**：每完成一个页面的 Repository 接入即验证，避免大爆炸式改动。
2. **接口契约稳定**：Repository 接口一旦定义，Fake 实现必须遵守，避免实现侧偷偷加方法。
3. **AI 辅助生成样板代码**：ViewModel 和 FakeRepository 的基础结构可由 AI 生成，人工审查接口设计与数据流。
4. **保持可运行状态**：每个工作项完成后确保应用仍能正常启动和运行。

---

> 本计划作为 Stage B 的执行指南，严格按顺序推进。完成后进入 Stage C（Room 落地）。

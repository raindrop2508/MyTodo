# FakeRepository（内存实现）完整实现方案

## 一、需求分析

### 1.1 目标概述

根据 Stage B 计划，需要实现基于内存的 FakeRepository，作为数据访问层的统一入口，支撑三条核心流程的闭环验证：
- 新建任务 → 保存 → 列表刷新
- 编辑任务 → 保存 → 详情刷新
- 标记完成 → 状态即时可见

### 1.2 核心约束

| 约束项 | 要求 |
|--------|------|
| 数据存储 | 纯内存（MutableList + MutableStateFlow） |
| ID 生成 | 自增 Long 类型 |
| 时间戳 | Long 类型（秒） |
| 枚举映射 | 接口层使用枚举类型，实现层需处理枚举↔Int 映射 |
| 默认数据 | 预置 5 个分类 + 5-6 个示例任务 |
| 级联删除 | 删除任务时级联删除关联步骤和会话 |
| 分类删除 | 删除分类时关联任务的 categoryId 置为 0 |

### 1.3 已有接口契约

已定义的 Repository 接口（[TaskRepository.kt](file:///c:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/repository/TaskRepository.kt)、[CategoryRepository.kt](file:///c:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/repository/CategoryRepository.kt)、[PomodoroRepository.kt](file:///c:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/repository/PomodoroRepository.kt)）。

---

## 二、技术方案

### 2.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│   TodayFragment / TasksFragment / TaskDetailActivity        │
└──────────────────────┬──────────────────────────────────────┘
                       │ ViewModel (StateFlow)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                     Repository Layer                        │
│   FakeTaskRepository / FakeCategoryRepository               │
│   (接口实现 + MutableStateFlow + MutableList)               │
└──────────────────────┬──────────────────────────────────────┘
                       │ 内存数据持有
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Models                          │
│   Task / TaskStep / Category / PomodoroSession              │
│   TaskType / TaskStatus / StepStatus / SessionStatus        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据流向设计

```
数据写入流程：
UI → ViewModel (viewModelScope.launch) → Repository.suspend() → 更新 MutableList → StateFlow.value = newValue → 通知所有订阅者

数据读取流程：
ViewModel → Repository.flow() → UI.collect(StateFlow) → 更新 UI
```

### 2.3 关键实现要点

#### 2.3.1 StateFlow 响应式机制

使用 `MutableStateFlow` 持有数据集合，每次数据变更时通过 `stateFlow.value = ArrayList(currentList)` 创建新列表触发更新通知。

#### 2.3.2 ID 自增生成器

使用 `AtomicLong` 保证线程安全的 ID 生成：
```kotlin
private val taskIdGenerator = AtomicLong(1)
private val stepIdGenerator = AtomicLong(1)
private val categoryIdGenerator = AtomicLong(1)
private val sessionIdGenerator = AtomicLong(1)
```

#### 2.3.3 枚举↔Int 映射

接口层接收枚举类型参数，实现层内部转换为 Int 存储：
```kotlin
// 接收枚举，转换为 Int 进行查询/更新
override suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
    val index = tasks.indexOfFirst { it.id == id }
    if (index != -1) {
        tasks[index] = tasks[index].copy(status = status.value)
        emitTasks()
    }
}
```

---

## 三、实现步骤

### 3.1 步骤一：创建 FakeTaskRepository

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/FakeTaskRepository.kt`

**核心结构**：
```kotlin
class FakeTaskRepository : TaskRepository {
    // 内存数据
    private val tasks = mutableListOf<Task>()
    private val taskSteps = mutableListOf<TaskStep>()
    private val pomodoroSessions = mutableListOf<PomodoroSession>()
    
    // StateFlow
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val stepsFlowMap = mutableMapOf<Long, MutableStateFlow<List<TaskStep>>>()
    
    // ID 生成器
    private val taskIdGenerator = AtomicLong(1)
    private val stepIdGenerator = AtomicLong(1)
    
    init {
        initDefaultTasks()
    }
    
    // ... 接口方法实现
}
```

**需实现的方法**（共 14 个）：

| 方法 | 实现策略 |
|------|---------|
| `getTasks(): Flow<List<Task>>` | 返回 `tasksFlow.asStateFlow()` |
| `getTaskById(id: Long): suspend Task?` | 直接从 `tasks` 查找 |
| `getTasksByQuery(query: TaskQuery): Flow<List<Task>>` | 组合条件过滤 |
| `getTasksByCategory(categoryId: Long): Flow<List<Task>>` | 按 categoryId 过滤 |
| `getTasksByType(taskType: TaskType): Flow<List<Task>>` | 按 taskType.value 过滤 |
| `getTasksByStatus(status: TaskStatus): Flow<List<Task>>` | 按 status.value 过滤 |
| `getTasksByQuadrant(isUrgent, isImportant): Flow<List<Task>>` | 按四象限过滤 |
| `getStepsByTaskId(taskId: Long): Flow<List<TaskStep>>` | 返回对应 taskId 的步骤 |
| `addTask(task: Task): suspend Long` | 生成新 ID，添加到 tasks，emit |
| `updateTask(task: Task): suspend Unit` | 替换原有任务，emit |
| `updateTaskStatus(id, status): suspend Unit` | 更新状态字段，emit |
| `deleteTask(id: Long): suspend Unit` | 删除任务及关联步骤/会话，emit |
| `addStep(step: TaskStep): suspend Long` | 生成新 ID，添加到 taskSteps，emit |
| `updateStep(step: TaskStep): suspend Unit` | 替换原有步骤，emit |
| `updateStepStatus(id, status): suspend Unit` | 更新步骤状态，emit |
| `deleteStep(id: Long): suspend Unit` | 删除步骤，emit |

### 3.2 步骤二：创建 FakeCategoryRepository

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/FakeCategoryRepository.kt`

**核心结构**：
```kotlin
class FakeCategoryRepository(
    private val taskRepository: TaskRepository
) : CategoryRepository {
    private val categories = mutableListOf<Category>()
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val categoryIdGenerator = AtomicLong(1)
    
    init {
        initDefaultCategories()
    }
    
    // ... 接口方法实现
}
```

**预置默认分类**：

| ID | 名称 | 颜色 | 图标 |
|----|------|------|------|
| 1 | 学习 | #FF6B6B | ic_category_study |
| 2 | 工作 | #4ECDC4 | ic_category_work |
| 3 | 生活 | #FFE66D | ic_category_life |
| 4 | 健康 | #95E1D3 | ic_category_health |
| 5 | 购物 | #F38181 | ic_category_shopping |

**需实现的方法**（共 5 个）：

| 方法 | 实现策略 |
|------|---------|
| `getCategories(): Flow<List<Category>>` | 返回 `categoriesFlow.asStateFlow()` |
| `getCategoryById(id: Long): suspend Category?` | 直接查找 |
| `addCategory(category: Category): suspend Long` | 生成新 ID，添加，emit |
| `updateCategory(category: Category): suspend Unit` | 替换，emit |
| `deleteCategory(id: Long): suspend Unit` | 删除分类，调用 `taskRepository` 将关联任务 categoryId 置为 0 |

### 3.3 步骤三：预置示例任务数据

在 `FakeTaskRepository.initDefaultTasks()` 中预置示例数据，模拟实际业务场景：

**示例任务数据结构**：

| ID | 标题 | 类型 | 状态 | 紧急 | 重要 | 分类 | 步骤数 |
|----|------|------|------|------|------|------|--------|
| 1 | 完成项目原型设计 | LONG | TODO | true | true | 工作(2) | 3 |
| 2 | 购买生活用品 | ONCE | TODO | false | true | 购物(5) | 0 |
| 3 | 回复客户邮件 | ONCE | COMPLETED | false | false | 工作(2) | 0 |
| 4 | 学习 Compose 新特性 | LONG | IN_PROGRESS | true | false | 学习(1) | 5 |
| 5 | 晚间跑步 | ONCE | TODO | true | false | 健康(4) | 0 |
| 6 | 整理项目文档 | LONG | TODO | false | true | 工作(2) | 4 |

**示例步骤数据**（关联任务 1）：

| ID | 任务ID | 标题 | 排序 | 状态 |
|----|--------|------|------|------|
| 1 | 1 | 收集需求文档 | 0 | COMPLETED |
| 2 | 1 | 设计页面布局 | 1 | TODO |
| 3 | 1 | 制作交互原型 | 2 | TODO |

---

## 四、文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `app/src/main/java/com/gordon/mypotato/data/repository/FakeTaskRepository.kt` | 任务仓库内存实现 |
| 新建 | `app/src/main/java/com/gordon/mypotato/data/repository/FakeCategoryRepository.kt` | 分类仓库内存实现 |

---

## 五、验证标准

### 5.1 功能验证

| 验证项 | 通过条件 |
|--------|---------|
| 接口实现完整性 | FakeTaskRepository 实现 TaskRepository 所有 14 个方法 |
| 接口实现完整性 | FakeCategoryRepository 实现 CategoryRepository 所有 5 个方法 |
| 响应式通知 | 数据变更后 Flow 能正确通知所有订阅者 |
| 枚举映射 | 接口层传入枚举类型，内部正确转换为 Int 存储 |
| 默认分类加载 | 启动后能获取 5 个预置分类 |
| 默认任务加载 | 启动后能获取 6 个预置任务 |
| 级联删除 | 删除任务时关联步骤和会话一并删除 |
| 分类删除处理 | 删除分类时关联任务的 categoryId 正确置为 0 |

### 5.2 代码质量验证

| 验证项 | 通过条件 |
|--------|---------|
| 无编译错误 | Build 成功通过 |
| 无 Room 依赖 | FakeRepository 不引用任何 Room 相关类 |
| 线程安全 | ID 生成器使用 AtomicLong |
| 协程规范 | 所有写操作使用 suspend 函数 |
| Flow 规范 | 所有读操作返回 Flow，使用 StateFlow 实现响应式 |

---

## 六、风险与依赖

### 6.1 依赖项

| 依赖 | 状态 | 说明 |
|------|------|------|
| 领域模型 | 已完成 | Task/TaskStep/Category/PomodoroSession |
| Repository 接口 | 已完成 | TaskRepository/CategoryRepository/PomodoroRepository |

### 6.2 风险点

| 风险 | 缓解措施 |
|------|---------|
| 数据一致性 | 使用统一的内存数据源，避免多副本 |
| 线程安全 | 使用 AtomicLong 生成 ID，StateFlow 保证线程安全 |
| 内存泄漏 | 不持有 Context 引用，StateFlow 生命周期由订阅者管理 |
| 数据丢失 | Stage B 预期行为，Stage C 将接入 Room 持久化 |

---

## 七、后续集成计划

FakeRepository 完成后，下一步需要：
1. 创建 ViewModel 层，通过构造注入获取 Repository 实例
2. 迁移各页面的 Mock 数据至统一数据源
3. 打通新建/编辑/完成三条主流程闭环
# TaskRepository 接口定义计划

## 一、需求分析

### 1.1 业务背景

根据 Stage B 计划，需要定义 Repository 接口作为数据访问的统一入口，覆盖当前 UI 的所有数据操作需求。该接口将由 `FakeRepository`（内存实现）先行实现，后续 Stage C 再替换为 Room 实现。

### 1.2 UI 数据需求梳理

| 页面                 | 数据操作需求                | 对应接口方法                                                                                   |
| ------------------ | --------------------- | ---------------------------------------------------------------------------------------- |
| TodayFragment      | 获取任务列表、四象限筛选、切换完成状态   | `getTasks()`, `getTasksByQuadrant()`, `updateTaskStatus()`                               |
| TasksFragment      | 获取任务列表、多维筛选（分类/类型）、搜索 | `getTasks()`, `getTasksByCategory()`, `getTasksByType()`, `getTasksByQuery()`             |
| TaskDetailActivity | 获取任务详情、步骤列表、标记完成      | `getTaskById()`, `getStepsByTaskId()`, `updateTaskStatus()`                              |
| TaskEditActivity   | 任务/步骤增删改              | `addTask()`, `updateTask()`, `deleteTask()`, `addStep()`, `updateStep()`, `deleteStep()` |
| 全局                 | 获取分类列表                | `getCategories()`                                                                        |

### 1.3 领域模型依赖

已存在的领域模型位于 `domain/` 包：

* [Task.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/Task.kt)

* [TaskStep.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/TaskStep.kt)

* [Category.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/Category.kt)

* [TaskType.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/TaskType.kt)

* [TaskStatus.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/TaskStatus.kt)

* [StepStatus.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/StepStatus.kt)
* [PomodoroSession.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/PomodoroSession.kt)
* [SessionStatus.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/domain/SessionStatus.kt)

## 二、接口设计方案

### 2.1 接口定义原则

1. **聚合根拆分**：按业务边界拆分为独立 Repository 接口，遵循单一职责原则
2. **Flow 返回**：查询方法使用 `Flow` 实现响应式数据刷新，数据变更时自动通知 UI
3. **异步方法**：写操作和单次查询使用 `suspend` 函数，确保线程安全
4. **枚举类型安全**：接口层使用枚举类型（`TaskStatus`, `TaskType` 等）保证编译期类型安全，实现层负责枚举↔Int 的映射
5. **组合查询**：提供 `TaskQuery` 数据类支持多条件组合查询

### 2.2 聚合根拆分方案

| Repository 接口 | 职责 | 说明 |
|----------------|------|------|
| `TaskRepository` | 任务 CRUD、筛选、状态流转 | 包含 TaskStep 操作（步骤无独立生命周期） |
| `CategoryRepository` | 分类 CRUD | 独立聚合根 |
| `PomodoroRepository` | 番茄钟会话 CRUD | Stage B 仅定义接口，Stage D 实现 |

### 2.3 组合查询数据类

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

### 2.4 TaskRepository 接口方法

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `getTasks(): Flow<List<Task>>` | `Flow<List<Task>>` | 获取所有任务，支持响应式刷新 |
| `getTaskById(id: Long): suspend Task?` | `suspend Task?` | 根据 ID 获取单个任务，异步返回 |
| `getTasksByQuery(query: TaskQuery): Flow<List<Task>>` | `Flow<List<Task>>` | 多条件组合查询 |
| `getTasksByCategory(categoryId: Long): Flow<List<Task>>` | `Flow<List<Task>>` | 按分类筛选任务（便捷方法） |
| `getTasksByType(taskType: TaskType): Flow<List<Task>>` | `Flow<List<Task>>` | 按任务类型筛选（ONCE/LONG） |
| `getTasksByStatus(status: TaskStatus): Flow<List<Task>>` | `Flow<List<Task>>` | 按状态筛选（TODO/IN_PROGRESS/COMPLETED/ARCHIVED） |
| `getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>>` | `Flow<List<Task>>` | 按四象限筛选 |
| `addTask(task: Task): suspend Long` | `suspend Long` | 添加任务，返回新生成的 ID |
| `updateTask(task: Task): suspend Unit` | `suspend Unit` | 更新任务 |
| `updateTaskStatus(id: Long, status: TaskStatus): suspend Unit` | `suspend Unit` | 更新任务状态 |
| `deleteTask(id: Long): suspend Unit` | `suspend Unit` | 删除任务及关联步骤 |
| `getStepsByTaskId(taskId: Long): Flow<List<TaskStep>>` | `Flow<List<TaskStep>>` | 获取指定任务的步骤列表 |
| `addStep(step: TaskStep): suspend Long` | `suspend Long` | 添加步骤，返回新生成的 ID |
| `updateStep(step: TaskStep): suspend Unit` | `suspend Unit` | 更新步骤 |
| `updateStepStatus(id: Long, status: StepStatus): suspend Unit` | `suspend Unit` | 更新步骤状态（TODO/COMPLETED） |
| `deleteStep(id: Long): suspend Unit` | `suspend Unit` | 删除步骤 |

### 2.5 CategoryRepository 接口方法

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `getCategories(): Flow<List<Category>>` | `Flow<List<Category>>` | 获取所有分类 |
| `getCategoryById(id: Long): suspend Category?` | `suspend Category?` | 根据 ID 获取单个分类 |
| `addCategory(category: Category): suspend Long` | `suspend Long` | 添加分类，返回新生成的 ID |
| `updateCategory(category: Category): suspend Unit` | `suspend Unit` | 更新分类信息 |
| `deleteCategory(id: Long): suspend Unit` | `suspend Unit` | 删除分类，关联任务的 categoryId 置为 0（未分类） |

### 2.6 PomodoroRepository 接口方法（Stage B 仅定义，Stage D 实现）

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `addSession(session: PomodoroSession): suspend Long` | `suspend Long` | 添加番茄钟会话，返回新生成的 ID |
| `updateSession(session: PomodoroSession): suspend Unit` | `suspend Unit` | 更新番茄钟会话 |
| `updateSessionStatus(id: Long, status: SessionStatus): suspend Unit` | `suspend Unit` | 更新会话状态（IN_PROGRESS/COMPLETED/INTERRUPTED） |
| `deleteSession(id: Long): suspend Unit` | `suspend Unit` | 删除番茄钟会话 |
| `getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>` | `Flow<List<PomodoroSession>>` | 获取指定任务的番茄钟会话列表 |
| `getSessionById(id: Long): suspend PomodoroSession?` | `suspend PomodoroSession?` | 根据 ID 获取单个番茄钟会话，异步返回 |

### 2.7 设计说明

1. **返回类型选择**：查询方法使用 `Flow` 实现响应式数据刷新，数据变更时自动通知所有订阅者；单次查询和写操作使用 `suspend` 函数，确保在后台线程执行，避免阻塞主线程。

2. **枚举类型安全**：接口层使用枚举类型（`TaskStatus`, `TaskType`, `StepStatus`, `SessionStatus`）作为参数和返回值，保证编译期类型安全。实现层（FakeRepository/RoomRepository）负责枚举↔Int 的映射，与领域模型中的 Int 字段保持一致。

3. **聚合根拆分**：
   - `TaskRepository` 包含 TaskStep 操作，因为步骤是任务的子实体，无独立生命周期，删除任务时应级联删除步骤。
   - `CategoryRepository` 独立，分类是独立的聚合根。
   - `PomodoroRepository` 在 Stage B 仅定义接口，番茄钟会话在 Stage D 才落库实现。

4. **组合查询**：`TaskQuery` 数据类支持多条件组合查询，包含分类、类型、状态、四象限、关键词搜索和分页参数。同时保留单维度查询方法作为便捷入口。

5. **删除级联**：`deleteTask` 应删除关联的所有步骤和番茄钟会话；`deleteCategory` 应将关联任务的 `categoryId` 置为 0（未分类），具体实现由 `FakeRepository` 负责。

6. **无 Room 依赖**：接口仅依赖领域模型，不引入任何 Room 相关类（如 `@Dao`, `@Entity`），便于后续从 FakeRepository 无缝切换到 RoomRepository。

## 三、实施步骤

### 步骤 1：创建组合查询数据类

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/TaskQuery.kt`

**内容结构**：
* 包声明：`com.gordon.mypotato.data.repository`
* 导入必要的类（`TaskType`, `TaskStatus`）
* 定义 `data class TaskQuery`

### 步骤 2：创建 TaskRepository 接口

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/TaskRepository.kt`

**内容结构**：
* 包声明：`com.gordon.mypotato.data.repository`
* 导入必要的类（`Task`, `TaskStep`, `TaskType`, `TaskStatus`, `StepStatus`, `Flow`）
* 定义 `interface TaskRepository`
* 声明 16 个接口方法，每个方法添加标准中文注释

### 步骤 3：创建 CategoryRepository 接口

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/CategoryRepository.kt`

**内容结构**：
* 包声明：`com.gordon.mypotato.data.repository`
* 导入必要的类（`Category`, `Flow`）
* 定义 `interface CategoryRepository`
* 声明 5 个接口方法，每个方法添加标准中文注释

### 步骤 4：创建 PomodoroRepository 接口（Stage B 仅定义）

**文件路径**：`app/src/main/java/com/gordon/mypotato/data/repository/PomodoroRepository.kt`

**内容结构**：
* 包声明：`com.gordon.mypotato.data.repository`
* 导入必要的类（`PomodoroSession`, `SessionStatus`, `Flow`）
* 定义 `interface PomodoroRepository`
* 声明 6 个接口方法，每个方法添加标准中文注释

### 步骤 5：验证编译

运行 Gradle 编译命令，确保所有接口定义正确，无语法错误。

## 四、统计接口说明

统计相关接口（如按日期汇总、完成率统计、番茄钟时长统计等）**延后至 Stage C/D 定义**，理由如下：

1. **Stage B 目标聚焦**：当前阶段核心目标是跑通新建/编辑/标记完成三条主流程，统计页面不在核心流程范围内。
2. **需求依赖 UI**：统计接口的方法签名高度依赖统计页面的具体展示需求，现在定义容易出现设计偏差。
3. **降低交付复杂度**：统计接口的 FakeRepository 实现需要模拟时间分布、聚合计算等，会增加 Stage B 交付成本。
4. **扩展成本可控**：待统计 UI 开发时，在 Repository 接口中追加统计方法，同步更新 `FakeRepository` 即可。

建议在 Repository 接口中预留注释位置，后续根据统计页面设计补充相关方法。

## 五、潜在风险与注意事项

### 5.1 风险点

| 风险      | 描述                         | 应对策略                      |
| ------- | -------------------------- | ------------------------- |
| 接口方法不足  | 当前定义可能未覆盖所有未来需求            | 后续可根据实际需求扩展接口，但需同步更新所有实现类 |
| 枚举映射错误  | 实现层枚举↔Int 映射可能出错            | 定义统一的映射工具方法，确保映射逻辑集中管理       |
| 组合查询复杂度 | TaskQuery 字段过多可能导致实现复杂        | 按优先级实现，初期支持核心字段，后续按需扩展       |
| 接口拆分过度  | 过多 Repository 接口可能增加使用复杂度      | 提供 `AppRepository` 门面接口统一组合所有 Repository |

### 5.2 注意事项

1. **接口稳定性**：一旦定义，`FakeRepository` 必须严格遵守，避免实现侧新增方法。

2. **枚举映射策略**：实现层负责枚举↔Int 的双向映射，确保与领域模型中的 Int 字段保持一致。

3. **注释规范**：每个方法必须包含标准中文注释，说明功能、入参、返回值。

4. **接口无 Room 依赖**：所有 Repository 接口仅依赖领域模型，不引入任何 Room 相关类。

5. **suspend 方法调用**：调用方必须在协程作用域内执行 `suspend` 方法，建议使用 `viewModelScope` 或 `lifecycleScope`。

## 六、验收标准

1. ✅ `TaskQuery.kt` 文件创建完成
2. ✅ `TaskRepository.kt` 文件创建完成，包含 16 个接口方法
3. ✅ `CategoryRepository.kt` 文件创建完成，包含 5 个接口方法
4. ✅ `PomodoroRepository.kt` 文件创建完成，包含 6 个接口方法
5. ✅ 查询方法返回类型统一使用 `Flow`
6. ✅ 写操作和单次查询使用 `suspend` 函数
7. ✅ 接口层使用枚举类型（`TaskStatus`, `TaskType`, `StepStatus`, `SessionStatus`）
8. ✅ 包含 `TaskQuery` 组合查询方法
9. ✅ 方法注释完整规范（中文）
10. ✅ 项目编译通过，无语法错误
11. ✅ 接口不依赖 Room 相关类

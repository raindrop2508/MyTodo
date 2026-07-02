# Repository 接口实现计划

## 一、需求分析

根据 `TaskRepository 接口定义计划.md`，需要实现以下 Repository 接口：

1. **TaskQuery** - 组合查询数据类
2. **TaskRepository** - 任务 CRUD 接口（16 个方法）
3. **CategoryRepository** - 分类 CRUD 接口（5 个方法）
4. **PomodoroRepository** - 番茄钟会话接口（6 个方法，Stage B 仅定义）

### 核心设计原则

| 原则 | 说明 |
|------|------|
| **Flow 返回** | 查询方法使用 `Flow` 实现响应式数据刷新 |
| **异步方法** | 写操作和单次查询使用 `suspend` 函数 |
| **枚举类型安全** | 接口层使用枚举类型（`TaskStatus`, `TaskType` 等） |
| **组合查询** | 提供 `TaskQuery` 数据类支持多条件组合查询 |
| **无 Room 依赖** | 接口仅依赖领域模型，不引入 Room 相关类 |

## 二、依赖分析

### 2.1 当前依赖状态

| 依赖 | 当前版本 | 是否可用 |
|------|---------|---------|
| Kotlin | 2.0.21 | ✅ Flow 已内置 |
| androidx-core-ktx | 1.10.1 | ✅ 已包含 |
| androidx-lifecycle-viewmodel-ktx | 2.8.7 | ✅ `viewModelScope` 已包含 |
| androidx-lifecycle-livedata-ktx | 2.8.7 | ✅ 已包含（备用） |

### 2.2 新增依赖分析

**无需新增依赖**，理由：
- **Flow**：Kotlin 1.4+ 标准库已内置 `kotlinx.coroutines.flow.Flow`
- **suspend**：Kotlin 协程已通过 `androidx-lifecycle-viewmodel-ktx` 依赖引入
- **Coroutines**：通过 `androidx-core-ktx` 和 `androidx-lifecycle-viewmodel-ktx` 间接引入

### 2.3 验证依赖命令

```bash
./gradlew app:dependencies --configuration compileClasspath | grep "coroutines"
```

## 三、文件结构

### 3.1 新建文件

| 文件路径 | 文件说明 |
|---------|---------|
| `app/src/main/java/com/gordon/mypotato/data/repository/TaskQuery.kt` | 组合查询数据类 |
| `app/src/main/java/com/gordon/mypotato/data/repository/TaskRepository.kt` | 任务 Repository 接口 |
| `app/src/main/java/com/gordon/mypotato/data/repository/CategoryRepository.kt` | 分类 Repository 接口 |
| `app/src/main/java/com/gordon/mypotato/data/repository/PomodoroRepository.kt` | 番茄钟 Repository 接口 |

### 3.2 依赖的领域模型

| 文件路径 | 说明 |
|---------|------|
| `domain/Task.kt` | 任务实体 |
| `domain/TaskStep.kt` | 步骤实体 |
| `domain/Category.kt` | 分类实体 |
| `domain/PomodoroSession.kt` | 番茄钟会话实体 |
| `domain/TaskStatus.kt` | 任务状态枚举 |
| `domain/TaskType.kt` | 任务类型枚举 |
| `domain/StepStatus.kt` | 步骤状态枚举 |
| `domain/SessionStatus.kt` | 会话状态枚举 |

## 四、实施步骤

### 步骤 1：创建组合查询数据类

**文件：** `data/repository/TaskQuery.kt`

```kotlin
package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskType

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

### 步骤 2：创建 TaskRepository 接口

**文件：** `data/repository/TaskRepository.kt`

包含 16 个方法：
- 7 个查询方法（返回 Flow）
- 1 个单次查询方法（suspend）
- 9 个写操作方法（suspend）

### 步骤 3：创建 CategoryRepository 接口

**文件：** `data/repository/CategoryRepository.kt`

包含 5 个方法：
- 1 个查询方法（返回 Flow）
- 1 个单次查询方法（suspend）
- 3 个写操作方法（suspend）

### 步骤 4：创建 PomodoroRepository 接口

**文件：** `data/repository/PomodoroRepository.kt`

包含 6 个方法：
- 1 个查询方法（返回 Flow）
- 1 个单次查询方法（suspend）
- 4 个写操作方法（suspend）

### 步骤 5：验证编译

运行 Gradle 编译命令验证接口定义：

```bash
./gradlew app:compileDebugKotlin
```

## 五、接口方法详情

### 5.1 TaskRepository 接口方法

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `getTasks(): Flow<List<Task>>` | `Flow<List<Task>>` | 获取所有任务 |
| `getTaskById(id: Long): suspend Task?` | `suspend Task?` | 根据 ID 获取单个任务 |
| `getTasksByQuery(query: TaskQuery): Flow<List<Task>>` | `Flow<List<Task>>` | 多条件组合查询 |
| `getTasksByCategory(categoryId: Long): Flow<List<Task>>` | `Flow<List<Task>>` | 按分类筛选 |
| `getTasksByType(taskType: TaskType): Flow<List<Task>>` | `Flow<List<Task>>` | 按任务类型筛选 |
| `getTasksByStatus(status: TaskStatus): Flow<List<Task>>` | `Flow<List<Task>>` | 按状态筛选 |
| `getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>>` | `Flow<List<Task>>` | 按四象限筛选 |
| `getStepsByTaskId(taskId: Long): Flow<List<TaskStep>>` | `Flow<List<TaskStep>>` | 获取步骤列表 |
| `addTask(task: Task): suspend Long` | `suspend Long` | 添加任务 |
| `updateTask(task: Task): suspend Unit` | `suspend Unit` | 更新任务 |
| `updateTaskStatus(id: Long, status: TaskStatus): suspend Unit` | `suspend Unit` | 更新任务状态 |
| `deleteTask(id: Long): suspend Unit` | `suspend Unit` | 删除任务 |
| `addStep(step: TaskStep): suspend Long` | `suspend Long` | 添加步骤 |
| `updateStep(step: TaskStep): suspend Unit` | `suspend Unit` | 更新步骤 |
| `updateStepStatus(id: Long, status: StepStatus): suspend Unit` | `suspend Unit` | 更新步骤状态 |
| `deleteStep(id: Long): suspend Unit` | `suspend Unit` | 删除步骤 |

### 5.2 CategoryRepository 接口方法

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `getCategories(): Flow<List<Category>>` | `Flow<List<Category>>` | 获取所有分类 |
| `getCategoryById(id: Long): suspend Category?` | `suspend Category?` | 根据 ID 获取单个分类 |
| `addCategory(category: Category): suspend Long` | `suspend Long` | 添加分类 |
| `updateCategory(category: Category): suspend Unit` | `suspend Unit` | 更新分类 |
| `deleteCategory(id: Long): suspend Unit` | `suspend Unit` | 删除分类 |

### 5.3 PomodoroRepository 接口方法

| 方法签名 | 返回类型 | 说明 |
|---------|---------|------|
| `getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>` | `Flow<List<PomodoroSession>>` | 获取会话列表 |
| `getSessionById(id: Long): suspend PomodoroSession?` | `suspend PomodoroSession?` | 根据 ID 获取单个会话 |
| `addSession(session: PomodoroSession): suspend Long` | `suspend Long` | 添加会话 |
| `updateSession(session: PomodoroSession): suspend Unit` | `suspend Unit` | 更新会话 |
| `updateSessionStatus(id: Long, status: SessionStatus): suspend Unit` | `suspend Unit` | 更新会话状态 |
| `deleteSession(id: Long): suspend Unit` | `suspend Unit` | 删除会话 |

## 六、注意事项

### 6.1 枚举类型安全

接口层使用枚举类型（`TaskStatus`, `TaskType` 等），实现层（FakeRepository/RoomRepository）负责枚举↔Int 的映射。领域模型中已提供 `fromValue(value: Int)` 方法用于反向转换。

### 6.2 Flow 依赖

Flow 属于 `kotlinx.coroutines.flow` 包，已通过 Kotlin 标准库引入，无需额外依赖。

### 6.3 suspend 方法调用

调用方必须在协程作用域内执行 `suspend` 方法，建议使用 `viewModelScope`（ViewModel 层）或 `lifecycleScope`（UI 层）。

### 6.4 无 Room 依赖

接口仅依赖领域模型（`domain/` 包），不引入任何 Room 相关类，便于后续从 FakeRepository 无缝切换到 RoomRepository。

## 七、风险处理

| 风险 | 应对策略 |
|------|---------|
| Flow 导入路径错误 | 使用 `kotlinx.coroutines.flow.Flow`，确保正确导入 |
| 枚举类型不匹配 | 确认领域模型的枚举类与接口参数一致 |
| 编译错误 | 使用 `./gradlew app:compileDebugKotlin` 验证 |

## 八、验收标准

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

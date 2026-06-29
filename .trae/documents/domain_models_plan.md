# 领域模型数据类创建计划

## 一、需求分析

根据 [领域数据模型定义.md](file:///c:/code/MyTodo/docs/stageB/领域数据模型定义.md)，需要创建以下领域模型数据类：

| 模型              | 状态 | 说明                  |
| --------------- | -- | ------------------- |
| Task            | 核心 | 任务实体，包含四象限、状态、分类等字段 |
| TaskStep        | 核心 | 任务步骤实体              |
| Category        | 核心 | 分类实体                |
| PomodoroSession | 核心 | 番茄钟会话实体             |

> **StatisticsCache（统计缓存）**：属于 Stage D，用于缓存统计聚合结果以提升性能。当前阶段数据量小，实时 SQL 查询足够，暂不创建。

## 二、文件结构规划

新建 `domain/` 包，位置：`app/src/main/java/com/gordon/mypotato/domain/`

```
domain/
├── TaskType.kt           # 任务类型枚举（ONCE=0, LONG=1）
├── TaskStatus.kt         # 任务状态枚举（TODO=0, IN_PROGRESS=1, COMPLETED=2, ARCHIVED=3）
├── StepStatus.kt         # 步骤状态枚举（TODO=0, COMPLETED=1）
├── SessionStatus.kt      # 番茄钟会话状态枚举（IN_PROGRESS=0, COMPLETED=1, INTERRUPTED=2）
├── Task.kt               # 任务数据类
├── TaskStep.kt           # 任务步骤数据类
├── Category.kt           # 分类数据类
└── PomodoroSession.kt    # 番茄钟会话数据类
```

## 三、数据类设计要点

### 3.1 类型约束

* ID 使用 `Long`

* 枚举值使用 `Int`（通过枚举类的 `value` 属性）

* 时间戳使用 `Long`（秒）

* 可选字段使用 `?` 标记

### 3.2 枚举类设计

每个枚举类包含：

* `value: Int` 字段，存储数据库/网络传输值

* `fromValue(value: Int)` 静态方法，用于反序列化

### 3.3 数据类字段对照

**Task.kt：**

| 字段名              | 类型      | 可空性 | 默认值      |
| ---------------- | ------- | --- | -------- |
| id               | Long    | 否   | -        |
| title            | String  | 否   | -        |
| content          | String  | 是   | null     |
| note             | String  | 是   | null     |
| taskType         | Int     | 否   | 0 (ONCE) |
| status           | Int     | 否   | 0 (TODO) |
| isUrgent         | Boolean | 否   | false    |
| isImportant      | Boolean | 否   | false    |
| categoryId       | Long    | 否   | 0        |
| createdAt        | Long    | 否   | -        |
| plannedStartAt   | Long?   | 是   | null     |
| finishedAt       | Long?   | 是   | null     |
| totalDurationSec | Long    | 否   | 0        |

**TaskStep.kt：**

| 字段名              | 类型     | 可空性 | 默认值  |
| ---------------- | ------ | --- | ---- |
| id               | Long   | 否   | -    |
| taskId           | Long   | 否   | -    |
| title            | String | 否   | -    |
| sortOrder        | Int    | 否   | -    |
| status           | Int    | 否   | 0    |
| completedAt      | Long?  | 是   | null |
| spentDurationSec | Long   | 否   | 0    |
| createdAt        | Long   | 否   | -    |

**Category.kt：**

| 字段名      | 类型     | 可空性 | 默认值  |
| -------- | ------ | --- | ---- |
| id       | Long   | 否   | -    |
| name     | String | 否   | -    |
| colorHex | String | 否   | -    |
| iconName | String | 是   | null |

**PomodoroSession.kt：**

| 字段名              | 类型    | 可空性 | 默认值  |
| ---------------- | ----- | --- | ---- |
| id               | Long  | 否   | -    |
| taskId           | Long  | 否   | -    |
| stepId           | Long? | 是   | null |
| startedAt        | Long  | 否   | -    |
| endedAt          | Long? | 是   | null |
| focusDurationSec | Long  | 否   | 0    |
| breakDurationSec | Long  | 否   | 0    |
| cycles           | Int   | 否   | 0    |
| status           | Int   | 否   | 0    |

## 四、实施步骤

### 步骤 1：创建枚举类（4 个文件）

* TaskType.kt

* TaskStatus.kt

* StepStatus.kt

* SessionStatus.kt

### 步骤 2：创建核心数据类（4 个文件）

* Task.kt

* TaskStep.kt

* Category.kt

* PomodoroSession.kt

### 步骤 3：编译验证

运行 `gradlew compileDebugKotlin` 验证代码正确性

## 五、潜在依赖与注意事项

1. **与现有代码的关系**：新创建的 domain 包独立于现有 `data/entity/`（Room Entity），Stage C 时会通过 Mapper 进行映射
2. **无 Room 注解**：Stage B 的领域模型是纯 Kotlin 数据类，不包含 `@Entity`、`@ColumnInfo` 等 Room 注解
3. **空值处理**：`content`、`note`、`iconName` 等可选字段使用 `String?`，创建任务时允许为空
4. **默认值约定**：

   * `categoryId = 0` 表示"未分类"

   * `taskType = 0` 表示"单次任务"

   * `status = 0` 表示"未开始/未完成"

## 六、风险处理

| 风险         | 应对策略                  |
| ---------- | --------------------- |
| 字段类型与文档不一致 | 严格按照文档定义实现，如有疑问及时确认   |
| 编译错误       | 逐个文件创建并验证，避免一次性创建所有文件 |
| 枚举值与现有代码冲突 | 使用独立枚举类，与现有 UI 层代码解耦  |

## 七、验证标准

* 所有 8 个文件创建完成

* `gradlew compileDebugKotlin` 编译通过

* 数据类字段与 [领域数据模型定义.md](file:///c:/code/MyTodo/docs/stageB/领域数据模型定义.md) 一致

* 枚举类包含 `value` 字段和 `fromValue` 方法


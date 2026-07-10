# MyPotato 术语表

> 状态：有效
> 最后更新：2026-07-10
> 适用范围：项目文档统一术语口径
> 目标读者：后续 AI 执行者、项目维护者、新接手开发者

---

## 一、术语使用约定

- 本表用于统一文档中的核心术语含义，减少多文档并行阅读时的歧义。
- 若同一术语在不同阶段存在不同口径，应以当前阶段权威文档为准。
- 若未来术语定义发生变化，应同步更新本表与对应权威文档。

---

## 二、核心术语

### 1. 本地离线优先（Local First）

- 定义：应用核心功能以本地数据可用、无云依赖为前提设计。
- 本项目落点：任务、步骤、分类、番茄钟数据以本地存储为主，导入导出属于增强能力。
- 参考文档： [项目整体规划文档](file:///e:/code/MyPotato/docs/plan/项目整体规划文档.md)

### 2. 四象限法（Eisenhower Matrix）

- 定义：基于“是否紧急”和“是否重要”两个维度对任务进行分类的方法。
- 本项目落点：`Task` 通过 `isUrgent` 与 `isImportant` 表达四象限属性。
- 参考文档： [B1：领域数据模型定义](file:///e:/code/MyPotato/docs/stageB/B1：领域数据模型定义.md)

### 3. 番茄工作法（Pomodoro Technique）

- 定义：通过专注时段与休息时段交替运行的时间管理方法。
- 本项目落点：仅长时任务可进入番茄钟，相关会话由 `PomodoroSession` 表示。
- 参考文档： [项目整体规划文档](file:///e:/code/MyPotato/docs/plan/项目整体规划文档.md)

### 4. 仓储模式（Repository Pattern）

- 定义：通过统一的数据访问接口屏蔽底层数据来源差异的模式。
- 本项目落点：UI 与 ViewModel 只依赖 `TaskRepository`、`CategoryRepository`、`PomodoroRepository` 接口，不直接依赖内存实现或数据库实现。
- 参考文档： [StageB_整体实现总结](file:///e:/code/MyPotato/docs/stageB/StageB_整体实现总结.md)

### 5. 假仓储（Fake Repository）

- 定义：不依赖真实数据库、以内存数据模拟真实数据访问行为的仓储实现。
- 本项目落点：Stage B 通过 `FakeTaskRepository`、`FakeCategoryRepository`、`FakePomodoroRepository` 先跑通业务链路。
- 参考文档： [StageB_整体实现总结](file:///e:/code/MyPotato/docs/stageB/StageB_整体实现总结.md)

### 6. 响应式数据流（Reactive Data Flow）

- 定义：数据源变化后，通过可观察流自动驱动上层状态与界面刷新。
- 本项目落点：`Repository -> Flow/StateFlow -> ViewModel -> UI`
- 参考文档： [B4：Today页面Fake数据接入Flow到ViewModel再到UI的数据流总结](file:///e:/code/MyPotato/docs/stageB/B4：Today页面Fake数据接入Flow到ViewModel再到UI的数据流总结.md)

### 7. 领域模型（Domain Model）

- 定义：与具体存储技术无关、只表达业务实体和业务规则的数据模型。
- 本项目落点：`Task`、`TaskStep`、`Category`、`PomodoroSession` 及其相关枚举。
- 参考文档： [B1：领域数据模型定义](file:///e:/code/MyPotato/docs/stageB/B1：领域数据模型定义.md)

### 8. 视图模型（ViewModel）

- 定义：负责持有 UI 状态、组织数据读取与写操作、向界面暴露稳定状态的中间层。
- 本项目落点：`TodayViewModel`、`TasksViewModel`、`TaskDetailViewModel` 等页面级 ViewModel。
- 参考文档： [StageB_整体实现总结](file:///e:/code/MyPotato/docs/stageB/StageB_整体实现总结.md)

### 9. 单次任务（Once Task）

- 定义：一次性完成的短事务型任务，不参与番茄钟计时。
- 本项目落点：`TaskType.ONCE`
- 参考文档： [B1：领域数据模型定义](file:///e:/code/MyPotato/docs/stageB/B1：领域数据模型定义.md)

### 10. 长时任务（Long Task）

- 定义：需要持续投入、可累计专注时长的任务类型。
- 本项目落点：`TaskType.LONG`，允许进入番茄钟页面。
- 参考文档： [B1：领域数据模型定义](file:///e:/code/MyPotato/docs/stageB/B1：领域数据模型定义.md)

### 11. Stage A-E（项目阶段）

- 定义：项目按阶段推进的里程碑划分，从 UI 骨架到数据落地、统计闭环、质量加固。
- 本项目落点：
  - Stage A：UI 骨架
  - Stage B：FakeRepository + 核心流程跑通
  - Stage C：Room 落地
  - Stage D：长时任务与统计闭环
  - Stage E：架构补齐与质量加固
- 参考文档： [项目整体规划文档](file:///e:/code/MyPotato/docs/plan/项目整体规划文档.md)

### 12. 安全参数传递（Safe Args）

- 定义：Jetpack Navigation 提供的类型安全导航参数生成机制。
- 本项目落点：Stage B 已部分落地，但仍存在与显式 `Intent` 并存的情况。
- 参考文档： [StageB_整体实现总结](file:///e:/code/MyPotato/docs/stageB/StageB_整体实现总结.md)

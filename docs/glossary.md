# MyPotato 术语表

> 状态：有效  
> 最后更新：2026-09-05  
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
- 本项目落点：任务、步骤、分类、番茄钟数据以本地 Room 存储为主，导入导出属于增强能力。
- 参考文档：[项目整体规划文档](plan/项目整体规划文档.md)

### 2. 四象限法（Eisenhower Matrix）

- 定义：基于「是否紧急」和「是否重要」两个维度对任务进行分类的方法。
- 本项目落点：`Task` 通过 `isUrgent` 与 `isImportant` 表达四象限属性；Today 页以四象限 Chip 筛选。
- 参考文档：[B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md)

### 3. 番茄工作法（Pomodoro Technique）

- 定义：通过专注时段与休息时段交替运行的时间管理方法。
- 本项目落点：仅长时任务可进入番茄钟；会话由 `PomodoroSession` 表示，阶段由 `PomodoroPhase` 表示。
- 参考文档：[项目整体规划文档](plan/项目整体规划文档.md)、[番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)

### 4. 仓储模式（Repository Pattern）

- 定义：通过统一的数据访问接口屏蔽底层数据来源差异的模式。
- 本项目落点：UI / ViewModel 只依赖 `TaskRepository`、`CategoryRepository`、`PomodoroRepository` 接口；运行时实现为 `Room*Repository`。
- 参考文档：[StageC_Room数据库实现总结](plan/stageC/StageC_Room数据库实现总结.md)

### 5. 假仓储（Fake Repository）

- 定义：不依赖真实数据库、以内存数据模拟真实数据访问行为的仓储实现。
- 本项目落点：**Stage B 历史实现**；工程中已移除 `Fake*Repository`，仅文档保留概念说明。
- 参考文档：[StageB_整体实现总结](plan/stageB/StageB_整体实现总结.md)

### 6. 响应式数据流（Reactive Data Flow）

- 定义：数据源变化后，通过可观察流自动驱动上层状态与界面刷新。
- 本项目落点：`Room DAO (Flow) -> Repository -> ViewModel (StateFlow) -> UI`
- 参考文档：[项目技术架构与实现说明](项目技术架构与实现说明.md)

### 7. 领域模型（Domain Model）

- 定义：与具体存储技术无关、只表达业务实体和业务规则的数据模型。
- 本项目落点：`Task`、`TaskStep`、`Category`、`PomodoroSession` 及枚举；Entity 与 Domain 通过 `EntityMapper` 双向转换。
- 参考文档：[B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md)

### 8. 视图模型（ViewModel）

- 定义：负责持有 UI 状态、组织数据读写、向界面暴露稳定状态的中间层。
- 本项目落点：`TodayViewModel`、`TasksViewModel`、`TaskDetailViewModel`、`TaskEditViewModel`、`PomodoroViewModel`、`SettingsViewModel`；由 `ViewModelFactory` 注入 RoomRepository。
- 参考文档：[项目技术架构与实现说明](项目技术架构与实现说明.md)

### 9. 单次任务（Once Task）

- 定义：一次性完成的短事务型任务，不参与番茄钟计时。
- 本项目落点：`TaskType.ONCE`；详情页隐藏番茄钟入口；`PomodoroViewModel` 二次校验拒绝进入。
- 参考文档：[B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md)

### 10. 长时任务（Long Task）

- 定义：需要持续投入、可累计专注时长的任务类型。
- 本项目落点：`TaskType.LONG`，允许进入番茄钟页面并落库会话。
- 参考文档：[B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md)

### 11. Stage A–E（项目阶段）

- 定义：项目按阶段推进的里程碑划分。
- 本项目落点：
  - Stage A：UI 骨架 ✅
  - Stage B：Repository 接口 + 核心流程跑通 ✅
  - Stage C：Room 业务实体落地 ✅
  - Stage D：番茄钟会话落库与统计闭环 ⏳（D1 已完成，统计待做）
  - Stage E：架构补齐与质量加固 ⏳（活动计时持久化专题已落地）
- 参考文档：[项目整体规划文档](plan/项目整体规划文档.md)

### 12. 安全参数传递（Safe Args）

- 定义：Jetpack Navigation 提供的类型安全导航参数生成机制。
- 本项目落点：底部导航 Fragment 使用 Navigation；深层页（TaskDetail / TaskEdit / Pomodoro）多为独立 Activity + Intent extras，与 Safe Args 并存。
- 参考文档：[StageB_整体实现总结](plan/stageB/StageB_整体实现总结.md)

### 13. 番茄钟阶段（Pomodoro Phase）

- 定义：单次会话所处的计时段类型。
- 本项目落点：`PomodoroPhase` — `FOCUS` / `SHORT_BREAK` / `LONG_BREAK`；休息结束后回到专注；仅专注完成增加 cycle。
- 参考文档：[番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)

### 14. 活动计时持久化（Active Timer Persistence）

- 定义：将运行中/暂停中的番茄钟字段写入 Room，使进程被杀后仍可识别未完成会话。
- 本项目落点：`phase`、`plannedDurationMs`、`targetEndEpochMs`、`remainingMsWhenPaused`、`pauseStartedAtEpochMs`；开始/暂停/继续/重置/完成均先落库；全库活动会话 ≤ 1。
- 参考文档：[番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)

### 15. 孤儿会话收尾（Orphan Pomodoro Settlement）

- 定义：冷启动时对残留活动会话（`IN_PROGRESS` / `PAUSED`）的用户确认收尾流程。
- 本项目落点：`OrphanPomodoroSettlement` + `MainActivity` 弹窗；专注阶段可选「保留时长」→ `COMPLETED`（不改 Task/Step）或「不保留」→ `INTERRUPTED`；休息阶段直接中断。
- 参考文档：[番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)

### 16. 统计口径（Statistics Scope）

- 定义：统计页应纳入哪些会话时长的业务规则。
- 本项目落点（已定规则，统计页尚未接真数据）：仅统计 `phase == FOCUS` 且 `status == COMPLETED` 的会话专注时长。
- 参考文档：[番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)、[StageD 计划](plan/stageD/StageD_番茄钟会话落库与统计闭环计划.md)

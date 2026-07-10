# Stage B：整体实现总结

> 文档版本：v1.0\
> 更新日期：2026-07-08\
> 依据范围：`c:\code\MyTodo\docs\stageB\StageB_核心流程跑通计划.md` 与当前项目代码实现\
> 适用阶段：MyPotato Stage B 收尾总结 / Stage C 交接参考

***

## 一、阶段目标回顾

Stage B 的核心目标不是引入数据库（Database），而是先建立一套稳定的“领域模型（Domain Model） + 仓储（Repository） + 假仓储（Fake Repository） + 视图模型（ViewModel） + UI”响应式链路，让当前应用在不依赖 Room 的前提下跑通核心业务流程。

围绕该目标，Stage B 实际完成了三件关键事情：

1. 建立统一的数据契约，消除页面内分散的临时模型与本地假数据。
2. 使用内存版仓储与 `Flow` / `StateFlow` 打通多页面共享数据源。
3. 以 `ViewModel` 为中心收口读写逻辑，完成“新增任务、编辑任务、标记完成”三条核心闭环。

从当前代码现状看，**Stage B 主体目标已经完成，B1-B6、B8、B9 已基本落地，B7 为部分完成，B3 存在一处与计划不完全一致的实现差异。**

***

## 二、整体架构落地

## 1. 领域层（Domain Layer）

Stage B 已将任务系统涉及的核心业务实体统一沉淀到 `domain` 目录：

- `Task.kt`
- `TaskStep.kt`
- `Category.kt`
- `TaskType.kt`
- `TaskStatus.kt`
- `StepStatus.kt`
- `PomodoroSession.kt`
- `SessionStatus.kt`

这一层统一了以下约束：

- 主键统一使用 `Long`
- 状态与类型统一使用 `Int` 存储值 + 枚举（Enum）辅助转换
- 时间统一使用时间戳（Timestamp）
- 长时任务（Long Task）与单次任务（Once Task）在领域层已有明确区分

这意味着页面不再自行维护 `TodayTask`、`TaskUiModel`、`StepMock` 之类的局部模型，而是围绕统一领域模型开发。

## 2. 仓储层（Repository Layer）

Stage B 已定义 3 组仓储接口：

- `TaskRepository`
- `CategoryRepository`
- `PomodoroRepository`

接口层的关键设计已经明确：

- 读取列表统一使用 `Flow`
- 单次读取与写操作统一使用 `suspend`
- 查询条件通过 `TaskQuery` 统一封装
- UI 与 ViewModel 只依赖接口，不依赖具体实现

这一步的价值在于：**当前可以接 FakeRepository，后续 Stage C 可以平滑替换为 RoomRepository，而上层页面和 ViewModel 无需整体返工。**

## 3. 假仓储层（Fake Repository Layer）

当前项目使用的并不是数据库，而是内存实现：

- `FakeTaskRepository`
- `FakeCategoryRepository`
- `FakePomodoroRepository`

其实现特点如下：

- 使用 `MutableList` 持有内存数据
- 使用 `MutableStateFlow` 对外暴露可观察数据快照
- 使用自增 ID 生成器维护任务、步骤、分类、会话标识
- 预置默认分类与演示任务
- 任务、步骤、番茄钟会话已具备基本关联关系

这部分实现是 Stage B 响应式刷新的基础。页面之所以能联动，不是因为手动刷新，而是因为所有页面订阅的是同一份仓储数据流。

## 4. 视图模型层（ViewModel Layer）

Stage B 已补齐计划中的 6 个核心 `ViewModel`：

- `TodayViewModel`
- `TasksViewModel`
- `TaskDetailViewModel`
- `TaskEditViewModel`
- `PomodoroViewModel`
- `SettingsViewModel`

此外，还抽出了 `BaseTaskViewModel` 与 `ViewModelFactory`：

- `BaseTaskViewModel` 负责沉淀任务增删改、状态更新等公共写操作
- `ViewModelFactory` 负责统一创建各页面 `ViewModel`，集中装配 `TaskRepository`、`CategoryRepository`、`PomodoroRepository` 与 `Context` 等依赖，并在创建 `PomodoroViewModel` 时补充读取 `SharedPreferences` 中的专注时长、短休息、长休息与长休息间隔等设置项，避免 `Activity`/`Fragment` 直接拼装依赖，确保不同页面复用同一套仓储实例与同一份内存数据源

这一步完成后，UI 层的职责明显收敛为两类：

- 收集状态（Collect State）
- 派发事件（Dispatch Event）

而数据读取、状态聚合、异步写入、异常处理则集中在 `ViewModel` 内部完成。

## 5. UI 层（Presentation Layer）

Stage B 之后，Today、Tasks、TaskDetail、TaskEdit、Pomodoro、Settings 等页面都已不再以“页面自带假数据”的方式运行，而是改为：

`UI -> ViewModel -> Repository -> Flow -> ViewModel -> UI`

这条链路已经成为当前项目的主数据流骨架，也是 Stage C 引入 Room 的直接承接点。

***

## 三、B1-B9 落地情况总结

## B1：定义领域模型（Domain Models）

**落地结论：已完成。**

当前代码已经完整包含计划中要求的 8 个领域模型文件，字段约束、枚举值约束、可空时间字段约束均已建立。该部分已经为后续 Repository 与 Room 落地提供稳定契约。

## B2：定义 Repository 接口契约

**落地结论：已完成。**

当前 `TaskRepository`、`CategoryRepository`、`PomodoroRepository` 与 `TaskQuery` 已创建完成，接口分层清晰，读写边界明确，符合 Stage B 计划中的接口设计目标。

## B3：实现 FakeRepository（内存实现）

**落地结论：基本完成，但有一处差异。**

当前 FakeRepository 体系已经支持：

- 任务 CRUD
- 步骤 CRUD
- 番茄钟会话 CRUD
- `Flow` 推送刷新
- 默认分类与默认任务初始化
- 任务删除时关联步骤清理
- 任务状态、步骤状态、累计时长等逻辑维护

但与计划相比，仍有一处未完全满足：

- `FakeCategoryRepository.deleteCategory(id)` 当前只删除分类本身
- **尚未实现“删除分类后，将关联任务的 `categoryId` 置为 `0`（未分类）”**

因此，B3 更准确的状态应理解为：**主体完成，验收细项存在遗漏。**

## B4：补齐各页面 ViewModel

**落地结论：已完成。**

当前项目已形成较完整的 `ViewModel` 体系，且实现上有几个值得确认的优化点：

- 所有主要页面均改为通过 `ViewModelFactory` 获取依赖
- 公共写操作下沉到 `BaseTaskViewModel`
- `ViewModel` 内部使用 `viewModelScope` 收口异步写操作
- UI 层不再需要直接调用仓储层挂起函数
- `PomodoroViewModel` 与 `SettingsViewModel` 也已落地，不再处于占位状态

这一步标志着 Stage B 已从“单页演示逻辑”走向“多页面一致的 MVVM（Model-View-ViewModel）结构”。

## B5：迁移 Mock 数据至统一数据源

**落地结论：已完成。**

当前页面层已基本清理以下旧模式：

- 页面内部 `buildMockTasks()`
- 页面内部 `buildMockSteps()`
- 分散的 `TodayTask`、`TaskUiModel`、`StepMock`

页面展示数据统一来自 Repository，经由 ViewModel 暴露给 UI。这一变化直接解决了“多页面各自维护一套假数据、无法联动”的问题。

## B6：打通三条主流程闭环

**落地结论：已完成。**

Stage B 计划中的三条主流程已经跑通：

1. 新建任务 -> 保存 -> 列表刷新
2. 编辑任务 -> 保存 -> 详情刷新
3. 标记完成 -> 状态即时可见

其实现基础不是手动页面回写，而是：

- 仓储层写入后更新 `StateFlow`
- `ViewModel` 持续收集仓储数据流
- UI 持续收集 `uiState`
- 任意一处写入触发全链路状态回流

因此，这部分不仅是“功能能跑通”，更是“响应式闭环已经成立”。

## B7：导航参数规范化

**落地结论：部分完成。**

当前项目已经做了以下工作：

- 启用了 Safe Args（安全参数传递）
- 在 `main_nav_graph.xml` 中为 `taskDetail`、`taskEdit`、`pomodoro` 声明了参数
- `TodayFragment -> TaskDetail` 与 `TasksFragment -> TaskDetail` 已使用 `Directions`
- `TaskDetailActivity`、`TaskEditActivity`、`PomodoroActivity` 已使用 `Args.fromBundle(...)` 解析参数
- `TaskDetail -> Pomodoro` 已补齐 `taskId + taskTitle` 传递

但当前实现并不是“全链路纯 Safe Args”：

- `TaskDetail -> TaskEdit`
- `TaskDetail -> Pomodoro`

这两条链路的发起端仍使用显式 `Intent` 手动 `putExtra(...)` 传值，只是在接收端继续通过 `Args` 读取。

所以，B7 的真实状态更适合表述为：

- **参数协议已基本统一**
- **Safe Args 已部分落地**
- **尚未形成完全一致的导航实现风格**

## B8：规则一致性校验

**落地结论：已完成。**

当前“单次任务不能进入番茄钟”这一规则已经在两层落地：

1. `TaskDetailActivity` 中对单次任务直接隐藏番茄钟入口
2. `PomodoroViewModel` 在页面加载任务时再次校验任务类型，若不是长时任务则标记为无效任务并给出错误提示

也就是说，项目已经具备：

- UI 层前置约束
- 业务层兜底校验

规则一致性目标已满足。

## B9：设置项接入（番茄钟时长）

**落地结论：已完成。**

当前设置页与番茄钟页之间已建立以下链路：

`SettingsFragment -> SettingsViewModel -> SharedPreferences -> ViewModelFactory -> PomodoroViewModel`

目前已接入的设置项包括：

- 专注时长 `focusMinutes`
- 短休息时长 `shortBreakMinutes`
- 长休息时长 `longBreakMinutes`
- 长休息间隔 `longBreakInterval`

这说明 Stage B 已不再使用硬编码的 25 分钟作为唯一专注时长，而是支持通过设置页进行持久化配置。

***

## 四、三条主流程是如何跑通的

## 1. 新建任务 -> 保存 -> 列表刷新

Today 页与 Tasks 页的新增动作，最终都会调用 `ViewModel` 的新增方法，再由仓储层写入内存数据。写入完成后，`FakeTaskRepository` 会更新任务流，列表页订阅到新数据后自动刷新展示。

关键意义在于：

- 页面不需要手动拼接刷新逻辑
- 新建任务后的列表联动由响应式数据流自然完成

## 2. 编辑任务 -> 保存 -> 详情刷新

TaskDetail 页面进入 TaskEdit 页面后，编辑结果通过仓储层更新任务数据。TaskDetail 页重新加载或继续订阅到同一数据源时，可以立即看到最新结果。

关键意义在于：

- 详情页不再依赖手动回传整包数据
- 任务真相（Source of Truth）已经从页面本地变量转移到统一仓储

## 3. 标记完成 -> 状态即时可见

Today、Tasks、TaskDetail 等页面对任务完成状态的修改，都会通过 `setTaskStatus(taskId, isCompleted)` 下沉到仓储层，由统一数据源广播到各页面。

关键意义在于：

- 勾选动作从“局部 UI 切换”升级为“全局业务状态更新”
- 多页面能对同一任务状态保持一致

***

## 五、Stage B 的核心工程价值

结合当前项目代码，可以把 Stage B 的价值概括为以下 5 点：

1. **统一数据真相（Single Source of Truth）**  
   任务、步骤、分类、番茄钟会话不再分散在页面内部，而是统一归仓储层管理。

2. **建立响应式刷新链路（Reactive Data Flow）**  
   页面刷新不再主要依赖命令式更新，而是依赖 `Flow` / `StateFlow` 驱动。

3. **完成 UI 职责收敛（UI Responsibility Reduction）**  
   UI 聚焦展示与交互，状态计算与写操作逻辑下沉到 `ViewModel`。

4. **为 Room 落地做接口预埋（Room Migration Readiness）**  
   Repository 契约已经就位，Stage C 更换数据源时可以保持上层结构稳定。

5. **让多页面联动具备真实可扩展性（Scalability）**  
   新增、编辑、完成、番茄钟、设置等页面已经围绕同一套数据骨架协同工作。

***

## 六、当前实现与计划的差异点

Stage B 主体已经完成，但从“计划验收口径”和“当前代码现状”对照来看，仍有两点需要单独说明。

## 1. B3 差异：删除分类后的任务回填未完成

计划要求：

- 删除分类后，关联任务的 `categoryId` 置为 `0`

当前现状：

- `FakeCategoryRepository` 只删除分类本身
- 尚未联动更新任务数据

影响：

- 某些任务可能仍持有已不存在的分类 ID
- UI 在展示分类名称时会退化为“无分类”或空映射状态，但底层数据并未真正完成回填

## 2. B7 差异：导航规范化并非全链路一致

计划期望：

- 使用 Safe Args 或统一参数协议，确保参数传递类型安全

当前现状：

- Fragment 到 TaskDetail 已使用 Safe Args
- Activity 接收端也使用 `Args.fromBundle(...)`
- 但 TaskDetail 发起到 TaskEdit、Pomodoro 仍是显式 `Intent + putExtra`

影响：

- 参数类型安全已经基本具备
- 但导航风格不完全统一，后续维护时需要明确项目约定

因此，如果以后继续推进 Stage C / Stage D，建议尽早确定以下策略之一：

- 全面切换为以 Navigation（导航组件）为中心的参数跳转
- 明确保留 Activity 显式跳转，但抽出统一的参数协议封装

***

## 七、对 Stage C 的衔接意义

Stage B 完成后，Stage C 的重点已经不再是页面结构调整，而是**数据源替换**。

换句话说，Stage C 的主要工作应聚焦于：

- 引入 Room（本地数据库）
- 定义 Entity（实体）、DAO（数据访问对象）、Database（数据库）
- 用 Room 实现 Repository 接口
- 让现有 ViewModel 与 UI 无缝切换到真实持久化数据源

由于 Stage B 已经完成：

- 领域模型统一
- Repository 接口稳定
- UI 不直接依赖具体实现
- 响应式数据流已形成

所以 Stage C 理论上可以把“改页面”降到最低，把主要精力集中在“替换底层实现”上。

***

## 八、建议的收尾与补强项

如果要把 Stage B 做到更完整、更利于 Stage C 接续，建议优先补齐以下两项：

1. 补齐 `FakeCategoryRepository.deleteCategory()` 对关联任务的 `categoryId = 0` 回填逻辑。
2. 明确 B7 的导航约定，统一 TaskDetail 到 TaskEdit / Pomodoro 的参数传递策略。

可选优化项包括：

- 为 Stage B 主流程补充单元测试（Unit Test）或界面测试（UI Test）
- 为 Settings 与 Pomodoro 之间增加更明确的配置变更说明
- 在文档中固定“Safe Args 与显式 Intent 并存”的工程约定，避免后续重复摇摆

***

## 九、核心技术点与知识点

- `MVVM（Model-View-ViewModel）`
- 仓储模式（Repository Pattern）
- 假仓储（Fake Repository / In-Memory Repository）
- 领域模型（Domain Model）
- Kotlin 协程（Kotlin Coroutines）
- `Flow` / `StateFlow`
- 状态聚合（State Aggregation）
- UI 状态建模（UI State Modeling）
- Jetpack Navigation（导航组件）
- Safe Args（安全参数传递）
- 显式 Intent（Explicit Intent）
- 共享偏好（SharedPreferences）
- 单一数据源（Single Source of Truth）
- 响应式刷新（Reactive Refresh）

***

## 十、一句话总结

Stage B 已经把 MyPotato 从“页面级假数据演示”推进到“具备统一领域模型、统一仓储接口、统一响应式数据流和多页面联动能力的可扩展应用骨架”。当前剩余问题主要集中在一处 FakeRepository 验收细项遗漏，以及一处导航参数规范尚未完全统一，但**不影响 Stage B 作为 Stage C 前置阶段的主体目标成立。**

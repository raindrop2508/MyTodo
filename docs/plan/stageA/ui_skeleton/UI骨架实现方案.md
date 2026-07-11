# UI 骨架实现方案（不修改代码的实施说明）

本方案用于指导你在当前工程基础上，逐步搭建符合《Todo 应用项目大纲（Android）》的 UI 骨架（Skeleton UI），并为后续 Stage B/C/D 的数据与业务闭环预留结构。

## 1. 目标与边界

### 1.1 目标
- 先落地可演示的页面壳：Today、Tasks、Statistics、Settings（底部导航为一级入口）。
- 保持 Material Design 3（Material3）风格一致，结构可扩展到 Task Detail / Task Edit / Pomodoro。
- 明确关键业务规则在 UI 层的呈现方式，避免后续返工。

### 1.2 边界（当前文档不做的事）
- 不修改任何现有代码与资源文件。
- 不引入新的依赖、不落地 Room 实体、DAO、统计查询、番茄钟逻辑。

## 2. 当前工程现状（用于对齐实现方式）

### 2.1 已存在的基础
- 主题：已使用 Material3 DayNight NoActionBar（`Theme.MyPotato`）。
- 入口：单 Activity（`MainActivity`）+ `activity_main.xml`，使用 DataBinding + LiveData。
- AppBar：通过 `MaterialToolbar` 作为 ActionBar。

### 2.2 当前缺失的关键 UI 基建
- 未落地 Navigation Component（Jetpack Navigation）的导航图（navigation graph）。
- 未落地 Fragment 页面承载体（目前只有一个 Activity）。
- 未落地 Bottom Navigation（底部导航）与 menu 资源。

### 2.3 与大纲的对齐点
- 大纲定义了 4 个底部一级页面：Today / Tasks / Statistics / Settings。
- Task Detail / Task Edit / Pomodoro 属于深层页面，适合通过导航栈进入。

## 3. 总体架构建议（UI 层）

### 3.1 推荐结构：单 Activity + 多 Fragment
- `MainActivity` 作为容器：
  - 承载 AppBar（Top App Bar）与 Bottom Navigation。
  - 承载 `NavHostFragment` 作为页面切换容器。
- 各页面用 Fragment 实现：
  - TodayFragment / TasksFragment / StatisticsFragment / SettingsFragment
  - TaskDetailFragment / TaskEditFragment / PomodoroFragment（深层页面）

这样做的好处：
- 与大纲的 Navigation Flow 直接匹配（底部导航 + 深层导航）。
- 后续在 Stage B/C/D 逐步替换数据来源时，UI 结构保持稳定。

### 3.2 ViewBinding / DataBinding 的取舍建议
- 现状已使用 DataBinding（`activity_main.xml`）。
- 新页面建议优先使用 ViewBinding（减少表达式与硬编码文本混入 XML 的风险），必要时再引入 DataBinding。
- 无论使用哪种绑定方式，都应坚持文案来自 string resources（国际化 i18n 规则）。

## 4. 资源与文件规划（建议清单）

### 4.1 资源（res）建议新增项
- `res/navigation/nav_main.xml`：主导航图（含底部 4 页面 + 深层页面）
- `res/menu/menu_bottom_nav.xml`：底部导航菜单
- `res/layout/fragment_today.xml`、`fragment_tasks.xml`、`fragment_statistics.xml`、`fragment_settings.xml`
- `res/layout/fragment_task_detail.xml`、`fragment_task_edit.xml`、`fragment_pomodoro.xml`
- `res/layout/item_task_card.xml`、`item_task_step.xml`（列表项）
- `res/values/strings.xml`：补齐页面标题、按钮、空态文案（同时准备 `values-zh-rCN` / `values-en`）

### 4.2 代码包结构建议（按特性分组）
- `com.gordon.mypotato.ui`
  - `ui.today`：TodayFragment / TodayViewModel / adapters
  - `ui.tasks`：TasksFragment / TasksViewModel / adapters
  - `ui.statistics`：StatisticsFragment / StatisticsViewModel
  - `ui.settings`：SettingsFragment / SettingsViewModel
  - `ui.taskdetail`：TaskDetailFragment / TaskDetailViewModel
  - `ui.taskedit`：TaskEditFragment / TaskEditViewModel
  - `ui.pomodoro`：PomodoroFragment / PomodoroViewModel
- `com.gordon.mypotato.domain`（Stage B 起逐步落地）
  - `model`：Task / TaskStep / Category / PomodoroSession（与大纲模型一致）
  - `repository`：TaskRepository（Fake -> Room）

## 5. 关键业务规则在 UI 的落点（必须先定）

### 5.1 任务类型规则（One-time vs Long-duration）
来源：大纲 2.1 / 4.3 / 4.4。

| 场景 | 长时任务（Long-duration Task） | 单次任务（One-time Task） |
|---|---|---|
| Task Detail 的番茄钟入口 | 显示“开始番茄钟”按钮 | 不可启动番茄钟：入口隐藏或禁用（二选一需定案） |
| Pomodoro 页面可进入性 | 可进入 | 不可进入（若从 DeepLink/错误入口进入需提示并返回） |
| 时间段统计（Statistics 模块 A） | 参与统计 | 不纳入统计 |

推荐做法（降低误解与返工）：
- Task Detail：单次任务显示禁用按钮 + 解释文案（而不是完全隐藏），让用户“知道有此功能但当前不可用”。
- Statistics：任务类型筛选默认“仅长时”，同时在空态时解释“为什么没有数据”。

## 6. 页面骨架规格（按页面列出结构与状态）

### 6.1 Today（底部一级页）
- 顶部区域：
  - 日期 + 问候语
  - 快速筛选：全部 / 单次 / 长时（ChipGroup 或 Segmented Buttons）
- 列表区域：
  - 按四象限分组（可先用标题分组 + RecyclerView 多类型 item）
  - Task 卡片元素：标题、类别标签、紧急/重要标识、状态、是否含步骤
- 关键交互（Stage B 才落地逻辑，Stage A 先占位）：
  - 点击卡片 -> Task Detail（带 taskId）
  - 左滑完成、右滑删除（占位说明 + 后续实现）
  - FAB 新建 -> Task Edit（taskId 为空）
- 状态：
  - 空态：无今日任务（建议提供“新建任务”按钮）
  - 长列表：需要保持滑动性能（后续考虑列表虚拟化/差分）

### 6.2 Tasks（底部一级页）
- 顶部 Tab：
  - 按类别 / 按四象限 / 按状态（TabLayout + ViewPager2 或单页内切换）
- 筛选区：
  - 类别筛选、任务类型筛选、日期范围筛选（Stage A 先画 UI，不做实际筛选）
- 列表区：
  - 列表项支持展开步骤摘要（Stage A 用静态展开/折叠示例）
  - 长按批量操作入口（占位）
- 状态：
  - 空态：无任务（引导新建、导入可选）

### 6.3 Statistics（底部一级页）
- 顶部筛选：
  - 日 / 周 / 月
  - 类别筛选
  - 任务类型筛选（默认仅长时）
- 模块 A：每日时间段分布（仅长时任务/步骤）
  - Stage A：先用占位卡片/假图表容器（不要直接接图表库）
  - 空态文案必须解释“仅统计长时任务/步骤”
- 模块 B：完成情况统计
  - Stage A：占位指标卡（完成率、步骤完成率、四象限完成数）
- 模块 C：类别步骤完成时间
  - Stage A：时间轴列表占位（列表结构优先）

### 6.4 Settings（底部一级页）
- 外观：主题模式切换（跟随系统/浅色/深色）
- 语言：跟随系统/简体中文/English
- 数据管理：导出/导入/清空（危险操作二次确认）
- 关于：版本号、检查更新入口（Stage A 先占位）

### 6.5 Task Detail（深层页）
- 信息区：标题、内容/备注、类别、紧急/重要、时间轴（创建/计划/完成）
- 步骤区：
  - 步骤列表 + 勾选完成 + 查看完成时间（Stage A 用静态列表）
  - 新增步骤按钮（占位）
- 番茄钟入口（关键规则落点）：
  - 长时任务：可点击进入 Pomodoro
  - 单次任务：禁用 + 提示文案

### 6.6 Task Edit（深层页）
- 表单字段：
  - 名称（必填）、内容、备注、类别（必选）、紧急、重要、任务类型、计划开始时间
- 任务类型切换为长时时：
  - 可显示番茄钟默认参数配置区域（Stage A 仅占位，不做保存）
- 保存校验（Stage B 起落地）：
  - 名称必填、类别必选

### 6.7 Pomodoro（深层页）
- 展示：
  - 当前任务标题（可选展示步骤标题）
  - 倒计时（工作/休息）
  - 开始/暂停/停止
- 限制：
  - 仅长时任务可进入；若 taskType 不符则提示并返回

## 7. 导航设计（Navigation Component）

### 7.1 顶层导航（Bottom Navigation）
- Today（startDestination）
- Tasks
- Statistics
- Settings

### 7.2 深层导航
- Task Detail：参数 `taskId: Long`
- Task Edit：参数 `taskId: Long?`（为空表示新建）
- Pomodoro：参数 `taskId: Long`，`stepId: Long?`

### 7.3 页面跳转与回传（Stage B 起实现）
- Task Edit 保存后：
  - 回到 Today / Tasks 触发刷新（通过 SharedViewModel、SavedStateHandle 或结果回传）
- Pomodoro 完成后：
  - 写入会话（Stage D）并回到 Task Detail

## 8. 占位数据与状态管理（Stage A/B 的分界）

### 8.1 Stage A（UI 骨架）
- 所有列表用“假数据”静态展示（写在 ViewModel 的临时数据或 resources 的 JSON 均可，后续替换）
- 所有按钮点击可先 Toast/Snackbar 占位提示（文案必须来自 string resources）

### 8.2 Stage B（假数据优先跑通流程）
- 引入 Fake Repository：
  - Today/Tasks/Detail/Edit 的数据与状态统一从仓库层提供
  - 先跑通创建/编辑/完成三条主流程

## 9. 体验与质量约束（用于 UI 骨架阶段就提前规避问题）
- 触控目标：关键按钮与图标点击区域满足 48dp（Material 建议值）。
- 交互反馈：按钮点击有 state layer/ripple；禁用态有明确视觉与解释。
- 空态策略：空态必须提供“下一步行动”（如新建任务），而不是仅提示无数据。
- 国际化：不允许在 XML/代码中出现面向用户的硬编码字符串。
- 深色模式：所有页面在 Day/Night 下保持可读性与层级清晰。


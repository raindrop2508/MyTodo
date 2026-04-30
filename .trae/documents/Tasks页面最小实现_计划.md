# Today 页面最小实现（UI + 跳转）计划

## 摘要

* 目标：基于 `docs` 设计文档与 `mypotato-focus-flow-main` Today 原型，实现 Android `TodayFragment` 的最小可用 UI 与交互。

* 范围：仅实现静态 UI、写死数据、筛选切换、任务点击跳转、Fab 弹出 BottomSheet；不接入数据库与真实业务写入。

* 用户已确认的关键决策：

  * Fab 新任务采用 `BottomSheet` 弹窗。

  * “长时任务 / 单次任务”采用“单列表 + 类型标签”展示。

  * 任务卡片点击需要跳转到“详情占位页”。

  * UI 文案使用中文。

## 现状分析（基于当前仓库）

* `TodayFragment` 当前仅为占位实现：`Fragment(R.layout.fragment_today)`，无交互逻辑。

* `fragment_today.xml` 当前只有一个居中文本。

* 导航图 `main_nav_graph.xml` 仅包含四个主 Tab（Today/Tasks/Statistics/Settings），不存在任务详情目的地。

* 工程依赖已包含 Material、Navigation、ConstraintLayout、Lifecycle，满足本次最小实现。

* `docs/ui-skeleton-android/page_design.md` 与 `docs/ui-skeleton-android/lovable_ui_logic_file_map.md` 已定义 Today 页的结构与交互方向，可直接映射到 Android XML + Fragment。

## 方案总览

* 页面结构：顶部标题与日期 + 统计卡片 + 优先级筛选（All/紧急且重要/重要/紧急/其他）+ 任务列表 + 空状态 + 右下角 FAB。

* 数据策略：在 `TodayFragment` 内维护写死任务列表（含 type、urgent、important），并由筛选条件实时过滤。

* 展示策略：任务统一单列表渲染，通过标签同时展示“长时/单次”与优先级属性。

* 交互策略：

  * 点击优先级筛选按钮更新列表。

  * 点击任务卡片跳转到 `TaskDetailFragment`（占位）。

  * 点击 FAB 弹出新任务 BottomSheet（占位按钮，不写入数据）。

* 代码约束：后续待接入真实数据的方法，统一补中文注释并添加 `TODO` 标记说明职责。

## 具体改动（文件级）

### 1) 重构 Today 页布局

* 文件：`app/src/main/res/layout/fragment_today.xml`

* 改动：

  * 使用 `ConstraintLayout` + `NestedScrollView`（或等效结构）搭建页面骨架。

  * 新增标题区（Today / 日期）、欢迎统计卡片（完成数/专注时长/连续天数）。

  * 新增优先级筛选区（5 个可切换按钮，默认 All）。

  * 新增任务列表容器（`RecyclerView`）与空状态容器（图标 + 文案）。

  * 新增 `FloatingActionButton`（右下角新增任务）。

* 原因：满足截图与文档中的 Today 页面信息层级与交互入口。

### 2) 新增 Today 任务项布局

* 文件：`app/src/main/res/layout/item_today_task.xml`（新增）

* 改动：

  * 定义任务卡片 UI：标题、描述（可选）、类型标签（长时/单次）、优先级标签（紧急/重要）。

  * 增加基础视觉状态（未完成/已完成样式占位可简化）。

* 原因：将任务列表渲染与页面主布局解耦，便于最小实现与后续替换真实数据。

### 3) 实现 TodayFragment 页面逻辑

* 文件：`app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt`

* 改动：

  * 从仅布局构造升级为 `onViewCreated` 逻辑实现。

  * 初始化写死任务数据与统计数据。

  * 实现优先级筛选状态切换（All / UI / I / U / N）与列表刷新。

  * 绑定 `RecyclerView.Adapter`（可内部类或独立类）渲染任务卡片。

  * 实现任务点击跳转到详情占位页（携带 `taskId`/`title` 参数）。

  * 实现 FAB 点击弹出 BottomSheet（占位表单/占位按钮）。

  * 对后续真实业务方法补中文注释与 `TODO` 标记（如：加载真实任务、保存新任务、提交筛选埋点）。

* 原因：本次核心逻辑入口集中在 Fragment，最小成本满足“UI + 跳转 + 假数据”。

### 4) 新增任务详情占位页（用于跳转闭环）

* 文件：

  * `app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailFragment.kt`（新增）

  * `app/src/main/res/layout/fragment_task_detail.xml`（新增）

* 改动：

  * 新增占位 Fragment，展示传入任务标题/ID。

  * 页面仅提供最小占位文案，标注后续真实详情能力为 `TODO`。

* 原因：用户明确需要“任务卡片点击跳转”，且现有导航图无详情目的地。

### 5) 扩展导航图

* 文件：`app/src/main/res/navigation/main_nav_graph.xml`

* 改动：

  * 新增 `taskDetail` destination。

  * 在 `today` destination 下新增到 `taskDetail` 的 action（含参数定义）。

* 原因：完成 Today -> 详情占位页跳转链路。

### 6) 补充字符串与必要资源

* 文件：

  * `app/src/main/res/values/strings.xml`

  * 视情况新增：`app/src/main/res/drawable/*`（若需要卡片/标签背景）

* 改动：

  * 新增 Today 页面中文文案（筛选标签、空状态、统计项、BottomSheet 标题等）。

  * 如使用自定义圆角标签背景，新增最小 drawable 资源并统一复用。

* 原因：避免硬编码文本，保证 UI 可维护性与后续国际化扩展。

## 方法注释与 TODO 约定（执行时落地）

* 在 `TodayFragment` 与新建 Fragment 中，对“后续需接入真实数据/业务”的方法补充中文 KDoc 注释。

* 注释内容至少包含：方法功能、入参、出参、异常/边界说明。

* 同时在方法体内添加 `TODO` 标记，明确后续接入点，例如：

  * `TODO: 接入 Room/Repository 真实任务数据`

  * `TODO: 接入新建任务表单校验与保存`

  * `TODO: 接入任务详情完整信息查询`

## 假设与决策

* 本次不新增数据库字段、不改动 Room 实体，不引入网络请求。

* 统计数据（完成数/专注分钟/连续天数）允许写死或由写死任务推导，优先保证界面可见与结构正确。

* BottomSheet 本次只做“新任务入口占位”，不落地真实创建逻辑。

* 任务类型展示为标签，不做“按类型分组”与“类型 Tab”。

## 验证步骤（执行阶段）

* 编译检查：`./gradlew :app:assembleDebug`

* 运行检查：

  * 打开 Today 页可看到标题、日期、统计卡片、筛选区、任务列表与 FAB。

  * 切换筛选按钮后，列表与空状态按预期变化。

  * 点击任务卡片可跳转到详情占位页并显示传参信息。

  * 点击 FAB 可弹出 BottomSheet 占位视图。

* 代码质量：

  * 检查新增/修改方法是否包含中文注释与 `TODO` 标记。

  * 检查文案是否全部来自 `strings.xml`，无硬编码中文散落。


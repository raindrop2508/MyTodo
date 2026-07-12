# Todo 应用项目大纲（Android）

## 1. 项目目标
构建一个本地离线优先（Local First）的 Todo 应用，支持任务管理、番茄钟计时（Pomodoro Timer）、数据统计与暗黑模式。  
UI 风格遵循 Material Design 3（Material3）规范，交互参考 Microsoft To Do 的易用性。

## 2. 需求边界与关键规则
### 2.1 任务类型规则
- 任务分为两种：
  - 单次任务（One-time Task）：如买东西、回消息
  - 长时任务（Long-duration Task）：如背单词
- 仅长时任务可启动番茄钟（Pomodoro Timer）。
- 仅长时任务及其步骤，纳入“任务时间段统计”。

### 2.2 步骤任务规则
- 一个任务可选包含多个步骤（Step/Subtask）。
- 步骤属于同一任务类（Category）与同一主任务上下文。
- 统计页面需支持：按类别查看该类任务中每个步骤的完成时间。

### 2.3 数据存储规则
- 所有业务数据默认本地存储（Room）。
- 导入导出功能为可选增强功能，不依赖云端服务。

### 2.4 国际化（i18n）规则
- 提前预留多语言能力，默认支持：
  - 简体中文（zh-rCN）
  - English（en）
- 所有界面文案、错误提示、按钮文本禁止硬编码，统一放入 `string resources`。
- 时间、日期、数字格式按 `Locale` 渲染。
- 图标与图片尽量避免内嵌文字，涉及文字的图片需提供多语言版本。

## 3. 技术栈（含英文名）
- 平台：Android
- 语言：Kotlin（Kotlin）
- 架构：MVVM（Model-View-ViewModel）
- 状态管理：LiveData（LiveData）
- 数据层：Room（Room Persistence Library）
- UI：Material 3（Material3）
- 导航：Navigation Component（Jetpack Navigation）
- 异步：Kotlin Coroutines（协程）
- 国际化：Android Resources（values/values-zh-rCN/values-en）+ AppCompatDelegate Locale API
- 可选图表：
  - MPAndroidChart（成熟度高）
  - Vico（Compose 生态较新，可按后期技术栈评估）

### 3.1 Material3 统一主题规范
- 主题继承链统一：
  - `Theme.MyPotato` -> `Base.Theme.MyPotato`
  - `Base.Theme.MyPotato` -> `Theme.Material3.DayNight.NoActionBar`
- 禁止再引入 `Theme.MaterialComponents.*` 作为应用主主题父类。
- 配色策略：
  - Android 12+ 优先 Dynamic Color（动态配色）
  - Android 10/11 使用静态品牌色回退（Static Fallback）
- 主题职责约束：
  - `values/themes.xml` 与 `values-night/themes.xml` 维护昼夜配色 token
  - `values-v23/themes.xml` 仅维护系统栏相关差异项（如透明状态栏）

## 4. 功能模块设计
### 4.1 任务管理（Task Management）
- 支持任务增删改查（CRUD）。
- 任务字段：
  - 标识：id
  - 基础信息：名称、内容、备注
  - 时间信息：创建时间、计划开始时间、实际完成时间
  - 属性：是否紧急、是否重要、类别、任务类型（单次/长时）
  - 状态：未开始、进行中、已完成、已归档（可选）
  - 累计用时（长时任务有效）
- 四象限分类（Eisenhower Matrix）：
  - 紧急且重要
  - 紧急但不重要
  - 重要但不紧急
  - 不紧急且不重要

### 4.2 步骤任务（Task Steps）
- 支持步骤增删改查、拖拽排序（可选）。
- 每个步骤字段：
  - id、taskId、title、note、sortOrder
  - status（未完成/已完成）
  - completedAt（完成时间）
  - spentDuration（步骤累计时长，可选）
- 步骤完成会写入统计明细，用于类别维度分析。

### 4.3 番茄钟（Pomodoro Timer）
- 仅长时任务显示“开始番茄钟”入口。
- 计时模型：
  - 工作时段（如 25 分钟）
  - 休息时段（如 5 分钟）
  - 可配置轮次（可选）
- 会话数据关联任务与步骤（若在步骤上下文中启动）。

### 4.4 数据统计（Statistics）   扩展功能，可以留到最后
- 时间段统计：展示每天各时段做了什么（仅长时任务/步骤）。
- 完成统计：任务完成率、步骤完成率、四象限完成分布。
- 类别步骤统计：按类别查看步骤完成时间明细和聚合结果。

### 4.5 设置（Settings）
- 检查更新（预留入口，本地可先展示版本信息）。
- 数据导入导出（可选，JSON/CSV）。
- 暗黑模式切换（跟随系统/浅色/深色）。
- 语言切换（跟随系统/简体中文/English）。

## 5. 页面信息架构与详细设计
### 5.1 页面总览
- 启动页（Splash，可选）
- 主页面（Today）
- 任务管理页（Tasks）
- 任务详情页（Task Detail）
- 任务编辑页（Task Edit）
- 类别管理页（Category Manage）
- 统计页（Statistics）
- 设置页（Settings）
- 番茄钟页（Pomodoro）
- 数据导入导出页（Import/Export，可选）

### 5.2 主页面（Today）设计
- 顶部：
  - 今日日期、问候语、快速筛选（全部/单次/长时）
- 中部：
  - 今日任务卡片列表，按四象限分组展示
  - 任务卡片元素：标题、类别标签、优先信息、状态、是否含步骤
- 卡片交互：
  - 点击卡片：进入任务详情页
  - 左滑：完成任务
  - 右滑：删除任务（需二次确认）
  - 更多按钮：编辑、移动类别、归档
- FAB（悬浮按钮）：
  - 新建任务（默认进入任务编辑页）

### 5.3 任务管理页（Tasks）设计
- 顶部 Tab：
  - 按类别
  - 按四象限
  - 按状态
- 筛选区：
  - 类别筛选、任务类型筛选（单次/长时）、日期范围筛选
- 列表区：
  - 支持展开查看步骤摘要
  - 长按进入批量操作（删除、归档、改类别）

### 5.4 任务详情页（Task Detail）设计
- 展示任务完整信息与时间轴。
- 步骤区：
  - 步骤列表、勾选完成、查看每步完成时间
  - 新增步骤按钮
- 番茄钟入口：
  - 仅当任务类型为“长时任务”时可见
  - 单次任务显示不可用提示文案

### 5.5 任务编辑页（Task Edit）设计
- 表单项：
  - 名称、内容、备注、类别、紧急、重要、任务类型
  - 计划开始时间（可选）
- 当任务类型切换为长时时：
  - 展示番茄钟默认参数配置（可选）
- 保存前校验：
  - 名称必填、类别必选

### 5.6 统计页（Statistics）设计
- 顶部筛选：
  - 日/周/月
  - 类别筛选
  - 任务类型筛选（默认仅长时）
- 模块 A：每日时间段分布
  - 目标：看每天时间花在哪里
  - 图表：堆叠柱状图（Stacked Bar Chart）
  - 维度：时间段（如 06-09、09-12）x 类别/任务
- 模块 B：完成情况统计
  - 图表：环形图（Donut/Pie）+ 柱状图（Bar）
  - 指标：任务完成率、步骤完成率、四象限完成数
- 模块 C：类别步骤完成时间
  - 图表：时间轴列表 + 折线图（Line）
  - 指标：某类别下每个步骤完成时间点、平均完成时长

### 5.7 设置页（Settings）设计
- 外观设置：主题模式切换
- 语言设置：跟随系统、简体中文、English
- 数据管理：导出、导入、清空数据（危险操作需确认）
- 关于：版本号、检查更新入口

## 6. 页面跳转逻辑（Navigation Flow）
### 6.1 底部导航（Bottom Navigation）
- 主页面（Today）
- 任务管理页（Tasks）
- 统计页（Statistics）
- 设置页（Settings）

### 6.2 关键跳转
- Today -> Task Detail：点击任务卡片
- Today -> Task Edit：点击 FAB 新建
- Tasks -> Task Detail：点击列表项
- Task Detail -> Task Edit：点击编辑按钮
- Task Detail -> Pomodoro：点击开始番茄钟（仅长时）
- Statistics -> Task Detail：点击图表数据点（可选）
- Settings -> Import/Export：点击数据管理入口

### 6.3 跳转参数建议
- Task Detail：taskId
- Task Edit：taskId（可空，空表示新建）
- Pomodoro：taskId、stepId（可空）
- Statistics Drill-down：categoryId、dateRange

## 7. 数据模型（详细版）
### 7.1 Task
- id: Long
- title: String
- content: String
- note: String
- taskType: Int（0=单次，1=长时）
- status: Int（0=未开始，1=进行中，2=已完成，3=已归档）
- isUrgent: Boolean
- isImportant: Boolean
- categoryId: Long
- createdAt: Long
- plannedStartAt: Long?
- finishedAt: Long?
- totalDurationSec: Long（长时有效）

### 7.2 TaskStep
- id: Long
- taskId: Long
- title: String
- note: String
- sortOrder: Int
- status: Int（0=未完成，1=已完成）
- completedAt: Long?
- spentDurationSec: Long

### 7.3 Category
- id: Long
- name: String
- colorHex: String
- iconName: String（可选）

### 7.4 PomodoroSession
- id: Long
- taskId: Long
- stepId: Long?（可空）
- startedAt: Long
- endedAt: Long?
- focusDurationSec: Long
- breakDurationSec: Long
- cycles: Int
- status: Int（0=进行中，1=完成，2=中断）

### 7.5 StatisticsCache（可选）
- id: Long
- dayKey: String（yyyy-MM-dd）
- metricType: String
- payloadJson: String
- updatedAt: Long

## 8. 数据库与查询设计（Room）
### 8.1 核心关系
- Category 1:N Task
- Task 1:N TaskStep
- Task 1:N PomodoroSession
- TaskStep 1:N PomodoroSession（通过 stepId 可选关联）

### 8.2 关键查询
- 今日任务查询（按四象限排序）
- 按类别 + 时间范围任务查询
- 长时任务时间段聚合查询
- 步骤完成时间查询（按类别过滤）
- 完成率聚合查询（任务/步骤）

## 9. 统计图组件方案
### 9.1 推荐组件
- 首选：MPAndroidChart
  - 优点：稳定、示例多、柱状图/折线图/饼图齐全
  - 场景：时间段柱状图、完成率饼图、趋势折线图
- 备选：Vico
  - 优点：与 Compose 风格更统一
  - 风险：团队熟悉度要求更高

### 9.2 图表与模块映射
- 时间段统计 -> StackedBarChart（堆叠柱）
- 完成占比 -> PieChart/DonutChart（环形图）
- 完成趋势 -> LineChart（折线图）
- 类别步骤完成时刻 -> Timeline List + LineChart

## 10. 分阶段构建方案（可直接按步骤执行）
### Stage A：MVP 体验定义（先 UI 原型）
- 目标：先做可演示最小闭环，不先从技术分层开始。
- 开发内容：
  - 搭建 Today、Tasks 两个核心页面的静态 UI 骨架
  - 优先完成 Material3 主题统一与基础导航结构
  - 图标统一使用 Material Symbols Outlined（先覆盖核心操作）
- 产出物：
  - 可运行页面壳
  - 浅色/深色主题预览图
  - 首版交互走查脚本

### Stage B：核心流程跑通（假数据优先）
- 目标：不依赖数据库，先把任务主流程跑通。
- 开发内容：
  - 使用 Fake Data/Fake Repository 提供任务列表与详情数据
  - 打通创建任务、编辑任务、标记完成三条主流程
  - 页面间参数传递（taskId）与回传刷新机制
- 产出物：
  - 从 Today 到 Task Detail/Task Edit 的完整交互链路
  - 关键状态（未完成/已完成）可视化验证

### Stage C：轻量数据结构落地（再接入 Room）
- 目标：把已验证的交互映射为最小可用数据结构。
- 开发内容：
  - 落地 `Task`、`TaskStep`、`Category`、`PomodoroSession` 的最小字段
  - 建立 Room Entity/DAO/Database
  - 替换 Fake Repository 为本地存储实现
- 产出物：
  - 应用重启后数据持久化有效
  - 与 Stage B 交互行为保持一致

### Stage D：长时任务 + 统计闭环
- 目标：完成长时任务计时与统计最小闭环。
- 开发内容：
  - 仅长时任务显示番茄钟入口并记录会话
  - 完成时间段统计、完成率统计、类别步骤完成时间统计
  - 接入统计图（柱状图/折线图/环形图）
- 产出物：
  - 长时任务可计时并入统计
  - 单次任务不支持番茄钟且不纳入时间段统计

### Stage E：架构补齐 + 质量加固
- 目标：在功能稳定后进行工程化收敛。
- 开发内容：
  - 补齐 Repository/MVVM 分层边界
  - i18n 清理（禁止硬编码文案）
  - 完成主题一致性排查、关键测试与发布前检查
- 产出物：
  - 结构清晰、可维护版本
  - 验收清单可闭环通过

## 11. 验收标准（Definition of Done）
- 单次任务不可启动番茄钟。
- 长时任务可完整执行番茄钟流程并落库会话。
- 统计页可按天查看时间段分布。
- 统计页可按类别查看该类别下步骤完成时间。
- 四象限分类展示与筛选准确。
- 暗黑模式切换即时生效。
- 中英文切换后主要页面文案正确，无硬编码文案。
- 全量数据在离线环境可用。

## 12. 素材清单与建议存储位置
### 12.1 图标素材（Icons）
- 应用图标：前景、背景、自适应图标
- 分类图标：学习、工作、生活、健康、购物等
- 操作图标：新增、编辑、删除、完成、计时、导入、导出
- 存储位置：
  - `app/src/main/res/mipmap-*`（应用图标）
  - `app/src/main/res/drawable`（矢量图标 XML）

### 12.2 图片素材（Images / Illustrations）
- 空状态插图：无任务、无统计数据、无分类
- 引导页插图（可选）
- 存储位置：
  - `app/src/main/res/drawable`（小图）
  - `app/src/main/res/raw`（动画资源，可选）

### 12.3 字体素材（Fonts）
- 可选自定义字体（中英文统一风格）
- 存储位置：
  - `app/src/main/res/font`

### 12.4 动效素材（Animation，可选）
- Lottie 动画（任务完成、番茄钟完成）
- 存储位置：
  - `app/src/main/assets/lottie`

### 12.5 配置素材（Config）
- 默认类别配置（JSON）
- 默认番茄钟参数配置（JSON）
- 存储位置：
  - `app/src/main/assets/config`

### 12.6 导入导出模板（可选）
- JSON 模板、CSV 模板
- 存储位置：
  - `docs/templates`
  - 应用运行时导出目录：`/Documents/MyPotato/exports`（Android 文件系统）

### 12.7 素材命名字段定义（可直接用于重命名）
命名规范建议：`<模块>_<语义>_<状态可选>`，统一小写下划线。
- 图标来源优先级：`Material Symbols Outlined` -> 自绘矢量补充 -> 位图（仅必要场景）。

#### 图标命名（`app/src/main/res/drawable`）
| 素材用途 | 名称字段（资源名） | 说明 |
|---|---|---|
| 应用图标前景 | `ic_launcher_foreground` | 启动图标前景 |
| 应用图标背景 | `ic_launcher_background` | 启动图标背景 |
| 新增任务 | `ic_task_add` | FAB 或工具栏新增 |
| 编辑任务 | `ic_task_edit` | 任务编辑入口 |
| 删除任务 | `ic_task_delete` | 删除操作 |
| 完成任务 | `ic_task_done` | 完成状态 |
| 番茄钟开始 | `ic_pomodoro_start` | 长时任务计时 |
| 番茄钟暂停 | `ic_pomodoro_pause` | 计时暂停 |
| 番茄钟停止 | `ic_pomodoro_stop` | 计时结束 |
| 导入数据 | `ic_data_import` | 导入入口 |
| 导出数据 | `ic_data_export` | 导出入口 |
| 设置 | `ic_nav_settings` | 底部导航设置 |
| 今日 | `ic_nav_today` | 底部导航今日 |
| 任务管理 | `ic_nav_tasks` | 底部导航任务 |
| 统计 | `ic_nav_statistics` | 底部导航统计 |
| 类别-学习 | `ic_category_study` | 学习类别图标 |
| 类别-工作 | `ic_category_work` | 工作类别图标 |
| 类别-生活 | `ic_category_life` | 生活类别图标 |
| 类别-健康 | `ic_category_health` | 健康类别图标 |
| 类别-购物 | `ic_category_shopping` | 购物类别图标 |

#### 图片命名（`app/src/main/res/drawable`）
| 素材用途 | 名称字段（资源名） | 说明 |
|---|---|---|
| 空状态-无今日任务 | `img_empty_today` | Today 页空态 |
| 空状态-无统计数据 | `img_empty_statistics` | Statistics 页空态 |
| 空状态-无分类 | `img_empty_category` | 分类为空 |
| 引导页-第一页 | `img_onboarding_01` | 可选 |
| 引导页-第二页 | `img_onboarding_02` | 可选 |
| 引导页-第三页 | `img_onboarding_03` | 可选 |

#### 动效命名（`app/src/main/assets/lottie`）
| 素材用途 | 名称字段（文件名） | 说明 |
|---|---|---|
| 任务完成动效 | `anim_task_complete.json` | 勾选完成时播放 |
| 番茄钟完成动效 | `anim_pomodoro_finish.json` | 番茄钟轮次完成 |

#### 配置模板命名（`app/src/main/assets/config`）
| 素材用途 | 名称字段（文件名） | 说明 |
|---|---|---|
| 默认类别配置 | `categories_default.json` | 预置类别 |
| 默认番茄钟配置 | `pomodoro_default.json` | 默认时长与轮次 |

#### 导入导出模板命名（`docs/templates`）
| 素材用途 | 名称字段（文件名） | 说明 |
|---|---|---|
| 导入模板 JSON | `todo_import_template.json` | 导入示例结构 |
| 导入模板 CSV | `todo_import_template.csv` | 导入示例结构 |
| 导出示例 JSON | `todo_export_sample.json` | 导出样例 |
| 导出示例 CSV | `todo_export_sample.csv` | 导出样例 |

## 13. 国际化（i18n）预留构建方案
### 13.1 资源目录规划
- `app/src/main/res/values/strings.xml`（默认）
- `app/src/main/res/values-zh-rCN/strings.xml`（简体中文）
- `app/src/main/res/values-en/strings.xml`（English）
- `app/src/main/res/values/plurals.xml`（复数规则）

### 13.2 需国际化的文案域
- 页面标题、按钮文案、空状态文案、错误提示、Toast/Snackbar 文案
- 统计页图表标题、图例、筛选标签
- 设置页主题与语言选项文案

### 13.3 代码约束
- 禁止直接使用硬编码字符串。
- 文案统一通过 `R.string.xxx` 获取。
- 时间与日期统一通过 `DateTimeFormatter` + `Locale` 处理。
- 导入导出文件中的展示字段建议保留可本地化表头映射层。

### 13.4 语言切换行为
- 支持“跟随系统 / 简体中文 / English”。
- 切换语言后重建 Activity 使界面即时生效。
- 语言选择结果持久化到本地配置表或 SharedPreferences。

### 13.5 i18n 验收点
- 中英文切换后，底部导航、任务页、统计页、设置页文案一致正确。
- 图表图例与空状态文案可随语言切换。
- 无明显文案截断与布局错位。
## 14. 风险与决策建议
- 图表库选型建议尽早确定，避免后期重构统计层。
- 若后续切换到 Compose，需评估 LiveData 与 StateFlow 混用策略。
- 番茄钟是否使用前台服务，取决于后台稳定性要求。
## 15. 后续 AI 执行建议
- 按 Phase 顺序逐步实施，不跨阶段并行大改。
- 每阶段先完成数据结构，再实现 UI，再补测试。
- 每阶段输出：变更清单、可运行截图、验收结果。


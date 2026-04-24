# Material3 统一主题与开发流程计划

## 一、Summary（目标摘要）

* 目标 1：将项目现有主题体系统一为 Material 3（Material3），移除 Material Components 旧父主题的混用状态。

* 目标 2：更新 `Todo 应用项目大纲（Android）.md`，将除“主题优化”外的开发流程、图标策略、开发顺序全部整理到文档中。

* 约束偏好：

  * 兼容 Android 10（minSdk 29）

  * 主题配色优先 Dynamic Color（Android 12+），低版本自动回退静态品牌色

  * 开发主线采用“UI 原型 + 核心流程 -> 假数据跑通 -> 轻量数据结构 -> 架构补齐”

  * 执行边界：代码层仅处理主题统一；其他流程性内容只更新文档，不做代码实现

## 二、Current State Analysis（当前状态分析）

* 主题文件现状：

  * `app/src/main/res/values/themes.xml` 中 `Theme.MyPotato` 继承 `Theme.MaterialComponents.DayNight.DarkActionBar`，`Base.Theme.MyPotato` 继承 `Theme.Material3.DayNight.NoActionBar`，存在混用。

  * `app/src/main/res/values-night/themes.xml` 存在相同混用问题。

  * `app/src/main/res/values-v23/themes.xml` 的 `Theme.MyPotato` 继承 `Base.Theme.MyPotato`，用于系统栏透明设置。

* 应用入口：

  * `app/src/main/AndroidManifest.xml` 的 `application/activity` 都使用 `@style/Theme.MyPotato`。

* UI 组件：

  * `app/src/main/res/layout/activity_main.xml` 已使用 Material 组件（`AppBarLayout` / `MaterialToolbar`）。

  * 存在 DataBinding 表达式中的硬编码英文 `"No DB Data"`，后续应迁移至字符串资源（与 i18n 规则一致）。

* 依赖现状：

  * `app/build.gradle` + `gradle/libs.versions.toml` 已引入 `com.google.android.material`。

* 图标资源现状：

  * `app/src/main/res/drawable` 仅有 `ic_launcher_*`，业务图标基本尚未落库，适合一次性建立命名规范与导入策略。

* 文档现状：

  * `Todo 应用项目大纲（Android）.md` 已有 i18n、素材命名与分阶段方案，但尚未明确“Material3 统一迁移清单”与“从 MVP 倒推的执行步骤细化”。

## 三、Proposed Changes（拟议改动）

### 3.1 代码改动：仅做 Material3 主题统一

1. 文件：`app/src/main/res/values/themes.xml`

* What：

  * 将 `Theme.MyPotato` 改为继承 `Base.Theme.MyPotato`（或直接继承 Material3 父主题），不再继承 `Theme.MaterialComponents.DayNight.DarkActionBar`。

  * 在 `Base.Theme.MyPotato` 中统一维护核心 color roles（`colorPrimary`、`colorSecondary`、`colorSurface`、`colorOnSurface` 等）。

* Why：

  * 避免主题继承链冲突，保证 Material3 token 一致生效。

* How：

  * 用 `Base.Theme.MyPotato` 作为唯一基底；

  * `Theme.MyPotato` 仅承载应用级覆盖（如 action bar/no action bar、窗口属性）。

1. 文件：`app/src/main/res/values-night/themes.xml`

* What：

  * 与 `values/themes.xml` 保持同构定义，统一继承链与暗色 token。

* Why：

  * 防止昼夜模式切换时出现组件风格不一致。

* How：

  * 仅保留 Material3 语义化色值映射，不沿用旧 `MaterialComponents` 字段命名语义。

1. 文件：`app/src/main/res/values-v23/themes.xml`

* What：

  * 保留系统栏透明/浅色状态栏控制，但父主题链必须指向统一后的 `Theme.MyPotato` / `Base.Theme.MyPotato`。

* Why：

  * 兼顾 edge-to-edge 与主题一致性。

* How：

  * 只做 API 23+ 差异化窗口属性覆盖，不重复定义主题色。

1. 文件：`app/src/main/java/com/gordon/mypotato/MainActivity.kt`

* What：

* 本轮不改动。

* Why：

* 根据你的要求，除主题优化外，其余流程与策略整理到文档中，不在本次代码范围内扩展。

* How：

* Dynamic Color 具体接入步骤写入大纲文档，作为后续实施任务。

1. 文件：`app/src/main/res/values/colors.xml`（必要时）

* What：

  * 调整/补充静态品牌色，作为 Dynamic Color 的回退方案。

* Why：

  * 动态色仅 Android 12+，低版本需稳定视觉基线。

* How：

  * 保留最少可用色板，不堆积冗余颜色常量。

### 3.2 文档改动：`Todo 应用项目大纲（Android）.md`

1. 新增或增强“Material3 统一规范”小节

* What：

  * 明确主题继承链标准（仅 Material3）。

  * 明确 Dynamic Color 策略（Android 12+ 启用，低版本回退静态色）。

  * 明确图标来源标准（Material Symbols Outlined）。

* Why：

  * 后续 AI/开发者执行时避免二义性。

* How：

  * 放在“技术栈”或“UI 规范”相关章节，附最小约束清单。

1. 强化“素材清单”章节

* What：

  * 为每个功能模块补齐“图标键 -> 业务场景 -> 页面位置”映射。

  * 明确图标来源优先级：Material Symbols Outlined -> 自绘补充。

* Why：

  * 方便你提前重命名和准备素材。

* How：

  * 用表格按模块组织（导航、任务、番茄钟、统计、设置）。

1. 新增“开发流程与开发步骤（MVP 倒推）”章节

* What：

  * 给出不按技术分层起步的执行路径：先关键 UI 与流程，再数据，再架构。

* Why：

  * 符合你指定的推进策略，且更快拿到可演示版本。

* How：

  * 提供阶段目标、输入、输出、完成定义（DoD）。

## 四、Assumptions & Decisions（假设与决策）

* 决策 1：图标风格采用 Material Symbols Outlined。

* 决策 2：主题策略采用 Dynamic Color 优先，Android 10/11 自动回退静态色。

* 决策 3：开发路径采用 MVP 倒推（UI 原型 + 核心流程优先）。

* 决策 4：本轮执行边界为“代码只改主题，流程策略只改文档”。

* 假设 1：当前项目仍以 XML + ViewBinding/DataBinding 为主，不切换到 Compose。

* 假设 2：图标素材可先以矢量 XML 落地，不依赖外部位图资源。

## 五、开发流程与步骤（执行顺序，MVP 倒推）

1. Stage A：定义最小可用体验（1-2 天）

* 目标：确定首个可演示闭环（创建任务 -> 查看列表 -> 标记完成）。

* 先做：

  * Today/Tasks 两页静态 UI 骨架

  * 统一主题与图标基线（Material3 + Outlined）

* 交付物：

  * 可运行壳页面

  * 统一主题生效截图（浅色/深色）

1. Stage B：核心流程 + 假数据（2-3 天）

* 目标：不依赖数据库，先把核心交互跑通。

* 先做：

  * 内存假数据仓库（Fake Repository）

  * 新建任务、编辑任务、完成任务的页面跳转与状态回传

* 交付物：

  * 完整交互链路演示

  * 关键页面状态切换可见

1. Stage C：轻量数据结构落地（2-3 天）

* 目标：把已验证流程映射到最小数据模型。

* 先做：

  * `Task` / `TaskStep` / `Category` / `PomodoroSession` 最小字段集

  * Room 表与基础 DAO

* 交付物：

  * 应用重启后数据可恢复

  * 与 Stage B 交互行为一致

1. Stage D：统计与长时任务能力（3-4 天）

* 目标：接入长时任务计时与统计最小闭环。

* 先做：

  * 长时任务番茄钟入口

  * 时间段统计与完成率基础图表

* 交付物：

  * 长时任务可计时并计入统计

  * 单次任务不展示计时入口

1. Stage E：架构补齐与质量加固（2-4 天）

* 目标：在功能稳定后补齐工程化结构。

* 先做：

  * Repository/MVVM 分层梳理

  * i18n 清理、硬编码排查、主题一致性排查

  * 测试补齐（单元 + 基础 UI）

* 交付物：

  * 结构清晰、可维护版本

  * 发布前验收清单通过

## 六、Verification Steps（验证步骤）

* 主题一致性验证：

  * 浅色/深色模式切换，检查 Toolbar、Button、Text、Surface 颜色是否符合 Material3 token。

  * Android 12+ 检查 Dynamic Color 生效；Android 10/11 检查静态色回退。

* 图标一致性验证：

  * 本轮只验证文档约束是否完整定义图标来源、命名和使用原则。

* 功能顺序验证：

  * `Todo 应用项目大纲（Android）.md` 已明确 Stage A-E 与每阶段交付物。

* 文档一致性验证：

  * `Todo 应用项目大纲（Android）.md` 与“代码只改主题、流程落文档”的边界一致。

* 基础构建验证：

  * Debug 构建通过；

  * 关键页面可打开且无明显崩溃。


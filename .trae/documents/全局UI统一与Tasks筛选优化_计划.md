# 全局 UI 统一与 Tasks 筛选优化计划

## Summary
- 目标：在当前已完成最小实现的基础上，统一 `Today / Tasks / Statistics / Settings` 四个主页面，以及底部导航、任务详情页、新建任务弹窗的视觉风格。
- 用户关注点：
  - 左上角页面标题的字号、语言和层级不一致
  - `Tasks` 页仍存在英文文案，与其他页面语言不一致
  - 各页 FAB 主色不统一
  - 页面背景需要改为暖白色
  - 卡片圆角、描边、填充色、字体层级需统一
  - 少量加入 Emoji，避免页面过于呆板
  - `Tasks` 页筛选区要折叠收纳，并展示当前已选筛选摘要
- 本次范围按用户确认执行为：四个主页面 + 底部导航 + 任务详情页 + 新建任务 BottomSheet。

## Current State Analysis
- `fragment_today.xml`、`fragment_settings.xml`、`fragment_statistics.xml` 的页面主标题大多为 `34sp`，`fragment_tasks.xml` 甚至为 `44sp`；而 `docs/ui-skeleton-android/page_design.md` 规定页面大标题应为 `28sp`。
- `fragment_tasks.xml` 与 `TasksFragment.kt` 仍大量使用英文文案资源（如 `Tasks`、`Search tasks...`、`Pending`、`Completed`），与其他中文页面不一致。
- FAB 颜色不统一：
  - `Today` 使用 `@color/purple_500`
  - `Tasks` 使用 `@color/task_category_work`
- 主题层目前仍以纯白 `colorSurface` 和默认紫色 `#6200EE` 为主：
  - `themes.xml` 中 `colorSurface` 为 `@color/white`
  - `colors.xml` 中主色仍是旧紫色，与文档里建议的紫堇色、用户要求的暖白背景不一致
- 卡片圆角目前混用：
  - `item_today_task.xml` 为 `16dp`
  - `fragment_today.xml` 的统计卡为 `20dp`
  - `fragment_settings.xml` 和 `fragment_statistics.xml` 多处为 `24dp`
  - `bottom_sheet_add_task_placeholder.xml` 输入框圆角为 `16dp`，但标题字号为 `32sp`
- `activity_main.xml` 的 `BottomNavigationView` 仍是系统默认外观，没有与页面背景、文字色、选中态统一。
- `fragment_task_detail.xml` 仍是非常基础的占位结构，虽然标题字号是 `28sp`，但页面背景、卡片化结构、次级文案层级尚未与其他页面统一。
- `fragment_tasks.xml` 当前筛选区由三行横向 `ChipGroup` 常驻显示，占据明显垂直空间；与用户提出的“折叠收纳 + 已选摘要”不一致。

## Proposed Changes

### 1. 统一全局主题与基础视觉资源

#### 1.1 更新 `app/src/main/res/values/colors.xml`
- 将主色调整为更接近设计规范与原型的紫堇色系，而不是当前默认 Android 紫。
- 新增一组用于本次 UI 统一的基础颜色资源：
  - 暖白背景色（页面底色）
  - 暖白卡片底色
  - 更清晰的深色文字 / 辅助文字
  - 统一描边黑色或深灰描边
  - FAB 统一主色
  - 柔和填充色（用于卡片、摘要、选中态）
- 保留任务分类色，但与新主色系协调。

#### 1.2 更新 `app/src/main/res/values/themes.xml` 与 `values-night/themes.xml`
- 让 `colorSurface` / `colorOnSurface` / `colorPrimary` 等主题属性对齐新的统一配色。
- 浅色主题下页面背景改为暖白色而不是纯白。
- 深色主题下保留深色背景，但确保卡片和描边层级统一。

#### 1.3 更新 `app/src/main/res/values/dimens.xml`
- 新增统一尺寸资源，避免多个页面继续硬编码不同圆角和间距：
  - 页面水平边距
  - 页面大标题字号
  - 副标题字号
  - 卡片圆角
  - 按钮圆角
  - FAB 外边距
- 以文档规范为基准：
  - 页面标题 `28sp`
  - 卡片/列表标题 `18sp`
  - 按钮文字 `14sp`
  - 正文辅助 `12sp`
  - 圆角统一 `16dp`

### 2. 统一字符串与文案语言

#### 2.1 更新 `app/src/main/res/values/strings.xml`
- 将 `Tasks` 页相关英文文案全部改为中文，与其他页面统一：
  - 页面标题
  - 搜索提示
  - 状态筛选
  - 类型筛选
  - 优先级筛选
  - 空态文案
  - FAB 文案
- 为少量 Emoji 增加或直接落入资源文案：
  - 页面副标题
  - 空态提示
  - 筛选摘要标题
  - 详情页提示
- Emoji 控制策略按用户偏好执行：
  - 只做少量点缀
  - 优先用于副标题、空态、小节标题
  - 不放入主要数据值、筛选选项主体文本

### 3. 统一四个主页面的标题、背景、卡片和 FAB

#### 3.1 更新 `fragment_today.xml`
- 页面主标题字号收敛到统一标题规格，不再使用当前偏大的视觉。
- 页面背景跟随统一暖白主题。
- 顶部统计卡、Chip 区和空态图标区按统一圆角、描边和填充策略调整。
- FAB 改为统一主色，保证与 `Tasks` 页一致。
- 视需要在副标题或欢迎语中加入少量 Emoji，但不过度影响可读性。

#### 3.2 更新 `fragment_tasks.xml`
- 将主标题字号从 `44sp` 调整到统一规范。
- 页面副标题、总数字段、搜索框、列表区的文字层级统一到文档规范。
- FAB 颜色改为统一主色。
- 筛选区重构为“默认折叠摘要”结构：
  - 默认只展示一个筛选摘要卡片
  - 摘要内显示当前已选状态/类型/优先级
  - 点击“展开/收起”后展示完整筛选区
- 折叠后保留搜索框常驻，避免丢失高频入口。
- 通过更紧凑的筛选布局减少顶部垂直占用。

#### 3.3 更新 `TasksFragment.kt`
- `setupHeader()` 使用中文标题与中文汇总文案。
- 新增筛选摘要文案构建逻辑，实时反映当前已选筛选项。
- 增加展开/收起筛选区的最小状态管理。
- 在不改变现有筛选行为的前提下，仅重构筛选 UI 呈现方式。

#### 3.4 更新 `fragment_statistics.xml`
- 页面标题、副标题字号对齐统一规范。
- KPI 卡片、图表卡片的圆角和描边统一为共享规格。
- 日/月/年切换按钮统一颜色、边框和字体样式。
- 背景改为暖白，图表卡片用稍深一点的暖白/浅填充色与页面背景形成层次。

#### 3.5 更新 `fragment_settings.xml`
- 保持现有结构，但统一标题字号、卡片圆角、按钮外观、描边与填充色。
- 让主题卡、番茄钟卡、数据卡与其他页面卡片保持同一视觉体系。
- 可在副标题或区块标题做少量 Emoji 点缀。

### 4. 统一详情页与 BottomSheet

#### 4.1 更新 `fragment_task_detail.xml`
- 将当前纯文本占位页提升为与四个主页面一致的视觉壳层：
  - 暖白背景
  - 统一标题字号与副标题层级
  - 主要内容卡片化
  - 描边与圆角统一
- 保持当前仍为“最小实现占位”，但视觉不再显得脱节。

#### 4.2 更新 `TaskDetailFragment.kt`
- 仅调整展示文案与内容组织，适配新的卡片化布局和更统一的文案层级。
- 不扩展任务详情业务逻辑。

#### 4.3 更新 `bottom_sheet_add_task_placeholder.xml`
- 底部弹窗标题字号从 `32sp` 下调到统一标题体系。
- 输入框、按钮、Chip、ToggleGroup 的圆角与描边统一。
- 主按钮颜色、描边按钮、分类 Chip 颜色和卡片层级统一到新的主题色。
- 适度加一点 Emoji 或轻量副文案，让弹窗视觉更亲和，但不影响表单效率。

### 5. 统一底部导航

#### 5.1 更新 `activity_main.xml`
- 为根布局与 `BottomNavigationView` 增加主题一致的背景层级。
- 让底部导航与页面背景衔接更自然，不显得像默认系统组件。

#### 5.2 可能需要更新 `MainActivity.kt`
- 若底部导航的选中/未选中文字色、背景或阴影需要代码层设置，则在 `MainActivity` 做最小样式初始化。
- 仅做样式相关处理，不改动导航逻辑。

### 6. 公共列表项与卡片样式统一

#### 6.1 更新 `item_today_task.xml`
- 作为 Today/Tasks 复用的任务卡片，将其作为统一卡片视觉基准：
  - 圆角保持统一
  - 描边颜色调整为统一深色/浅灰黑边框
  - Tag 背景和文字对比更清晰
  - 标题、正文、标签字号统一
- 若需要轻量点缀，可在标签或空态使用少量 Emoji，但不建议放在任务标题本体。

## Assumptions & Decisions
- 本次工作重点是“UI 统一与优化”，不改变页面的核心业务逻辑与数据流。
- 统一范围按用户确认覆盖：四个主页面 + 底部导航 + 任务详情页 + 新建任务 BottomSheet。
- `Tasks` 页筛选区采用“默认折叠摘要”的方案，而不是仅压缩常驻布局。
- 页面整体语言统一为中文；`Tasks` 页原英文资源改为中文。
- Emoji 只做少量点缀，主要用于副标题、空态和提示，不用于主要功能文案和数据标签。
- 浅色主题下页面背景以暖白为主；卡片通过深色边框和柔和填充色拉开层次。
- 所有主要卡片、输入框、按钮圆角统一收敛到 `16dp`，除非个别组件因系统控件限制需要接近值。

## Verification Steps
1. 对比四个主页面，确认：
   - 左上标题字号一致
   - 副标题语言和风格一致
   - 背景暖白统一
2. 检查 `Tasks` 页：
   - 所有文案均为中文
   - 筛选区默认折叠
   - 当前筛选摘要可见
   - 展开后仍能正常筛选
3. 检查 `Today` 与 `Tasks` 页 FAB：
   - 颜色一致
   - 与原型主色更接近
4. 检查 `Statistics`、`Settings`、`TaskDetail`、BottomSheet：
   - 卡片圆角、边框、填充层级一致
   - 标题和区块标题字号一致
5. 检查底部导航：
   - 背景与页面整体协调
   - 选中态与未选中态清晰
6. 编辑完成后检查相关 Kotlin/XML 文件诊断，重点关注：
   - `TasksFragment.kt`
   - `TodayFragment.kt`
   - `StatisticsFragment.kt`
   - `SettingsFragment.kt`
   - `TaskDetailFragment.kt`
   - `activity_main.xml`
   - `fragment_today.xml`
   - `fragment_tasks.xml`
   - `fragment_statistics.xml`
   - `fragment_settings.xml`
   - `fragment_task_detail.xml`
   - `bottom_sheet_add_task_placeholder.xml`
   - `item_today_task.xml`
   - `themes.xml`
   - `colors.xml`
   - `dimens.xml`
   - `strings.xml`

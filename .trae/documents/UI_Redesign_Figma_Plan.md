# 全局 UI 重构计划 (Figma 设计对齐)

基于您提供的四张截图（今天、任务、统计、设置），为了统一风格并与 Figma 设计稿对齐，将进行全面的 UI 优化。本次优化的核心是**大圆角、浅色背景与白色卡片的对比、彩色分类标签、黑白对比强烈的选中状态**。

所有共同的样式和风格将被抽离到 `colors.xml` 和 `themes.xml` / `styles.xml` 中，以便全局复用。

## 1. 全局样式统一定义 (Global Styles & Colors)

### 1.1 颜色定义 (`res/values/colors.xml`)

* **背景色与卡片**:

  * `page_bg_light`: `#FAFAFA` (全局页面背景色，极浅灰)

  * `card_bg_white`: `#FFFFFF` (纯白卡片背景)

* **文本色**:

  * `text_primary`: `#1A1A1A` (主要黑色文本)

  * `text_secondary`: `#757575` (次要灰色文本)

* **品牌与状态色**:

  * `brand_fab_bg`: `#E8EAF6` (浅紫色，用于FAB和底部导航选中背景)

  * `state_success_green`: `#10B981` (完成状态数字、绿色 CheckBox)

  * `state_error_red`: `#EF4444` (红色感叹号)

  * `state_chart_orange`: `#F59E0B` (柱状图颜色)

* **动态标签颜色 (Tag Colors)**:

  * 工作 (Work): 浅紫底 `#F3E8FF`，深紫字 `#7E22CE`

  * 购物 (Shopping): 浅橙底 `#FFEDD5`，深橙字 `#C2410C`

  * 学习 (Study): 浅蓝底 `#DBEAFE`，深蓝字 `#1D4ED8`

  * 健康 (Health): 浅粉底 `#FCE7F3`，深红字 `#BE123C`

  * 通用灰 (Default): 浅灰底 `#F3F4F6`，深灰字 `#4B5563`

### 1.2 尺寸与样式 (`res/values/dimens.xml` & `themes.xml` & `styles.xml`)

* **圆角 (`dimens.xml`)**:

  * `card_corner_radius_large`: `20dp` (主要卡片)

  * `chip_corner_radius`: `24dp` (胶囊形状 Chip)

  * `tag_corner_radius_small`: `8dp` (任务列表内的彩色小标签)

* **全局 Theme (`themes.xml`)**:

  * 更新 `colorSurface` 为 `#FFFFFF`，窗口背景色更新为 `page_bg_light`。

* **自定义 Styles (`styles.xml`)**:

  * **Filter Chip Style**: 创建自定义 Chip 样式。选中状态为黑底白字 (`#1A1A1A`底，`#FFFFFF`字)，未选中为白底黑字带极浅边框。

  * **Dropdown / Setting Item Style**: 统一设置页面中带圆角和灰色背景的下拉框样式。

## 2. 页面级 UI 改造

### 2.1 Today 页面 (`TodayFragment`)

* **头部区域**: 标题 "Today" 靠左对齐，日期紧随其后且左侧增加日历小图标。

* **筛选 Chips**: 移除原有的多排样式，改为单排水平滚动。文本调整为："全部", "紧急且重要", "重要", "紧急", "其他"。应用新的黑白对比 Chip Style。

* **任务列表项 (`item_today_task.xml`)**:

  * 整体改为白色卡片，增大圆角，移除明显边框。

  * 标题在完成时**移除删除线**，仅降低透明度。

  * **标签组优化**: 将类别标签（如“工作”）、类型标签（如“长时”）、时长标签（带时钟图标）、步骤标签水平排列，使用代码动态绑定上面定义的彩色标签。

  * 右侧增加红色感叹号图标和右箭头图标。

* **FAB (悬浮按钮)**: 改为浅紫底黑字/深色字的大圆角/圆形按钮。

### 2.2 Tasks 页面 (`TasksFragment`)

* **头部区域**: 标题 "任务管理" 靠左，右上角增加筛选图标。

* **搜索框**: 调整为胶囊形状（大圆角），内部底色浅灰，无明显边框。

* **分类 Tabs**: 实现 "按类别", "按象限", "按状态" 的文本导航，当前选中项底部带横线指示器。

* **二级筛选 Chips**: 添加水平滚动的胶囊按钮（如 "全部", "学习 1", "工作 2"），带数字徽标，样式同 Today 页面的黑白 Chip。

* **列表展示**: 复用 `item_today_task.xml`，并在代码中支持按类别分组的 Header 显示（如列表项上方显示 "工作 2"）。

### 2.3 Statistics 页面 (`StatisticsFragment`)

* **头部与筛选**: 标题靠左。添加两行水平滚动的 Chips。第一行为时间维度 ("今日", "本周", "本月")，第二行为类别维度 ("全部类别", "仅长时任务")。

* **数据概览卡片**: 将原有的两个卡片改为**三个均等并排的白色卡片**。

  * 已完成：黑色大数字。

  * 进行中：绿色大数字。

  * 完成率：灰色百分比。

* **图表卡片**: 标题改为 "时间段分布" 并带日历图标。柱状图颜色更新为橙色 (`state_chart_orange`)。

### 2.4 Settings 页面 (`SettingsFragment`)

* **整体布局**: 采用大圆角白色卡片分组，背景为浅灰色。

* **外观设置**: "主题模式" 改为带有灰色背景的圆角矩形条目，左侧太阳图标，右侧下拉箭头。

* **语言设置**: "应用语言" 采用相同下拉条目样式。

* **番茄钟设置**: 包含 "工作时长"、"休息时长" (下拉条目样式) 和 "完成提醒" (带 Switch 开关的列表项)。

* **关于**: 包含 "版本号" (右侧显示文字) 和 "检查更新" (带左侧 Info 图标和右侧箭头)。

## 3. 辅助资源 (Resources)

* **图标**: 由开发使用 Android 矢量图标 (Vector Asset) 生成或导入，如 `ic_calendar`, `ic_urgent_alert`, `ic_arrow_right`, `ic_clock`, `ic_dropdown`, `ic_sun`, `ic_info` 等。

* **Shape Drawables**: 创建通用的圆角矩形背景 (`bg_tag_rounded.xml`, `bg_dropdown_item.xml` 等) 用于代码中动态着色。

## 4. 验收标准 (Acceptance Criteria)

1. `colors.xml` 包含所有新定义的设计稿颜色，无硬编码颜色在布局文件中。
2. 今天页面的统计卡片被移除，任务列表项拥有彩色分类标签，且右侧有感叹号和箭头。
3. 任务页面的搜索框变圆润，增加水平分类 Tab 和带数字的 Chip。
4. 统计页面展示三个并排的数据概览卡片（颜色分别为黑、绿、灰）。
5. 设置页面重构为卡片分组样式，下拉选择和 Switch 开关样式统一。
6. 所有选中态的 Chip 都呈现黑底白字，未选中呈现白底黑字。


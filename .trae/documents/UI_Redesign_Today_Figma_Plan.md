# Today页面 Figma UI 重设计计划 (UI Redesign Plan for Today Page)

## 1. 当前状态分析 (Current State Analysis)

当前项目中的 `TodayFragment` 包含一个顶部统计卡片 (`card_summary`)，并且任务列表项 (`item_today_task.xml`) 的标签和视觉样式与提供的 Figma 设计稿不一致。Figma 设计稿呈现了更加极简、清爽的界面风格，移除了顶部的统计卡片，标题与日期居中显示，任务列表项采用了更加丰富的分类标签颜色（如紫色、橙色、蓝色、粉色），并且包含了时间、步骤信息以及右侧的感叹号与箭头图标。

## 2. 改造目标 (Goals)

完全对齐 Figma 设计稿，重构 `TodayFragment` 和 `item_today_task.xml` 的 UI 层，并更新数据绑定逻辑。

## 3. 具体修改方案 (Proposed Changes)

### 3.1 移除统计卡片并调整页面头部 (Remove Summary Card & Adjust Header)

* **文件**: `app/src/main/res/layout/fragment_today.xml`

* **操作**:

  1. 删除 `MaterialCardView` (`@+id/card_summary`) 及其内部所有代码。
  2. 修改 `tv_today_title` 和 `tv_today_date` 的布局约束，使其水平居中对齐 (`app:layout_constraintStart_toStartOf="parent"` 和 `app:layout_constraintEnd_toEndOf="parent"`)。
  3. 在 `tv_today_date` 左侧添加一个日历图标的 Drawable (`android:drawableStart="@drawable/ic_nav_calendar"` 或者新建一个 `ic_calendar_gray`)，并调整 `drawablePadding`。
  4. 更新 `ChipGroup` 的顶部约束，使其直接位于 `tv_today_date` 下方，并居中显示。更新各个 `Chip` 的文本为设计稿中的：“全部”、“紧急且重要”、“重要”、“紧急”、“其他”。

### 3.2 任务列表项 UI 重构 (Refactor Task Item UI)

* **文件**: `app/src/main/res/layout/item_today_task.xml`

* **操作**:

  1. 移除卡片的多余 padding，确保白底与轻微圆角 (`cardCornerRadius` = 12dp 或 16dp) 以及非常浅的边框 (`strokeColor="#F0F0F0"`)。
  2. 修改右侧区域：添加一个红色的感叹号图标 (`ImageView` 红色警告图标) 和一个向右的箭头 (`ImageView` 灰色右箭头)。
  3. 优化标签组 (Tags Layout)：

     * 新增类别标签 `tv_category_tag`（例如“工作”、“购物”、“学习”、“健康”），后续在代码中动态设置背景色与文字颜色。

     * 调整类型标签 `tv_type_tag`（例如“长时”、“单次”）的样式为灰底灰字。

     * 新增时间标签 `tv_time_tag`（例如“60分钟”）和步骤标签 `tv_step_tag`（例如“5个步骤”），带前置小图标。

### 3.3 逻辑绑定与数据源更新 (Update Logic and Mock Data)

* **文件**: `app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt`

* **操作**:

  1. **移除旧逻辑**: 删除 `setupSummaryCard()` 方法及相关的调用。
  2. **扩展数据模型**: 修改 `TodayTask` 数据类，增加 `category` (String, 如 "work", "shopping", "study", "health")，`minutes` (Int)，`steps` (Int) 字段。
  3. **更新 Mock 数据**: 在 `buildMockTasks()` 中添加对应的数据，以匹配 Figma 中的示例（如“完成项目原型设计”、“购买生活用品”等）。
  4. **优化选中状态视觉**: 在 `TodayTaskViewHolder.bind()` 中，当 `item.done` 为 `true` 时，**移除**文字删除线 (Strikethrough) 逻辑，仅降低文字透明度 (`alpha = 0.5f`)。
  5. **动态标签颜色绑定**: 在 `TodayTaskViewHolder` 中增加一个方法 `bindCategoryTag(category: String, textView: TextView)`，根据分类动态设置 `TextView` 的背景和文字颜色（使用 `ColorStateList`）。

### 3.4 颜色与图标资源补充 (Add Color and Icon Resources)

* **文件**: `app/src/main/res/values/colors.xml` & `app/src/main/res/drawable/`

* **操作**:

  1. 添加类别颜色：`tag_work_bg` (浅紫), `tag_work_text` (深紫), `tag_shopping_bg` (浅橙), `tag_shopping_text` (深橙), `tag_study_bg` (浅蓝), `tag_study_text` (深蓝), `tag_health_bg` (浅粉), `tag_health_text` (深粉)。
  2. 添加图标：`ic_calendar_small.xml` (日历)、`ic_urgent_alert.xml` (红色感叹号)、`ic_arrow_right.xml` (右侧箭头)、`ic_time_clock.xml` (时钟)。

## 4. 假设与决策 (Assumptions & Decisions)

* **极简设计**: 遵循 Figma 中展现出的极简 (Minimalism) 前端美学，去除了不必要的统计卡片，通过留白与彩色标签 (Tags) 构建视觉层次。

* **Android 原生组件**: 继续使用 Material3 的 `MaterialCardView`, `ChipGroup`, `MaterialCheckBox` 来实现，以保持代码的健壮性。标签的圆角背景可以使用 `GradientDrawable` 或 `ShapeDrawable` 在代码中动态生成，或者通过 XML 定义多种 shape。

## 5. 验收标准 (Verification Steps)

1. 编译并运行项目。
2. 导航到“Today”页面，确认顶部没有统计卡片，标题和日期居中。
3. 检查任务列表，确认标签具有对应的分类颜色（工作为紫、购物为橙等）。
4. 勾选任务时，确认文字没有删除线，仅颜色变浅。
5. 确认卡片右侧存在红色感叹号及右箭头。

***

> 计划生成完毕，等待用户确认后开始执行。


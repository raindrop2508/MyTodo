# 任务页筛选切换器重构计划

## 1. 现状分析

* 当前在 `fragment_tasks.xml` 中，使用了一个带有下拉图标的 `TextView` (`@+id/tv_filter_menu`) 配合 Kotlin 中的 `PopupMenu` 来实现“按类别”、“按象限”、“按状态”三个过滤维度的切换。

* 用户希望按照设计图中的效果，将下拉菜单替换为平铺的、带有圆角背景的类似 Segmented Control (分段控制器) 的 Tab 样式。

* 该样式特点：外层容器是一个浅灰色的圆角矩形（胶囊状），内部分布三个选项，被选中的选项拥有白色的圆角背景，并且文字颜色较深；未选中的选项背景透明，文字颜色较浅。

## 2. 改造方案

### 2.1 新增必要的 Drawable 和 Color Selector

为了实现纯自定义的分段控制器样式，我们需要新增以下几个资源文件：

* **`res/drawable/bg_segmented_container.xml`**: 外层容器背景，使用 `@color/warm_surface_variant` (浅灰色) 填充，圆角 18dp。

* **`res/drawable/bg_segmented_item_checked.xml`**: 选中项背景，使用 `@color/white` 填充，圆角 16dp。

* **`res/drawable/selector_segmented_button.xml`**: 选中项的背景选择器，`state_checked="true"` 时使用 `bg_segmented_item_checked`，否则使用透明背景。

* **`res/color/selector_segmented_text.xml`**: 文字颜色选择器，`state_checked="true"` 时使用 `@color/text_primary`，否则使用 `@color/text_secondary`。

### 2.2 修改 XML 布局 (`fragment_tasks.xml`)

* 将 `tv_filter_menu` 替换为 `RadioGroup`，横向排列。

* 为 `RadioGroup` 设置背景为 `@drawable/bg_segmented_container`，并增加内边距（如 `padding="2dp"`）以便给选中的白色背景留出边缘空间。

* 在 `RadioGroup` 内部放置三个 `RadioButton`，对应的文本为“按类别”、“按象限”、“按状态”。

* 为 `RadioButton` 设置 `android:button="@null"`，`android:background="@drawable/selector_segmented_button"`，以及对应的文字颜色选择器。

* 将原本位于 `tv_filter_menu` 下方的 `HorizontalScrollView` 的顶部约束更改为对齐到 `RadioGroup` 的底部。

### 2.3 修改 Kotlin 逻辑 (`TasksFragment.kt`)

* 移除原先 `setupFilterMenu()` 中涉及 `PopupMenu` 的逻辑。

* 替换为 `setupDimensionTabs()` 方法，绑定 `RadioGroup` 的 `setOnCheckedChangeListener`。

* 根据选中的 `RadioButton` 的 ID，调用 `switchDimension()` 切换至相应的 `FilterDimension`，并更新下方的 Chip 列表。

## 3. 假设与约束

* 使用 `RadioGroup` 配合背景 Selector 是一种轻量、侵入性小且非常贴合该设计图实现的方法。

* 现有的颜色如 `warm_surface_variant` 和 `white` 已足够实现灰底白块的效果。

## 4. 验证步骤

1. 编译并运行项目，进入 Tasks 页面。
2. 确认原来的“按类别”下拉菜单已经被圆角 Tab 所取代。
3. 点击“按类别”、“按象限”、“按状态”，确认被选中的项目出现白色圆角背景，并且下方的胶囊卡片（ChipGroup）能正确跟随切换。
4. 确认列表过滤逻辑依然正常工作。


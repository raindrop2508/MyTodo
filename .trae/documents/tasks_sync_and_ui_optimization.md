# Tasks页面同步及全局UI优化实施计划

## 1. 现状分析

1. **`getString`** **与** **`findViewById`** **评估**：

   * **`getString`**：在 Fragment 的代码逻辑中（如动态拼接字符串、条件判断返回不同文案），使用 `getString(R.string.xxx)` 是获取资源的标准且正确的方式，ViewBinding 并不替代资源获取功能。因此，现有的 `getString` 使用是合理的，**无需修改**。

   * **`findViewById`**：目前在 `TodayFragment` 和 `TasksFragment` 的 `showAddTaskBottomSheet` 方法中，使用了 `content.findViewById` 来寻找 BottomSheet 弹窗内的控件。因为项目中已开启 ViewBinding，这部分确实可以优化。
2. **全局圆角 CheckBox**：目前复选框使用的是 Material 3 默认的微圆角矩形。用户期望使用“原型（圆形）或直接使用圆角”且保证各页面全局统一。
3. **TasksFragment 逻辑同步**：`TasksFragment` 同样使用了 `item_today_task.xml`，但尚未同步 `TodayFragment` 中新增的勾选事件、状态双向切换以及“已完成任务沉底”的排序逻辑。

## 2. 改造目标

1. 消除 `findViewById`，在 BottomSheet 弹窗中使用 `BottomSheetAddTaskPlaceholderBinding`。
2. 创建全局统一的圆形复选框样式，并在主题中全局应用。
3. 对齐 `TasksFragment` 与 `TodayFragment` 的任务勾选及排序逻辑。

## 3. 具体修改步骤

### 3.1 优化 `findViewById` 为 ViewBinding

* **涉及文件**：`TodayFragment.kt`、`TasksFragment.kt`

* **修改内容**：在 `showAddTaskBottomSheet()` 方法中，移除所有 `findViewById` 的代码。

* **替代方案**：使用 `val sheetBinding = BottomSheetAddTaskPlaceholderBinding.bind(content)`，后续通过 `sheetBinding.tilTaskTitle`、`sheetBinding.etTaskTitle` 等方式直接安全地访问视图。

### 3.2 实现全局圆形 CheckBox

* **新增图标文件**：创建 `res/drawable/ic_check_white.xml`（白色勾选图标）。

* **新增状态选择器**：创建 `res/drawable/sel_checkbox_circle.xml`。使用 `oval`（圆形）作为基础形状：

  * 选中态：填充主色（`?attr/colorPrimary`），内部叠加白色勾选图标。

  * 未选中态：透明填充，带 2dp 的边框（`?attr/colorOutline`）。

* **修改主题配置** (`res/values/themes.xml` 及夜间模式)：

  * 定义样式 `<style name="Widget.MyPotato.CheckBox" parent="Widget.Material3.CompoundButton.CheckBox">`，将其 `android:button` 属性指向 `@drawable/sel_checkbox_circle`。

  * 在 `Base.Theme.MyPotato` 中添加 `<item name="checkboxStyle">@style/Widget.MyPotato.CheckBox</item>`，从而实现全局页面的 CheckBox 样式统一为圆形。

### 3.3 同步 `TasksFragment` 的勾选与排序逻辑

* **涉及文件**：`TasksFragment.kt`

* **修改内容**：

  1. 将 `allTasks` 改为 `MutableList<TaskUiModel>`。
  2. 在 `TasksAdapter` 及 `TasksViewHolder` 中增加 `onCheckChanged: (TaskUiModel, Boolean) -> Unit` 回调。
  3. 在 `ViewHolder.bind` 中，解绑后设置 `binding.cbTaskDone.isChecked = item.done`，然后重新绑定监听器，并保留点击卡片本体的详情跳转逻辑。视觉上对齐 `TodayFragment` 的删除线和透明度逻辑。
  4. 在 `TasksFragment` 新增 `private fun onTaskCheckChanged(task: TaskUiModel, isChecked: Boolean)` 方法，修改数据源对应项的状态，并调用 `renderTasks()` 刷新。
  5. 修改 `renderTasks()` 方法，在过滤结果后追加 `.sortedBy { it.done }`，实现已完成任务沉底。

## 4. 假设与决策

* **设计决策**：根据用户的“原型”描述，推测意为“圆形”。采用 `oval`（圆形）复选框设计，这在 Todo 类应用中非常常见，能提供清晰的“打钩完成”心理暗示。

* **技术决策**：通过覆盖 `checkboxStyle` 主题属性，一劳永逸地解决所有页面的复选框样式问题，符合“保证各个页面中都是圆角”的要求。对于 `getString` 予以保留，因为它是 Android 中获取 String 资源的正确 API。

## 5. 验证步骤

1. 打开应用，进入 Today 页和 Tasks 页，确认所有任务卡片左侧的复选框均变成了统一的圆形样式。
2. 验证 Tasks 页的任务勾选逻辑：点击未完成任务的圆形复选框，任务文字加上删除线并自动沉底；再次点击可恢复。点击卡片空白处正常跳转详情。
3. 点击 FAB 按钮打开“新增任务”弹窗，确认弹窗正常弹出、内部交互（如标题非空校验、选项切换）工作正常，验证 ViewBinding 改造没有破坏原有逻辑。


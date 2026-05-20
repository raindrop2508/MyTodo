# 修改 Today 任务卡片增加勾选及排序功能实施计划

## 1. 现状分析

当前工程中的 `TodayFragment` 已实现了基于 Mock 数据的任务列表展示，列表项布局为 `item_today_task.xml`。
目前任务卡片内部为一个垂直方向的 `LinearLayout`，展示标题、内容和标签。任务数据模型 `TodayTask` 中包含 `done` 字段，但 UI 上没有交互入口，也未对已完成的任务进行沉底排序。

## 2. 改造目标

1. 在任务卡片左侧添加一个符合 Material 风格的勾选框（CheckBox）。
2. 添加勾选逻辑：

   * 勾选后改变任务为“完成”状态。

   * 再次点击已勾选的框，可取消勾选，恢复为“未完成”状态（防误触）。

   * 保留原有的卡片点击事件：点击复选框之外的卡片区域，依然可以进入任务详情。
3. 修改任务列表排序逻辑，确保“已完成”的任务始终排在“未完成”任务的末尾。
4. 保持 UI 骨架的最小实现（仅更新内存中的 Mock 数据，不涉及持久化存储）。

## 3. 具体修改步骤

### 3.1 修改卡片布局 (`item_today_task.xml`)

* 将卡片内部根布局的垂直 `LinearLayout` 改为水平 `LinearLayout`，使其分为左右两部分。

* 左侧添加 `com.google.android.material.checkbox.MaterialCheckBox`。

* 右侧使用一个新的垂直 `LinearLayout` 包裹原有的标题、内容和标签等元素，并设置 `layout_weight="1"` 占满剩余空间。

### 3.2 修改数据集合 (`TodayFragment.kt`)

* 将 `allTasks` 从不可变的 `List<TodayTask>` 修改为可变的 `MutableList<TodayTask>`，以便更新特定任务的状态。

* 修改为：`private val allTasks: MutableList<TodayTask> = buildMockTasks().toMutableList()`

### 3.3 扩展 Adapter 及 ViewHolder (`TodayFragment.kt`)

* 在 `TodayTaskAdapter` 及其 `ViewHolder` 中，新增 `onCheckChanged: (TodayTask, Boolean) -> Unit` 回调。

* 在 `ViewHolder.bind` 方法中：

  * 先移除 CheckBox 的监听器，再根据 `item.done` 设置勾选状态，最后重新挂载监听器，支持完成与未完成状态的自由切换。

  * 为 `binding.root` 保留 `setOnClickListener { onItemClick(item) }`，确保卡片本体的点击仍然跳转详情。

  * （可选但推荐的交互）根据 `item.done` 状态，为任务标题设置删除线并降低透明度，以直观表示任务已完成。恢复未完成时则移除删除线。

### 3.4 添加事件处理与列表排序 (`TodayFragment.kt`)

* 在 `TodayFragment` 中新增 `onTaskCheckChanged(task: TodayTask, isChecked: Boolean)` 方法。

* 当状态改变时，更新 `allTasks` 中对应元素的 `done` 字段（支持双向切换）。

* 重新计算当前的筛选条件（读取 `chipGroupPriority.checkedChipIds`），并调用 `renderTasks`。

* 调用 `setupSummaryCard()` 更新顶部的统计数字。

* 在 `renderTasks` 渲染逻辑中，对过滤后的列表应用 `.sortedBy { it.done }`，由于 `false` 在 `true` 之前，这样能确保未完成排在已完成前面。

## 4. 假设与决策

* **设计决策**：复用 `MaterialCheckBox` 提供原生、符合 Material 3 规范的圆角矩形复选框，满足用户“原型或直接使用圆角”且与整体布局统一的期望。复选框和卡片本体的点击事件相互独立互不干扰。

* **存储假设**：根据 `UI骨架实现方案.md`，目前仍处于 Stage A 阶段，因此修改任务状态仅在内存（`allTasks` 列表）中生效，应用重启后将恢复初始 Mock 数据。

* **视觉反馈**：除了排序后置，增加简单的标题删除线和透明度变化，能让“已完成”状态更清晰。

## 5. 验证步骤

1. 打开 Today 页面，确认每个任务卡片左侧出现勾选框。
2. 勾选任意未完成的任务，确认：

   * 任务文字变为删除线状态。

   * 任务自动沉底，排在所有未完成任务之后。

   * 页面顶部的“已完成”统计数量 +1。
3. 点击已完成任务的勾选框，确认：

   * 任务恢复正常文字显示。

   * 任务从底部恢复到上方未完成区域。

   * 页面顶部的“已完成”统计数量 -1。
4. 点击任务卡片的空白区域，确认正常跳转到任务详情占位页。
5. 切换不同的优先级筛选标签，确认筛选和排序依然正确生效。


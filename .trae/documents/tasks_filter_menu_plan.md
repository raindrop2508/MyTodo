# 任务页筛选菜单重构计划

## 1. 现状分析

* `fragment_tasks.xml` 中使用了 `LinearLayout` 和三个 `TextView` 来模拟“按类别”、“按象限”、“按状态”的 Tab 切换，但并未在 Kotlin 中实现对应的点击事件。

* 目前的 `TasksFragment.kt` 仅支持根据“分类”（categoryFilter）进行列表数据过滤。

* 数据模型 `TaskUiModel` 中包含 `urgent`、`important` 字段可用于象限过滤，以及 `done` 字段可用于状态过滤。

## 2. 改造方案

### 2.1 修改 XML 布局 (`fragment_tasks.xml`)

* **移除旧布局**：删除原有的 `layout_tabs` (LinearLayout) 和下方的 `tab_indicator` (View)。

* **增加菜单触发器**：添加一个 `TextView` (`@+id/tv_filter_menu`)，文本默认为“按类别”，右侧带下拉图标 (`app:drawableEndCompat="@drawable/ic_dropdown"`)，作为弹出菜单的触发按钮。

* **重构 Chip 区域**：

  * 在 `hsv_category_filters` 内部使用 `FrameLayout` 包裹。

  * 保留并修改 `chip_group_category`（默认显示）。

  * 新增 `chip_group_quadrant`（默认隐藏 `visibility="gone"`），包含的 Chip 为：全部、紧急且重要、重要、紧急、其他。

  * 新增 `chip_group_status`（默认隐藏 `visibility="gone"`），包含的 Chip 为：全部、已完成、进行中、未完成。

### 2.2 修改 Kotlin 逻辑 (`TasksFragment.kt`)

* **定义维度枚举**：新增 `FilterDimension` 枚举（CATEGORY, QUADRANT, STATUS）。

* **状态维护**：添加 `currentDimension`（当前维度）、`currentQuadrant`（当前象限选中值）、`currentStatus`（当前状态选中值）等状态变量。

* **实现下拉菜单**：新增 `setupFilterMenu()` 方法，使用 `androidx.appcompat.widget.PopupMenu` 绑定在 `tv_filter_menu` 上。点击不同菜单项时：

  * 更新 `tv_filter_menu` 的文字。

  * 切换 `currentDimension`，并控制对应的 `ChipGroup` 显隐。

  * 触发 `renderTasks()` 重新过滤。

* **绑定所有 ChipGroup**：将原有的 `setupCategoryFilter()` 扩展为 `setupChips()`，分别为三个 `ChipGroup` 添加 `setOnCheckedStateChangeListener` 监听器，并在改变时更新对应的状态变量，随后触发 `renderTasks()`。

* **更新过滤逻辑**：在 `renderTasks()` 中，基于用户选择的“独立过滤”逻辑，仅根据当前激活的 `currentDimension` 及其对应的选中值对 `allTasks` 进行筛选：

  * **按类别**：按原逻辑根据 `task.category` 过滤。

  * **按象限**：根据 `task.urgent` 和 `task.important` 组合过滤。

  * **按状态**：由于当前 `TaskUiModel` 仅有 `done` 字段，故“已完成”对应 `task.done == true`，“进行中”和“未完成”统一对应 `task.done == false`。

## 3. 假设与约束

* “进行中”和“未完成”在目前的 Mock 数据模型下无法明确区分，暂时都映射为 `!task.done`。

* 采用独立过滤逻辑：切换筛选维度时，之前的筛选条件不生效，仅根据当前界面上可见的 Chip 进行过滤。

## 4. 验证步骤

1. 运行应用，进入 Tasks 页面。
2. 点击“按类别”文字，确保能弹出菜单，并且选择“按象限”或“按状态”后，下方 Chip 会正确切换。
3. 切换到“按象限”，点击不同的 Chip，确保列表过滤逻辑正确（基于 urgent/important 字段）。
4. 切换到“按状态”，点击“已完成”、“未完成”等，确保列表数据根据 done 状态正确过滤。


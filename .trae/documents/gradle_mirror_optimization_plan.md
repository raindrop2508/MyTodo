# 任务详情页重构为Activity及番茄钟页面添加计划 (结合 Frontend-Design 细化)

## 1. 摘要

* 删除原有的 `TaskDetailFragment` 及其布局文件，并从 Navigation Component 导航图中移除。

* 新增 `TaskDetailActivity` 及其布局，深度还原截图中的现代化、留白丰富、卡片式 UI 风格，同时确保与 MyPotato App 的整体 Material 3 风格和配色系统完全一致。

* 新增 `PomodoroActivity` 及其占位布局。

* 修改 `TodayFragment` 和 `TasksFragment` 中的任务点击逻辑，使用 `Intent` 携带数据跳转至 `TaskDetailActivity`。

## 2. 当前状态分析

* 目前任务详情页使用 `TaskDetailFragment`，由 Jetpack Navigation 组件进行管理，且界面仅为基础占位文本。

* 原有布局未经过精细化打磨，与现代化的高级 UI 要求存在差距。

## 3. UI 视觉与交互细化 (Frontend-Design 赋能)

在实现布局时，应充分考虑视觉设计的核心原则，避免生硬的拼凑，追求极致的美感与一致性：

### 3.1 空间布局与层次 (Spatial Composition)

* **大留白背景**: 页面整体背景使用全局统一的 `@color/page_bg_light` (#FAFAFA) 或 `warm_background`，营造呼吸感。

* **扁平化卡片**: 任务信息主体卡片使用 `MaterialCardView`，设置大圆角（如 `16dp`），并去除阴影 (`app:cardElevation="0dp"`)，采用极细的边框 (`app:strokeWidth="1dp"`, 颜色 `#EBEBEB` 或 `?attr/colorOutlineVariant`)，使其视觉上既独立又轻盈。

### 3.2 字体与排版 (Typography)

* **主次分明**:

  * 任务标题使用字号大、字重加粗的排版（如 `22sp`, `textStyle="bold"`, 颜色 `@color/text_primary`），与左侧的圆形 CheckBox 垂直居中对齐。

  * 创建时间、累计用时、任务说明等辅助文本，使用稍小字号（如 `14sp`）和 `@color/text_secondary` (#757575) 灰色，降低视觉噪点。

### 3.3 色彩与标签体系 (Color & Theme)

* **状态标签**:

  * **分类标签 (如“工作”)**: 复用现有全局颜色 `@color/tag_work_bg` 和 `@color/tag_work_text`。

  * **类型标签 (如“单次任务”)**: 使用白底 + 细灰边框 (`#E0E0E0`) + 深灰文本 (`#4B5563`)。

  * **紧急标签**: 采用强警示色，如 `@color/state_error_red` (#EF4444) 背景 + 白色文字。

  * **重要标签**: 采用极简对比色，纯黑背景 (`#1A1A1A`) + 白色文字。

* **高光组件 (附带笔记卡片)**:

  * 任务描述下方的“参考Material Design”附带说明，使用带圆角的灰色底块 (`#F5F5F5` 或 `warm_surface_variant`)，搭配深灰色图标与文本，呈现轻微内嵌的视觉感。

### 3.4 交互细节 (Visual Details & Motion)

* **底部核心按钮 (Call to Action)**:

  * **单次任务**: 底部占位提示块采用大圆角、浅灰背景 (`#F5F5F5`) 和灰色文本，呈现明确的不可交互态（Disabled state）。

  * **长时任务**: “开始番茄钟”按钮采用极具视觉冲击力的全黑背景 (`#0A0A0A`) + 白色文字与图标，大圆角（例如 `28dp` 或 `match_parent` 药丸形），作为页面的视觉锚点。

* **任务步骤进度 (Steps Progress)**:

  * 进度条背景轨道使用浅灰 (`#E5E7EB`)，当前进度填充色使用纯黑 (`#1A1A1A`)，去除系统默认的蓝色调，强化中性高级感。

  * 已完成的子步骤文本透明度降低 (`alpha="0.5f"`)，勾选框使用 `@drawable/sel_checkbox_circle` 切换至绿色对号。

## 4. 具体修改步骤

### 4.1 删除旧文件与导航节点

* **删除文件**:

  * `app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailFragment.kt`

  * `app/src/main/res/layout/fragment_task_detail.xml`

* **修改导航图**:

  * `app/src/main/res/navigation/main_nav_graph.xml`: 移除 `taskDetail` 节点及跳转 `action`。

### 4.2 增加依赖的精细化资源文件

* **菜单**: `app/src/main/res/menu/menu_task_detail.xml`（编辑与删除按钮，删除按钮设为警示红）。

* **矢量图标**:

  * `app/src/main/res/drawable/ic_edit.xml`, `ic_delete.xml`, `ic_note.xml`

* **背景样式**:

  * `app/src/main/res/drawable/bg_tag_type.xml`（白色背景加灰色边框）。

  * `app/src/main/res/drawable/bg_tag_urgent.xml` / `bg_tag_important.xml`（圆角实心标签）。

  * `app/src/main/res/drawable/bg_progress_bar_black.xml`（定制化黑灰进度条）。

  * `app/src/main/res/drawable/bg_button_black.xml`（黑色药丸形按钮背景）。

  * `app/src/main/res/drawable/bg_note_card.xml`（内嵌笔记灰色底块）。

### 4.3 新增 TaskDetailActivity 及其布局

* **创建布局**: `app/src/main/res/layout/activity_task_detail.xml`

  * 使用 `MaterialToolbar` 配合透明背景，图标和文字使用深色。

  * 应用上述 3.1 - 3.4 节的设计规范实现卡片、标签、附注区域、以及步骤列表。

  * 长时任务独有卡片包含“任务步骤”列表，使用动态添加 `item_step.xml` 的方式渲染。

* **创建类**: `app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt`

  * 遵循用户规范：使用 `import` 导入，单一文件不超过 1000 行。

  * 编写方法时加入中文注释，包含入参、出参、异常等信息。

  * 根据 `Intent` 传入的数据动态切换单次任务与长时任务的 UI 显示状态。

### 4.4 新增 PomodoroActivity 及其布局

* **创建布局**: `app/src/main/res/layout/activity_pomodoro.xml`（简单的居中文本占位模板）。

* **创建类**: `app/src/main/java/com/gordon/mypotato/ui/pomodoro/PomodoroActivity.kt`。

### 4.5 注册 Activity 与更新跳转逻辑

* **修改清单**: `AndroidManifest.xml` 注册两个新 Activity。

* **修改跳转**: `TodayFragment.kt` 和 `TasksFragment.kt` 的 `onTaskClicked` 改为 `Intent` 跳转，携带详情所需数据。

## 5. 验证步骤

* 编译并运行应用。

* 视觉还原验证：标签颜色、边框粗细、字体大小、黑色番茄钟按钮、进度条样式必须与截图及本计划 3 节中描述的审美一致。

* 交互验证：长时任务点击黑色番茄钟按钮，成功跳转到 `PomodoroActivity` 页面。


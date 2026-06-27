# 新增任务步骤功能实施计划

## 目标
在 TodayFragment 的添加任务 BottomSheet 中新增「任务步骤」模块，支持子任务的添加、删除和排序功能，整体UI风格与原有页面以及 `activity_task_detail.xml` 保持一致。

## 涉及文件

1. **布局文件修改**
   - `app/src/main/res/layout/bottom_sheet_add_task_placeholder.xml`：添加任务步骤模块
   - 新增 `app/src/main/res/layout/item_step_editable.xml`：可编辑的子任务项布局

2. **代码文件修改**
   - `app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt`：实现子任务增删排序逻辑

3. **字符串资源**
   - `app/src/main/res/values/strings.xml`：添加必要的字符串

## 详细实施步骤

### 1. 创建可编辑子任务项布局 `item_step_editable.xml`
- 包含拖拽手柄、输入框、删除按钮
- 参考 TaskDetailActivity 的 item_step.xml，但适配编辑模式

### 2. 修改 BottomSheet 布局 `bottom_sheet_add_task_placeholder.xml`
- 在任务类型选择区域之后添加任务步骤模块
- 使用与 TaskDetailActivity 相同的 MaterialCardView 样式
- 仅在选择「长任务」时显示
- 包含标题、添加按钮和步骤列表 RecyclerView

### 3. 实现 TodayFragment 中的逻辑
- 创建子任务数据类
- 实现 RecyclerView Adapter 用于子任务
- 使用 ItemTouchHelper 实现拖拽排序
- 实现添加和删除子任务功能
- 根据任务类型显示/隐藏步骤模块

### 4. 添加字符串资源
- 添加相关提示文字

## 技术要点
- 使用 Material Design 3 组件
- 支持深色模式（使用 ?attr 属性）
- RecyclerView + ItemTouchHelper 实现拖拽排序
- 仅前端交互，不涉及数据库操作
- UI风格与 activity_task_detail.xml 保持一致

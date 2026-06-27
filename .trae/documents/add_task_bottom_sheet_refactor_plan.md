# 添加任务 BottomSheet 重构计划

## 1. 仓库研究结论

对比了 `TodayFragment.kt` 和 `TasksFragment.kt` 中的 `showAddTaskBottomSheet()` 方法，发现：

### 已实现的功能（TodayFragment）
- ✅ 任务类型切换（单次/长任务）
- ✅ 长任务时显示子任务列表
- ✅ 子任务添加、删除、编辑
- ✅ 子任务拖拽排序（ItemTouchHelper）
- ✅ 任务分类、紧急/重要设置
- ✅ 标题必填校验

### 未实现的功能（TasksFragment）
- ❌ 长任务子任务列表
- ❌ 子任务相关交互

### 重复代码
- 任务类型切换逻辑
- 任务分类选择逻辑
- 紧急/重要开关逻辑
- 标题校验逻辑
- 按钮点击关闭/取消逻辑

## 2. 实现方案

### 2.1 创建公共组件

#### 数据模型
- `EditableStep.kt` - 可编辑步骤数据类（从 TodayFragment 提取）

#### 适配器
- `EditableStepAdapter.kt` - 子任务适配器（从 TodayFragment 提取）

#### BottomSheet 管理类
- `AddTaskBottomSheetHelper.kt` - 封装 BottomSheet 的完整逻辑

### 2.2 文件修改计划

#### 新建文件
1. `app/src/main/java/com/gordon/mypotato/ui/common/EditableStep.kt`
2. `app/src/main/java/com/gordon/mypotato/ui/common/EditableStepAdapter.kt`
3. `app/src/main/java/com/gordon/mypotato/ui/common/AddTaskBottomSheetHelper.kt`

#### 修改文件
1. `app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt` - 使用公共组件
2. `app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt` - 使用公共组件并添加子任务功能

## 3. 实现步骤

### 步骤 1：创建公共数据模型和适配器
- 提取 `EditableStep` 数据类到独立文件
- 提取 `EditableStepAdapter` 到独立文件
- 确保适配器支持拖拽回调

### 步骤 2：创建 AddTaskBottomSheetHelper
- 封装 BottomSheetDialog 创建逻辑
- 封装子任务列表管理（添加、删除、拖拽）
- 封装表单字段监听（类型、分类、紧急/重要）
- 封装表单校验逻辑
- 提供回调接口供外部使用

### 步骤 3：重构 TodayFragment
- 移除原有的 `showAddTaskBottomSheet()` 方法
- 移除内部的 `EditableStep` 和 `EditableStepAdapter`
- 使用 `AddTaskBottomSheetHelper` 替代

### 步骤 4：重构 TasksFragment
- 移除原有的 `showAddTaskBottomSheet()` 方法
- 使用 `AddTaskBottomSheetHelper` 替代
- 自动获得子任务功能

### 步骤 5：测试验证
- 验证 TodayFragment 添加任务功能
- 验证 TasksFragment 添加任务功能
- 验证子任务添加、删除、编辑、拖拽功能
- 验证深色模式适配

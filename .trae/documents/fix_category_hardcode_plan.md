# 分类数据硬编码问题修复计划

## 一、问题分析

### 1.1 核心问题概述

项目中存在多处硬编码分类数据的问题，导致分类数据与 `FakeCategoryRepository` 不一致：

| 问题类型 | 文件 | 具体问题 |
|---------|------|---------|
| 分类数量缺失 | `TasksFragment.kt` | 缺少"购物"分类的过滤Chip |
| 分类数量缺失 | `AddTaskBottomSheetHelper.kt` | 缺少"购物"分类的选择选项 |
| 分类数量缺失 | `TaskEditActivity.kt` | 缺少"购物"分类的处理逻辑 |
| 硬编码ID映射 | `TasksFragment.kt` | `setupChips()` 中硬编码分类ID |
| 硬编码名称映射 | `TodayFragment.kt` | `mapCategoryNameToId()` 使用英文名称而非Repository定义的中文名称 |
| 硬编码名称映射 | `AddTaskBottomSheetHelper.kt` | `setupCategoryChips()` 使用英文名称（"work"）而非Repository的中文名称（"工作"） |
| 硬编码Chip映射 | `TaskEditActivity.kt` | `getSelectedCategoryName()` 和 `resolveCategoryChipId()` 硬编码Chip与分类的映射 |

### 1.2 数据不一致说明

`FakeCategoryRepository` 定义了5个默认分类：
- ID=1: 学习 (#FF6B6B)
- ID=2: 工作 (#4ECDC4)
- ID=3: 生活 (#FFE66D)
- ID=4: 健康 (#95E1D3)
- ID=5: 购物 (#F38181)

但UI层多处只处理了前4个分类，且使用英文名称进行映射，与Repository的中文名称不一致。

## 二、修复方案

### 2.1 修复策略

1. **统一数据源**: 所有分类数据应从 `CategoryRepository` 通过 ViewModel 获取
2. **移除硬编码映射**: 删除所有硬编码的分类ID和名称映射
3. **修复BottomSheet**: 直接传递分类ID而非字符串名称
4. **添加缺失分类**: 在XML布局和相关逻辑中添加"购物"分类

### 2.2 具体修改步骤

#### Step 1: 修改 AddTaskBottomSheetHelper.kt

- 将 `selectedCategory` 类型从 `String` 改为 `Long`
- 修改 `setupCategoryChips()` 方法，直接存储分类ID
- 修改回调接口 `Callback.onTaskCreate()` 的 `category` 参数类型从 `String` 改为 `Long`

#### Step 2: 修改 TodayFragment.kt

- 删除 `mapCategoryNameToId()` 方法
- 修改 `setupFab()` 中的回调，直接使用 `categoryId` 创建任务

#### Step 3: 修改 TasksFragment.kt

- 删除 `mapCategoryNameToId()` 方法
- 修改 `setupFab()` 中的回调，直接使用 `categoryId` 创建任务
- 修改 `setupChips()` 方法，添加"购物"分类的处理

#### Step 4: 修改 TaskEditActivity.kt

- 修改 `getSelectedCategoryName()` 和 `resolveCategoryChipId()` 方法，添加"购物"分类的处理
- 使用 ViewModel 的分类数据进行映射，而非硬编码

#### Step 5: 修改布局文件

- `bottom_sheet_add_task_placeholder.xml`: 添加"购物"分类Chip
- `activity_task_edit.xml`: 添加"购物"分类Chip
- `fragment_tasks.xml`: 添加"购物"分类Chip

## 三、文件修改清单

| 文件 | 修改类型 | 修改内容 |
|------|---------|---------|
| `ui/common/AddTaskBottomSheetHelper.kt` | 修改 | 修改回调参数、selectedCategory类型、Chip映射 |
| `ui/today/TodayFragment.kt` | 修改 | 删除mapCategoryNameToId，修改回调处理 |
| `ui/tasks/TasksFragment.kt` | 修改 | 删除mapCategoryNameToId，修改回调处理，添加购物分类 |
| `ui/tasks/TaskEditActivity.kt` | 修改 | 添加购物分类处理 |
| `res/layout/bottom_sheet_add_task_placeholder.xml` | 修改 | 添加购物分类Chip |
| `res/layout/activity_task_edit.xml` | 修改 | 添加购物分类Chip |
| `res/layout/fragment_tasks.xml` | 修改 | 添加购物分类Chip |

## 四、风险评估

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 回调接口变更可能影响调用方 | 高 | 同步修改所有调用方（TodayFragment、TasksFragment） |
| XML布局修改可能影响样式 | 中 | 保持与现有Chip相同的style属性 |
| 分类ID映射逻辑变更 | 中 | 确保所有地方使用统一的ID映射规则 |

## 五、验证方案

1. **新增任务验证**: 在TodayFragment和TasksFragment中新增任务，选择"购物"分类，验证任务创建成功且分类ID正确
2. **分类过滤验证**: 在TasksFragment中切换分类过滤，验证5个分类都能正常过滤
3. **任务编辑验证**: 在TaskEditActivity中编辑任务，验证能选择和保存"购物"分类
4. **任务详情验证**: 在TaskDetailActivity中查看任务，验证分类名称正确显示

## 六、实施步骤

1. 修改 `AddTaskBottomSheetHelper.kt`
2. 修改 `TodayFragment.kt`
3. 修改 `TasksFragment.kt`
4. 修改 `TaskEditActivity.kt`
5. 修改 `bottom_sheet_add_task_placeholder.xml`
6. 修改 `activity_task_edit.xml`
7. 修改 `fragment_tasks.xml`
8. 编译并运行验证
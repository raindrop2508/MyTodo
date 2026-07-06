# 动态生成分类 Chip 重构计划

## 一、问题分析

当前实现存在的根本问题：

| 问题 | 影响 |
|------|------|
| 分类 Chip 硬编码在 XML 布局中 | 新增/修改分类时需要手动更新布局文件 |
| 分类名称硬编码在 strings.xml 中 | 分类名称与 Repository 数据源不一致 |
| Chip ID 与分类 ID 的映射硬编码在 Kotlin 中 | 数据变化时需要同步修改多处代码 |

**理想状态**：UI 层完全从 `CategoryRepository` 获取分类数据，动态生成 Chip，Repository 变化时 UI 自动更新。

## 二、重构方案

### 2.1 核心思路

1. **XML 布局层**：移除所有硬编码的分类 Chip，只保留空的 `ChipGroup` 容器
2. **Kotlin 代码层**：
   - 创建通用的 `CategoryChipHelper` 工具类，负责动态生成分类 Chip
   - 修改 `AddTaskBottomSheetHelper`、`TaskEditActivity`、`TasksFragment` 使用动态 Chip
3. **资源层**：删除不再需要的分类字符串资源

### 2.2 具体修改步骤

#### Step 1: 创建 CategoryChipHelper 工具类

新建 `ui/common/CategoryChipHelper.kt`，提供：
- `populateCategoryChips()`: 根据分类列表动态填充 ChipGroup
- `populateCategoryFilterChips()`: 根据分类列表动态填充过滤 Chip（含"全部"选项）
- 获取选中分类 ID 的方法

#### Step 2: 修改 XML 布局文件

移除所有硬编码的分类 Chip，只保留空的 ChipGroup：
- `bottom_sheet_add_task_placeholder.xml`
- `activity_task_edit.xml`
- `fragment_tasks.xml`

#### Step 3: 修改 AddTaskBottomSheetHelper.kt

- 添加 `CategoryRepository` 依赖
- 使用 `CategoryChipHelper` 动态生成分类选择 Chip
- 删除硬编码的 Chip ID 映射逻辑

#### Step 4: 修改 TaskEditActivity.kt

- 使用 `CategoryChipHelper` 动态生成分类选择 Chip
- 删除硬编码的 `getSelectedCategoryName()` 和 `resolveCategoryChipId()` 方法
- 通过 ViewModel 的分类数据动态渲染

#### Step 5: 修改 TasksFragment.kt

- 使用 `CategoryChipHelper` 动态生成分类过滤 Chip
- 删除硬编码的 Chip ID 映射逻辑

#### Step 6: 删除冗余字符串资源

从 `strings.xml` 中删除：
- `today_bottom_sheet_category_work/life/study/health/shopping`
- `tasks_filter_chip_study/work/life/health/shopping`

## 三、文件修改清单

| 文件 | 修改类型 | 修改内容 |
|------|---------|---------|
| `ui/common/CategoryChipHelper.kt` | 新增 | 动态生成分类 Chip 的工具类 |
| `ui/common/AddTaskBottomSheetHelper.kt` | 修改 | 使用 CategoryChipHelper 动态生成 Chip |
| `ui/tasks/TaskEditActivity.kt` | 修改 | 使用 CategoryChipHelper 动态生成 Chip |
| `ui/tasks/TasksFragment.kt` | 修改 | 使用 CategoryChipHelper 动态生成过滤 Chip |
| `res/layout/bottom_sheet_add_task_placeholder.xml` | 修改 | 移除硬编码分类 Chip |
| `res/layout/activity_task_edit.xml` | 修改 | 移除硬编码分类 Chip |
| `res/layout/fragment_tasks.xml` | 修改 | 移除硬编码分类 Chip |
| `res/values/strings.xml` | 修改 | 删除冗余分类字符串资源 |

## 四、风险评估

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 动态生成 Chip 可能导致样式不一致 | 中 | 使用统一的 style 属性 |
| ChipGroup 状态管理复杂 | 中 | 使用 `chip.tag` 存储分类 ID |
| 布局文件变化影响现有功能 | 高 | 仔细测试所有分类选择场景 |

## 五、验证方案

1. **分类显示验证**：所有页面的分类选择器应显示5个分类（学习、工作、生活、健康、购物）
2. **动态性验证**：在 Repository 中添加新分类后，UI 应自动显示新分类
3. **功能验证**：新增/编辑/过滤任务时分类选择功能正常
4. **编译验证**：编译通过，无资源引用错误

## 六、实施步骤

1. 创建 `CategoryChipHelper.kt`
2. 修改 `bottom_sheet_add_task_placeholder.xml`
3. 修改 `activity_task_edit.xml`
4. 修改 `fragment_tasks.xml`
5. 修改 `AddTaskBottomSheetHelper.kt`
6. 修改 `TaskEditActivity.kt`
7. 修改 `TasksFragment.kt`
8. 删除 strings.xml 中冗余资源
9. 编译并验证
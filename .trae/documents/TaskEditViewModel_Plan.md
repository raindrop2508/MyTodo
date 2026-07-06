# TodayViewModel 修复计划 v2

## 1. 问题分析

### 问题 1：ANR（主线程死锁）

**原因**：`TodayFragment.onTaskCheckChanged` 使用 `runBlocking` 阻塞主线程，而 `TodayViewModel.toggleTaskStatus` 内部使用 `viewModelScope.launch` 尝试在主线程调度新协程，导致死锁。

**死锁调用链**：

```
主线程 onTaskCheckChanged()
    ↓
runBlocking { }  ← 阻塞主线程等待
    ↓
viewModel.toggleTaskStatus()
    ↓
viewModelScope.launch { }  ← 默认主线程调度，但主线程已阻塞
    ↓
.join()  ← 永远等待 → 死锁 → ANR
```

**修复策略**：

* `toggleTaskStatus` 移除嵌套 `viewModelScope.launch`，直接执行 suspend 逻辑

* Fragment 中所有 `runBlocking` 替换为 `lifecycleScope.launch`

### 问题 2：分类颜色硬编码

**原因**：`Category` 实体已定义 `colorHex` 字段，但 UI 层仍使用硬编码的颜色资源 ID 映射。

**修复策略**：

* 使用 `Category.colorHex` 动态解析颜色

* 根据背景色亮度自动计算文字颜色（保证对比度）

* 移除硬编码的颜色资源 ID 映射逻辑

## 2. 文件修改清单

| 文件                            | 修改内容                               |
| ----------------------------- | ---------------------------------- |
| `viewmodel/TodayViewModel.kt` | 修复 `toggleTaskStatus` 嵌套 launch 问题 |
| `ui/today/TodayFragment.kt`   | 替换 `runBlocking`；移除分类颜色硬编码         |

## 3. 实施步骤

### 步骤 1：修复 TodayViewModel.kt

**文件**：`app/src/main/java/com/gordon/mypotato/viewmodel/TodayViewModel.kt`

修改 `toggleTaskStatus` 方法，移除嵌套的 `viewModelScope.launch`：

```kotlin
// 修改前
suspend fun toggleTaskStatus(taskId: Long) {
    viewModelScope.launch {
        val task = taskRepository.getTaskById(taskId)
        task?.let {
            val newStatus = if (it.isCompleted()) TaskStatus.TODO else TaskStatus.COMPLETED
            taskRepository.updateTaskStatus(taskId, newStatus)
        }
    }.join()
}

// 修改后
suspend fun toggleTaskStatus(taskId: Long) {
    val task = taskRepository.getTaskById(taskId)
    task?.let {
        val newStatus = if (it.isCompleted()) TaskStatus.TODO else TaskStatus.COMPLETED
        taskRepository.updateTaskStatus(taskId, newStatus)
    }
}
```

### 步骤 2：修复 TodayFragment.kt

**文件**：`app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt`

**2.1 添加颜色相关导入**：

```kotlin
import android.graphics.Color
import android.graphics.ColorStateList
```

**2.2 修改** **`onTaskCheckChanged`**：

```kotlin
// 修改前
private fun onTaskCheckChanged(task: Task, isChecked: Boolean) {
    runBlocking {
        viewModel.toggleTaskStatus(task.id)
    }
}

// 修改后
private fun onTaskCheckChanged(task: Task, isChecked: Boolean) {
    lifecycleScope.launch {
        viewModel.toggleTaskStatus(task.id)
    }
}
```

**2.3 修改** **`setupFab`** **中的回调**：

```kotlin
// 修改前
runBlocking {
    // ... 创建 task
    viewModel.addTask(task)
}

// 修改后
lifecycleScope.launch {
    // ... 创建 task
    viewModel.addTask(task)
}
```

**2.4 修改** **`bindCategoryColor`** **方法**：

```kotlin
// 修改前：硬编码颜色映射
private fun bindCategoryColor(category: String, textView: TextView) {
    when (category) {
        "工作" -> { bgRes = R.color.tag_work_bg; textRes = R.color.tag_work_text }
        "购物" -> { bgRes = R.color.tag_shopping_bg; textRes = R.color.tag_shopping_text }
        "学习" -> { bgRes = R.color.tag_study_bg; textRes = R.color.tag_study_text }
        "健康" -> { bgRes = R.color.tag_health_bg; textRes = R.color.tag_health_text }
        else -> { bgRes = R.color.tag_default_bg; textRes = R.color.tag_default_text }
    }
    textView.backgroundTintList = ContextCompat.getColorStateList(context, bgRes)
    textView.setTextColor(ContextCompat.getColor(context, textRes))
}

// 修改后：使用 Category.colorHex
private fun bindCategoryColor(category: Category?, textView: TextView) {
    if (category != null) {
        try {
            val bgColor = Color.parseColor(category.colorHex)
            textView.backgroundTintList = ColorStateList.valueOf(bgColor)
            val luminance = Color.luminance(bgColor)
            val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
            textView.setTextColor(textColor)
        } catch (e: IllegalArgumentException) {
            applyDefaultCategoryColor(textView)
        }
    } else {
        applyDefaultCategoryColor(textView)
    }
}

private fun applyDefaultCategoryColor(textView: TextView) {
    val context = textView.context
    textView.backgroundTintList = ContextCompat.getColorStateList(context, R.color.tag_default_bg)
    textView.setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
}
```

**2.5 修改** **`TodayTaskViewHolder.bind`** **方法**：

```kotlin
// 修改前
val categoryName = categories[item.categoryId]?.name ?: "未分类"
binding.tvCategoryTag.text = categoryName
bindCategoryColor(categoryName, binding.tvCategoryTag)

// 修改后
val category = categories[item.categoryId]
binding.tvCategoryTag.text = category?.name ?: "未分类"
bindCategoryColor(category, binding.tvCategoryTag)
```

### 步骤 3：清理未使用的导入

确保其他方法都使用 lifecycleScope.launch，移除 Fragment 中不再使用的导入：

* `import kotlinx.coroutines.runBlocking`

## 4. 风险与注意事项

1. **颜色格式验证**：`Color.parseColor` 需要有效的 hex 格式（#RRGGBB），添加 try-catch 处理无效输入
2. **颜色对比度**：使用 `Color.luminance` 计算亮度，确保文字与背景有足够对比度
3. **默认颜色降级**：未分类或颜色解析失败时使用默认颜色
4. **协程生命周期**：`lifecycleScope.launch` 会在 Fragment 销毁时自动取消，无需手动管理

## 5. 验证标准

* 勾选任务完成时不再出现 ANR

* 分类标签颜色正确显示（基于 `colorHex`）

* 添加新任务后分类颜色正确显示

* 未分类任务使用默认颜色

* 编译通过，应用正常运行


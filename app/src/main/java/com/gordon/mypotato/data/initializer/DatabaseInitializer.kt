package com.gordon.mypotato.data.initializer

import com.gordon.mypotato.data.dao.CategoryDao
import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.dao.TaskStepDao
import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseInitializer(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val taskStepDao: TaskStepDao
) {

    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            if (categoryDao.getCategoryCount() == 0) {
                initializeDefaultData()
            }
        }
    }

    private suspend fun initializeDefaultData() {
        val now = System.currentTimeMillis() / 1000

        val categoryIds = initializeCategories()

        val taskIds = initializeTasks(now, categoryIds)

        initializeSteps(now, taskIds)
    }

    private suspend fun initializeCategories(): Map<String, Long> {
        val categories = listOf(
            CategoryEntity(
                id = 0,
                name = "学习",
                colorHex = "#FF6B6B",
                iconName = "ic_category_study"
            ),
            CategoryEntity(
                id = 0,
                name = "工作",
                colorHex = "#4ECDC4",
                iconName = "ic_category_work"
            ),
            CategoryEntity(
                id = 0,
                name = "生活",
                colorHex = "#FFE66D",
                iconName = "ic_category_life"
            ),
            CategoryEntity(
                id = 0,
                name = "健康",
                colorHex = "#95E1D3",
                iconName = "ic_category_health"
            ),
            CategoryEntity(
                id = 0,
                name = "购物",
                colorHex = "#F38181",
                iconName = "ic_category_shopping"
            )
        )

        val ids = mutableMapOf<String, Long>()
        categories.forEach { category ->
            val id = categoryDao.insert(category)
            ids[category.name] = id
        }
        return ids
    }

    private suspend fun initializeTasks(now: Long, categoryIds: Map<String, Long>): Map<String, Long> {
        val tasks = listOf(
            TaskEntity(
                id = 0,
                title = "完成项目原型设计",
                content = "同步项目进度和风险事项",
                note = "准备评审议程与需求变更说明",
                taskType = TaskType.LONG.value,
                status = TaskStatus.TODO.value,
                isUrgent = true,
                isImportant = true,
                categoryId = categoryIds["工作"] ?: 0,
                createdAt = now - 86400,
                plannedStartAt = now,
                finishedAt = null,
                totalDurationSec = 0
            ),
            TaskEntity(
                id = 0,
                title = "购买生活用品",
                content = "牙膏、洗发水、毛巾",
                note = null,
                taskType = TaskType.ONCE.value,
                status = TaskStatus.TODO.value,
                isUrgent = false,
                isImportant = true,
                categoryId = categoryIds["购物"] ?: 0,
                createdAt = now - 3600,
                plannedStartAt = null,
                finishedAt = null,
                totalDurationSec = 0
            ),
            TaskEntity(
                id = 0,
                title = "回复客户邮件",
                content = "关于Q3合作方案的确认",
                note = "重点说明交付时间节点",
                taskType = TaskType.ONCE.value,
                status = TaskStatus.COMPLETED.value,
                isUrgent = false,
                isImportant = false,
                categoryId = categoryIds["工作"] ?: 0,
                createdAt = now - 172800,
                plannedStartAt = null,
                finishedAt = now - 86400,
                totalDurationSec = 0
            ),
            TaskEntity(
                id = 0,
                title = "学习 Compose 新特性",
                content = "输入输出模型相关文章",
                note = "重点关注状态管理和重组优化",
                taskType = TaskType.LONG.value,
                status = TaskStatus.IN_PROGRESS.value,
                isUrgent = true,
                isImportant = false,
                categoryId = categoryIds["学习"] ?: 0,
                createdAt = now - 259200,
                plannedStartAt = now - 86400,
                finishedAt = null,
                totalDurationSec = 1800
            ),
            TaskEntity(
                id = 0,
                title = "晚间跑步",
                content = "慢跑5公里",
                note = "记得带运动耳机",
                taskType = TaskType.ONCE.value,
                status = TaskStatus.TODO.value,
                isUrgent = true,
                isImportant = false,
                categoryId = categoryIds["健康"] ?: 0,
                createdAt = now - 7200,
                plannedStartAt = null,
                finishedAt = null,
                totalDurationSec = 0
            ),
            TaskEntity(
                id = 0,
                title = "整理项目文档",
                content = "完善API文档和架构说明",
                note = "参考官方文档规范",
                taskType = TaskType.LONG.value,
                status = TaskStatus.TODO.value,
                isUrgent = false,
                isImportant = true,
                categoryId = categoryIds["工作"] ?: 0,
                createdAt = now - 129600,
                plannedStartAt = now + 86400,
                finishedAt = null,
                totalDurationSec = 0
            )
        )

        val ids = mutableMapOf<String, Long>()
        tasks.forEach { task ->
            val id = taskDao.insert(task)
            ids[task.title] = id
        }
        return ids
    }

    private suspend fun initializeSteps(now: Long, taskIds: Map<String, Long>) {
        val steps = listOf(
            TaskStepEntity(
                id = 0,
                taskId = taskIds["完成项目原型设计"] ?: 0,
                title = "收集需求文档",
                sortOrder = 0,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 3600,
                spentDurationSec = 1800,
                createdAt = now - 7200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["完成项目原型设计"] ?: 0,
                title = "设计页面布局",
                sortOrder = 1,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 7200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["完成项目原型设计"] ?: 0,
                title = "制作交互原型",
                sortOrder = 2,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 7200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["学习 Compose 新特性"] ?: 0,
                title = "阅读官方文档",
                sortOrder = 0,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 86400,
                spentDurationSec = 1200,
                createdAt = now - 259200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["学习 Compose 新特性"] ?: 0,
                title = "编写示例代码",
                sortOrder = 1,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 43200,
                spentDurationSec = 1800,
                createdAt = now - 259200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["学习 Compose 新特性"] ?: 0,
                title = "调试运行效果",
                sortOrder = 2,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["学习 Compose 新特性"] ?: 0,
                title = "总结学习笔记",
                sortOrder = 3,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            ),
            TaskStepEntity(
                id = 0,
                taskId = taskIds["学习 Compose 新特性"] ?: 0,
                title = "分享学习心得",
                sortOrder = 4,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            )
        )

        steps.forEach { step ->
            taskStepDao.insert(step)
        }
    }
}
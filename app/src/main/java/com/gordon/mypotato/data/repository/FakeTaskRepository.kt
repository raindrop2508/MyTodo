package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.SessionStatus
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeTaskRepository : TaskRepository {

    private val tasks = mutableListOf<Task>()
    private val taskSteps = mutableListOf<TaskStep>()
    private val pomodoroSessions = mutableListOf<PomodoroSession>()

    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val stepsFlowMap = mutableMapOf<Long, MutableStateFlow<List<TaskStep>>>()

    private val taskIdGenerator = AtomicLong(1)
    private val stepIdGenerator = AtomicLong(1)
    private val sessionIdGenerator = AtomicLong(1)

    init {
        initDefaultTasks()
    }

    private fun initDefaultTasks() {
        val now = System.currentTimeMillis() / 1000

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "完成项目原型设计",
                content = "同步项目进度和风险事项",
                note = "准备评审议程与需求变更说明",
                taskType = TaskType.LONG.value,
                status = TaskStatus.TODO.value,
                isUrgent = true,
                isImportant = true,
                categoryId = 2L,
                createdAt = now - 86400,
                plannedStartAt = now,
                finishedAt = null,
                totalDurationSec = 0
            )
        )

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "购买生活用品",
                content = "牙膏、洗发水、毛巾",
                note = null,
                taskType = TaskType.ONCE.value,
                status = TaskStatus.TODO.value,
                isUrgent = false,
                isImportant = true,
                categoryId = 5L,
                createdAt = now - 3600,
                plannedStartAt = null,
                finishedAt = null,
                totalDurationSec = 0
            )
        )

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "回复客户邮件",
                content = "关于Q3合作方案的确认",
                note = "重点说明交付时间节点",
                taskType = TaskType.ONCE.value,
                status = TaskStatus.COMPLETED.value,
                isUrgent = false,
                isImportant = false,
                categoryId = 2L,
                createdAt = now - 172800,
                plannedStartAt = null,
                finishedAt = now - 86400,
                totalDurationSec = 0
            )
        )

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "学习 Compose 新特性",
                content = "输入输出模型相关文章",
                note = "重点关注状态管理和重组优化",
                taskType = TaskType.LONG.value,
                status = TaskStatus.IN_PROGRESS.value,
                isUrgent = true,
                isImportant = false,
                categoryId = 1L,
                createdAt = now - 259200,
                plannedStartAt = now - 86400,
                finishedAt = null,
                totalDurationSec = 1800
            )
        )

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "晚间跑步",
                content = "慢跑5公里",
                note = "记得带运动耳机",
                taskType = TaskType.ONCE.value,
                status = TaskStatus.TODO.value,
                isUrgent = true,
                isImportant = false,
                categoryId = 4L,
                createdAt = now - 7200,
                plannedStartAt = null,
                finishedAt = null,
                totalDurationSec = 0
            )
        )

        tasks.add(
            Task(
                id = taskIdGenerator.getAndIncrement(),
                title = "整理项目文档",
                content = "完善API文档和架构说明",
                note = "参考官方文档规范",
                taskType = TaskType.LONG.value,
                status = TaskStatus.TODO.value,
                isUrgent = false,
                isImportant = true,
                categoryId = 2L,
                createdAt = now - 129600,
                plannedStartAt = now + 86400,
                finishedAt = null,
                totalDurationSec = 0
            )
        )

        initDefaultSteps()
        emitTasks()
    }

    private fun initDefaultSteps() {
        val now = System.currentTimeMillis() / 1000

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 1L,
                title = "收集需求文档",
                sortOrder = 0,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 3600,
                spentDurationSec = 1800,
                createdAt = now - 7200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 1L,
                title = "设计页面布局",
                sortOrder = 1,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 7200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 1L,
                title = "制作交互原型",
                sortOrder = 2,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 7200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 4L,
                title = "阅读官方文档",
                sortOrder = 0,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 86400,
                spentDurationSec = 1200,
                createdAt = now - 259200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 4L,
                title = "编写示例代码",
                sortOrder = 1,
                status = StepStatus.COMPLETED.value,
                completedAt = now - 43200,
                spentDurationSec = 1800,
                createdAt = now - 259200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 4L,
                title = "调试运行效果",
                sortOrder = 2,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 4L,
                title = "总结学习笔记",
                sortOrder = 3,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            )
        )

        taskSteps.add(
            TaskStep(
                id = stepIdGenerator.getAndIncrement(),
                taskId = 4L,
                title = "分享学习心得",
                sortOrder = 4,
                status = StepStatus.TODO.value,
                completedAt = null,
                spentDurationSec = 0,
                createdAt = now - 259200
            )
        )
    }

    private fun emitTasks() {
        tasksFlow.value = ArrayList(tasks)
    }

    private fun emitSteps(taskId: Long) {
        stepsFlowMap.getOrPut(taskId) {
            MutableStateFlow(getStepsListByTaskId(taskId))
        }.value = getStepsListByTaskId(taskId)
    }

    private fun getStepsListByTaskId(taskId: Long): List<TaskStep> {
        return taskSteps.filter { it.taskId == taskId }.sortedBy { it.sortOrder }
    }

    /**
     * 获取所有任务的响应式流
     *
     * @return Flow<List<Task>> 任务列表的 StateFlow，数据变更时自动通知订阅者
     */
    override fun getTasks(): Flow<List<Task>> {
        return tasksFlow.asStateFlow()
    }

    /**
     * 根据任务 ID 获取单个任务
     *
     * @param id 任务 ID
     * @return Task? 找到的任务对象，未找到返回 null
     */
    override suspend fun getTaskById(id: Long): Task? {
        delay(100)
        return tasks.find { it.id == id }
    }

    /**
     * 根据组合条件查询任务列表
     *
     * @param query 组合查询条件对象，包含分类、类型、状态、四象限、关键词等可选条件
     * @return Flow<List<Task>> 符合条件的任务列表流
     */
    override fun getTasksByQuery(query: TaskQuery): Flow<List<Task>> {
        return tasksFlow.asStateFlow().map { allTasks ->
            allTasks.filter { task ->
                query.categoryId?.let { task.categoryId == it } ?: true &&
                query.taskType?.let { task.taskType == it.value } ?: true &&
                query.status?.let { task.status == it.value } ?: true &&
                query.isUrgent?.let { task.isUrgent == it } ?: true &&
                query.isImportant?.let { task.isImportant == it } ?: true &&
                query.keyword?.let { keyword ->
                    task.title.contains(keyword, ignoreCase = true) ||
                    task.content?.contains(keyword, ignoreCase = true) == true
                } ?: true
            }.drop(query.offset).take(query.limit)
        }
    }

    /**
     * 根据分类 ID 查询任务列表
     *
     * @param categoryId 分类 ID
     * @return Flow<List<Task>> 指定分类下的任务列表流
     */
    override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> {
        return tasksFlow.asStateFlow().map { it.filter { task -> task.categoryId == categoryId } }
    }

    /**
     * 根据任务类型查询任务列表
     *
     * @param taskType 任务类型枚举（ONCE/LONG）
     * @return Flow<List<Task>> 指定类型的任务列表流
     */
    override fun getTasksByType(taskType: TaskType): Flow<List<Task>> {
        return tasksFlow.asStateFlow().map { it.filter { task -> task.taskType == taskType.value } }
    }

    /**
     * 根据任务状态查询任务列表
     *
     * @param status 任务状态枚举（TODO/IN_PROGRESS/COMPLETED/ARCHIVED）
     * @return Flow<List<Task>> 指定状态的任务列表流
     */
    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return tasksFlow.asStateFlow().map { it.filter { task -> task.status == status.value } }
    }

    /**
     * 根据四象限条件查询任务列表
     *
     * @param isUrgent 是否紧急
     * @param isImportant 是否重要
     * @return Flow<List<Task>> 指定象限的任务列表流
     */
    override fun getTasksByQuadrant(isUrgent: Boolean, isImportant: Boolean): Flow<List<Task>> {
        return tasksFlow.asStateFlow().map {
            it.filter { task -> task.isUrgent == isUrgent && task.isImportant == isImportant }
        }
    }

    /**
     * 根据任务 ID 获取关联的步骤列表
     *
     * @param taskId 任务 ID
     * @return Flow<List<TaskStep>> 指定任务的步骤列表流，按 sortOrder 排序
     */
    override fun getStepsByTaskId(taskId: Long): Flow<List<TaskStep>> {
        return stepsFlowMap.getOrPut(taskId) {
            MutableStateFlow(getStepsListByTaskId(taskId))
        }.asStateFlow()
    }

    /**
     * 添加新任务
     *
     * @param task 任务对象（id 字段会被自动生成覆盖）
     * @return Long 新生成的任务 ID
     */
    override suspend fun addTask(task: Task): Long {
        delay(100)
        val newId = taskIdGenerator.getAndIncrement()
        val newTask = task.copy(id = newId, createdAt = System.currentTimeMillis() / 1000)
        tasks.add(newTask)
        emitTasks()
        return newId
    }

    /**
     * 更新任务信息
     *
     * @param task 更新后的任务对象，根据 id 匹配进行替换
     */
    override suspend fun updateTask(task: Task) {
        delay(100)
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
            emitTasks()
        }
    }

    /**
     * 更新任务状态
     *
     * @param id 任务 ID
     * @param status 新的任务状态枚举
     */
    override suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
        delay(100)
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val now = System.currentTimeMillis() / 1000
            val newFinishedAt = if (status == TaskStatus.COMPLETED) now else null
            tasks[index] = tasks[index].copy(
                status = status.value,
                finishedAt = newFinishedAt
            )
            emitTasks()
        }
    }

    /**
     * 删除任务及关联数据
     *
     * @param id 任务 ID
     */
    override suspend fun deleteTask(id: Long) {
        delay(100)
        tasks.removeAll { it.id == id }
        taskSteps.removeAll { it.taskId == id }
        pomodoroSessions.removeAll { it.taskId == id }
        stepsFlowMap.remove(id)
        emitTasks()
    }

    /**
     * 添加新步骤
     *
     * @param step 步骤对象（id 字段会被自动生成覆盖）
     * @return Long 新生成的步骤 ID
     */
    override suspend fun addStep(step: TaskStep): Long {
        delay(100)
        val newId = stepIdGenerator.getAndIncrement()
        val newStep = step.copy(id = newId, createdAt = System.currentTimeMillis() / 1000)
        taskSteps.add(newStep)
        emitSteps(step.taskId)
        return newId
    }

    /**
     * 更新步骤信息
     *
     * @param step 更新后的步骤对象，根据 id 匹配进行替换
     */
    override suspend fun updateStep(step: TaskStep) {
        delay(100)
        val index = taskSteps.indexOfFirst { it.id == step.id }
        if (index != -1) {
            taskSteps[index] = step
            emitSteps(step.taskId)
        }
    }

    /**
     * 更新步骤状态
     *
     * @param id 步骤 ID
     * @param status 新的步骤状态枚举
     */
    override suspend fun updateStepStatus(id: Long, status: StepStatus) {
        delay(100)
        val index = taskSteps.indexOfFirst { it.id == id }
        if (index != -1) {
            val now = System.currentTimeMillis() / 1000
            val newCompletedAt = if (status == StepStatus.COMPLETED) now else null
            taskSteps[index] = taskSteps[index].copy(
                status = status.value,
                completedAt = newCompletedAt
            )
            emitSteps(taskSteps[index].taskId)
        }
    }

    /**
     * 删除步骤
     *
     * @param id 步骤 ID
     */
    override suspend fun deleteStep(id: Long) {
        delay(100)
        val step = taskSteps.find { it.id == id }
        if (step != null) {
            taskSteps.remove(step)
            emitSteps(step.taskId)
        }
    }
}
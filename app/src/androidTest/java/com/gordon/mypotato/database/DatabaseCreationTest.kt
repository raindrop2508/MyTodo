package com.gordon.mypotato.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gordon.mypotato.data.AppDatabase
import com.gordon.mypotato.data.dao.CategoryDao
import com.gordon.mypotato.data.dao.PomodoroSessionDao
import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.dao.TaskStepDao
import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity
import com.gordon.mypotato.domain.StepStatus
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class DatabaseCreationTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskStepDao: TaskStepDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var pomodoroSessionDao: PomodoroSessionDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        taskDao = db.taskDao()
        taskStepDao = db.taskStepDao()
        categoryDao = db.categoryDao()
        pomodoroSessionDao = db.pomodoroSessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDatabaseCreated() {
        assertNotNull(db)
        assertNotNull(taskDao)
        assertNotNull(taskStepDao)
        assertNotNull(categoryDao)
        assertNotNull(pomodoroSessionDao)
    }

    @Test
    fun testCategoryTableCRUD() = runBlocking {
        val category = CategoryEntity(
            id = 0,
            name = "测试分类",
            colorHex = "#FF0000",
            iconName = "ic_test"
        )

        val insertId = categoryDao.insert(category)
        assertEquals(1, insertId)

        val retrieved = categoryDao.getCategoryById(insertId)
        assertNotNull(retrieved)
        assertEquals("测试分类", retrieved?.name)
        assertEquals("#FF0000", retrieved?.colorHex)

        val updated = retrieved?.copy(name = "更新分类")
        updated?.let { categoryDao.update(it) }

        val afterUpdate = categoryDao.getCategoryById(insertId)
        assertEquals("更新分类", afterUpdate?.name)

        categoryDao.deleteById(insertId)
        val afterDelete = categoryDao.getCategoryById(insertId)
        assertNull(afterDelete)
    }

    @Test
    fun testTaskTableCRUD() = runBlocking {
        val task = TaskEntity(
            id = 0,
            title = "测试任务",
            content = "任务内容",
            note = "备注",
            taskType = TaskType.LONG.value,
            status = TaskStatus.TODO.value,
            isUrgent = true,
            isImportant = false,
            categoryId = 0,
            createdAt = System.currentTimeMillis() / 1000,
            plannedStartAt = null,
            finishedAt = null,
            totalDurationSec = 0
        )

        val insertId = taskDao.insert(task)
        assertEquals(1, insertId)

        val retrieved = taskDao.getTaskById(insertId)
        assertNotNull(retrieved)
        assertEquals("测试任务", retrieved?.title)

        taskDao.updateStatus(insertId, TaskStatus.COMPLETED.value, System.currentTimeMillis() / 1000, 60)
        val afterStatusUpdate = taskDao.getTaskById(insertId)
        assertEquals(TaskStatus.COMPLETED.value, afterStatusUpdate?.status)

        taskDao.deleteById(insertId)
        val afterDelete = taskDao.getTaskById(insertId)
        assertNull(afterDelete)
    }

    @Test
    fun testTaskStepTableCRUD() = runBlocking {
        val task = TaskEntity(
            id = 0,
            title = "父任务",
            content = null,
            note = null,
            taskType = TaskType.LONG.value,
            status = TaskStatus.TODO.value,
            isUrgent = false,
            isImportant = false,
            categoryId = 0,
            createdAt = System.currentTimeMillis() / 1000,
            plannedStartAt = null,
            finishedAt = null,
            totalDurationSec = 0
        )
        val taskId = taskDao.insert(task)

        val step = TaskStepEntity(
            id = 0,
            taskId = taskId,
            title = "测试步骤",
            sortOrder = 0,
            status = StepStatus.TODO.value,
            completedAt = null,
            spentDurationSec = 0,
            createdAt = System.currentTimeMillis() / 1000
        )

        val insertId = taskStepDao.insert(step)
        assertEquals(1, insertId)

        val retrieved = taskStepDao.getStepById(insertId)
        assertNotNull(retrieved)
        assertEquals("测试步骤", retrieved?.title)

        taskStepDao.updateStatus(insertId, StepStatus.COMPLETED.value, System.currentTimeMillis() / 1000, 30)
        val afterStatusUpdate = taskStepDao.getStepById(insertId)
        assertEquals(StepStatus.COMPLETED.value, afterStatusUpdate?.status)

        taskStepDao.deleteById(insertId)
        val afterDelete = taskStepDao.getStepById(insertId)
        assertNull(afterDelete)
    }

    @Test
    fun testPomodoroSessionTableCRUD() = runBlocking {
        val task = TaskEntity(
            id = 0,
            title = "番茄任务",
            content = null,
            note = null,
            taskType = TaskType.LONG.value,
            status = TaskStatus.IN_PROGRESS.value,
            isUrgent = false,
            isImportant = false,
            categoryId = 0,
            createdAt = System.currentTimeMillis() / 1000,
            plannedStartAt = null,
            finishedAt = null,
            totalDurationSec = 0
        )
        val taskId = taskDao.insert(task)

        val session = PomodoroSessionEntity(
            id = 0,
            taskId = taskId,
            stepId = null,
            startedAt = System.currentTimeMillis() / 1000,
            endedAt = null,
            focusDurationSec = 1500,
            breakDurationSec = 300,
            cycles = 1,
            status = 0,
            phase = 0,
            plannedDurationMs = 25 * 60 * 1000L,
            targetEndEpochMs = System.currentTimeMillis() + 25 * 60 * 1000L,
            remainingMsWhenPaused = 0L,
            pauseStartedAtEpochMs = 0L
        )

        val insertId = pomodoroSessionDao.insert(session)
        assertEquals(1, insertId)

        val retrieved = pomodoroSessionDao.getSessionById(insertId)
        assertNotNull(retrieved)
        assertEquals(taskId, retrieved?.taskId)

        pomodoroSessionDao.deleteById(insertId)
        val afterDelete = pomodoroSessionDao.getSessionById(insertId)
        assertNull(afterDelete)
    }

    @Test
    fun testForeignKeyCascadeTaskStep() = runBlocking {
        val task = TaskEntity(
            id = 0,
            title = "级联测试任务",
            content = null,
            note = null,
            taskType = TaskType.LONG.value,
            status = TaskStatus.TODO.value,
            isUrgent = false,
            isImportant = false,
            categoryId = 0,
            createdAt = System.currentTimeMillis() / 1000,
            plannedStartAt = null,
            finishedAt = null,
            totalDurationSec = 0
        )
        val taskId = taskDao.insert(task)

        val step = TaskStepEntity(
            id = 0,
            taskId = taskId,
            title = "级联测试步骤",
            sortOrder = 0,
            status = StepStatus.TODO.value,
            completedAt = null,
            spentDurationSec = 0,
            createdAt = System.currentTimeMillis() / 1000
        )
        taskStepDao.insert(step)

        val stepsBeforeDelete = taskStepDao.getStepsByTaskId(taskId).run { this }
        assertEquals(1, stepsBeforeDelete.first().size)

        taskDao.deleteById(taskId)

        val stepsAfterDelete = taskStepDao.getStepsByTaskId(taskId).run { this }
        assertEquals(0, stepsAfterDelete.first().size)
    }
}
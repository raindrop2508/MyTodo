package com.gordon.mypotato.data.backup

import com.gordon.mypotato.data.dao.CategoryDao
import com.gordon.mypotato.data.dao.PomodoroSessionDao
import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.dao.TaskStepDao
import com.gordon.mypotato.data.initializer.DatabaseInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomBackupRepository(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val taskStepDao: TaskStepDao,
    private val pomodoroSessionDao: PomodoroSessionDao,
) {

    suspend fun exportSnapshot(): BackupSnapshot = withContext(Dispatchers.IO) {
        BackupSnapshot(
            categories = categoryDao.getAllOnce(),
            tasks = taskDao.getAllOnce(),
            steps = taskStepDao.getAllOnce(),
            sessions = pomodoroSessionDao.getAllOnce(),
        )
    }

    suspend fun exportJson(): String = BackupJsonCodec.encode(exportSnapshot())

    suspend fun importJson(json: String) = withContext(Dispatchers.IO) {
        val snapshot = BackupJsonCodec.decode(json)
        clearAllTables()
        snapshot.categories.forEach { categoryDao.insert(it) }
        snapshot.tasks.forEach { taskDao.insert(it) }
        snapshot.steps.forEach { taskStepDao.insert(it) }
        snapshot.sessions.forEach { pomodoroSessionDao.insert(it) }
    }

    suspend fun clearAllAndReseed() = withContext(Dispatchers.IO) {
        clearAllTables()
        DatabaseInitializer(categoryDao, taskDao, taskStepDao).initializeIfNeeded()
    }

    private suspend fun clearAllTables() {
        pomodoroSessionDao.deleteAll()
        taskStepDao.deleteAll()
        taskDao.deleteAll()
        categoryDao.deleteAll()
    }
}

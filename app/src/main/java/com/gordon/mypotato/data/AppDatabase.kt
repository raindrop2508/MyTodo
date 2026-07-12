package com.gordon.mypotato.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gordon.mypotato.data.dao.AppInfoDao
import com.gordon.mypotato.data.dao.CategoryDao
import com.gordon.mypotato.data.dao.PomodoroSessionDao
import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.dao.TaskStepDao
import com.gordon.mypotato.data.entity.AppInfo
import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity

/**
* 注解关联数据实体 entity
 *
* */
@Database(
    entities = [
        AppInfo::class,
        TaskEntity::class,
        TaskStepEntity::class,
        CategoryEntity::class,
        PomodoroSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // 提供dao访问接口
    abstract fun appInfoDao(): AppInfoDao
    abstract fun taskDao(): TaskDao
    abstract fun taskStepDao(): TaskStepDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    /**
    * 构建数据库单例
    * */
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "potato_db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

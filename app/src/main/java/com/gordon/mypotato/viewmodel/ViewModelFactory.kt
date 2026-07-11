package com.gordon.mypotato.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gordon.mypotato.data.AppDatabase
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.PomodoroRepository
import com.gordon.mypotato.data.repository.RoomCategoryRepository
import com.gordon.mypotato.data.repository.RoomPomodoroRepository
import com.gordon.mypotato.data.repository.RoomTaskRepository
import com.gordon.mypotato.data.repository.TaskRepository

class ViewModelFactory(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory {
            return instance ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val taskRepo = RoomTaskRepository(database.taskDao(), database.taskStepDao())
                val categoryRepo = RoomCategoryRepository(database.categoryDao(), database.taskDao())
                val pomodoroRepo = RoomPomodoroRepository(database.pomodoroSessionDao())
                ViewModelFactory(taskRepo, categoryRepo, pomodoroRepo, context.applicationContext).also { instance = it }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TodayViewModel::class.java) -> {
                TodayViewModel(taskRepository, categoryRepository) as T
            }
            modelClass.isAssignableFrom(TasksViewModel::class.java) -> {
                TasksViewModel(taskRepository, categoryRepository) as T
            }
            modelClass.isAssignableFrom(TaskDetailViewModel::class.java) -> {
                TaskDetailViewModel(taskRepository, categoryRepository) as T
            }
            modelClass.isAssignableFrom(TaskEditViewModel::class.java) -> {
                TaskEditViewModel(taskRepository, categoryRepository) as T
            }
            modelClass.isAssignableFrom(PomodoroViewModel::class.java) -> {
                val sharedPreferences = context.getSharedPreferences("MyPotatoSettings", Context.MODE_PRIVATE)
                val focusMinutes = sharedPreferences.getInt(SettingsViewModel.KEY_FOCUS_MINUTES, SettingsViewModel.DEFAULT_FOCUS_MINUTES)
                val shortBreakMinutes = sharedPreferences.getInt(SettingsViewModel.KEY_SHORT_BREAK_MINUTES, SettingsViewModel.DEFAULT_SHORT_BREAK_MINUTES)
                val longBreakMinutes = sharedPreferences.getInt(SettingsViewModel.KEY_LONG_BREAK_MINUTES, SettingsViewModel.DEFAULT_LONG_BREAK_MINUTES)
                val longBreakInterval = sharedPreferences.getInt(SettingsViewModel.KEY_LONG_BREAK_INTERVAL, SettingsViewModel.DEFAULT_LONG_BREAK_INTERVAL)
                PomodoroViewModel(
                    taskRepository,
                    pomodoroRepository,
                    focusMinutes,
                    shortBreakMinutes,
                    longBreakMinutes,
                    longBreakInterval
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(context) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
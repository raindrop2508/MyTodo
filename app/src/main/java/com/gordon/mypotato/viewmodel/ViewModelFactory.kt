package com.gordon.mypotato.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.FakeCategoryRepository
import com.gordon.mypotato.data.repository.FakePomodoroRepository
import com.gordon.mypotato.data.repository.FakeTaskRepository
import com.gordon.mypotato.data.repository.PomodoroRepository
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
                val taskRepo = FakeTaskRepository.getInstance()
                val categoryRepo = FakeCategoryRepository.getInstance(taskRepo)
                val pomodoroRepo = FakePomodoroRepository.getInstance()
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
                val settingsViewModel = SettingsViewModel(context)
                val pomodoroSettings = settingsViewModel.getPomodoroSettings()
                PomodoroViewModel(
                    taskRepository,
                    pomodoroRepository,
                    pomodoroSettings.focusMinutes,
                    pomodoroSettings.shortBreakMinutes,
                    pomodoroSettings.longBreakMinutes,
                    pomodoroSettings.longBreakInterval
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(context) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
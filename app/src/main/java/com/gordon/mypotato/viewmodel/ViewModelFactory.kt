package com.gordon.mypotato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.data.repository.FakeCategoryRepository
import com.gordon.mypotato.data.repository.FakeTaskRepository
import com.gordon.mypotato.data.repository.TaskRepository

class ViewModelFactory(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(): ViewModelFactory {
            return instance ?: synchronized(this) {
                val taskRepo = FakeTaskRepository.getInstance()
                val categoryRepo = FakeCategoryRepository.getInstance(taskRepo)
                ViewModelFactory(taskRepo, categoryRepo).also { instance = it }
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
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
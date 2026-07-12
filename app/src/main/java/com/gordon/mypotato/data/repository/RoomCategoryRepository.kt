package com.gordon.mypotato.data.repository

import com.gordon.mypotato.data.dao.CategoryDao
import com.gordon.mypotato.data.dao.TaskDao
import com.gordon.mypotato.data.mapper.toDomain
import com.gordon.mypotato.data.mapper.toDomainList
import com.gordon.mypotato.data.mapper.toEntity
import com.gordon.mypotato.domain.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getCategories().map { it.toDomainList() }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun addCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
        taskDao.updateCategoryId(id, 0)
    }
}
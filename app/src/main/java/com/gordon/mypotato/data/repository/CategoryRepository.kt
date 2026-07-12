package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getCategories(): Flow<List<Category>>

    suspend fun getCategoryById(id: Long): Category?

    suspend fun addCategory(category: Category): Long

    suspend fun updateCategory(category: Category)

    suspend fun deleteCategory(id: Long)
}
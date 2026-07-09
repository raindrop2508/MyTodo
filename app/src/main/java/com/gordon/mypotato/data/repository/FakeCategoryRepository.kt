package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong

class FakeCategoryRepository private constructor(
    private val taskRepository: TaskRepository
) : CategoryRepository {

    private val categories = mutableListOf<Category>()
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val categoryIdGenerator = AtomicLong(1)

    init {
        initDefaultCategories()
    }

    companion object {
        @Volatile
        private var instance: FakeCategoryRepository? = null

        fun getInstance(taskRepository: TaskRepository): FakeCategoryRepository {
            return instance ?: synchronized(this) {
                instance ?: FakeCategoryRepository(taskRepository).also { instance = it }
            }
        }
    }

    private fun initDefaultCategories() {
        categories.add(
            Category(
                id = categoryIdGenerator.getAndIncrement(),
                name = "学习",
                colorHex = "#FF6B6B",
                iconName = "ic_category_study"
            )
        )

        categories.add(
            Category(
                id = categoryIdGenerator.getAndIncrement(),
                name = "工作",
                colorHex = "#4ECDC4",
                iconName = "ic_category_work"
            )
        )

        categories.add(
            Category(
                id = categoryIdGenerator.getAndIncrement(),
                name = "生活",
                colorHex = "#FFE66D",
                iconName = "ic_category_life"
            )
        )

        categories.add(
            Category(
                id = categoryIdGenerator.getAndIncrement(),
                name = "健康",
                colorHex = "#95E1D3",
                iconName = "ic_category_health"
            )
        )

        categories.add(
            Category(
                id = categoryIdGenerator.getAndIncrement(),
                name = "购物",
                colorHex = "#F38181",
                iconName = "ic_category_shopping"
            )
        )

        emitCategories()
    }

    private fun emitCategories() {
        categoriesFlow.value = ArrayList(categories)
    }

    /**
     * 获取所有分类的响应式流
     *
     * @return Flow<List<Category>> 分类列表的 StateFlow，数据变更时自动通知订阅者
     */
    override fun getCategories(): Flow<List<Category>> {
        return categoriesFlow.asStateFlow()
    }

    /**
     * 根据分类 ID 获取单个分类
     *
     * @param id 分类 ID
     * @return Category? 找到的分类对象，未找到返回 null
     */
    override suspend fun getCategoryById(id: Long): Category? {
        delay(100)
        return categories.find { it.id == id }
    }

    /**
     * 添加新分类
     *
     * @param category 分类对象（id 字段会被自动生成覆盖）
     * @return Long 新生成的分类 ID
     */
    override suspend fun addCategory(category: Category): Long {
        delay(100)
        val newId = categoryIdGenerator.getAndIncrement()
        val newCategory = category.copy(id = newId)
        categories.add(newCategory)
        emitCategories()
        return newId
    }

    /**
     * 更新分类信息
     *
     * @param category 更新后的分类对象，根据 id 匹配进行替换
     */
    override suspend fun updateCategory(category: Category) {
        delay(100)
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            emitCategories()
        }
    }

    /**
     * 删除分类
     *
     * @param id 分类 ID
     */
    override suspend fun deleteCategory(id: Long) {
        delay(100)
        categories.removeAll { it.id == id }
        
        val allTasks = taskRepository.getTasks().first()
        allTasks.forEach { task ->
            if (task.categoryId == id) {
                taskRepository.updateTask(task.copy(categoryId = 0))
            }
        }
        
        emitCategories()
    }
}
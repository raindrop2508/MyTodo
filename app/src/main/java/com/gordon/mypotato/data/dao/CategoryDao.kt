package com.gordon.mypotato.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gordon.mypotato.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
* 一次性操作 与 响应式流
 * 返回 Flow 或 LiveData 的方法不加 suspend，而直接返回结果的方法必须加 suspend。
 * 对于 Flow 或 LiveData 其仅仅是产生一个管道（Stream）；
 * 真正的数据库查询是在点击 collect（收集流）的时候，由 Room 在其内部的后台线程触发的。
* */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY id ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)
}
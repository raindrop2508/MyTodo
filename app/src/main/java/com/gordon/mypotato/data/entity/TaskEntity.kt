package com.gordon.mypotato.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index("category_id"),
        Index("status"),
        Index("created_at"),
        Index("is_urgent"),
        Index("is_important")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String?,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "task_type")
    val taskType: Int,

    @ColumnInfo(name = "status")
    val status: Int,

    @ColumnInfo(name = "is_urgent")
    val isUrgent: Boolean,

    @ColumnInfo(name = "is_important")
    val isImportant: Boolean,

    @ColumnInfo(name = "category_id", defaultValue = "0")
    val categoryId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "planned_start_at")
    val plannedStartAt: Long?,

    @ColumnInfo(name = "finished_at")
    val finishedAt: Long?,

    @ColumnInfo(name = "total_duration_sec")
    val totalDurationSec: Long
)
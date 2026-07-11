package com.gordon.mypotato.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_step",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("task_id"),
        Index("sort_order"),
        Index("status")
    ]
)
data class TaskStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "status")
    val status: Int,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,

    @ColumnInfo(name = "spent_duration_sec")
    val spentDurationSec: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
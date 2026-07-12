package com.gordon.mypotato.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pomodoro_session",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["step_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("task_id"),
        Index("step_id"),
        Index("status"),
        Index("started_at")
    ]
)
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,

    @ColumnInfo(name = "step_id")
    val stepId: Long?,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,

    @ColumnInfo(name = "focus_duration_sec")
    val focusDurationSec: Long,

    @ColumnInfo(name = "break_duration_sec")
    val breakDurationSec: Long,

    @ColumnInfo(name = "paused_duration_sec")
    val pausedDurationSec: Long = 0,

    @ColumnInfo(name = "cycles")
    val cycles: Int,

    @ColumnInfo(name = "status")
    val status: Int
)
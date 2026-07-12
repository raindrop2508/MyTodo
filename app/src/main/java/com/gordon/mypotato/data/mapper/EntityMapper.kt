package com.gordon.mypotato.data.mapper

import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStep

fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        title = this.title,
        content = this.content,
        note = this.note,
        taskType = this.taskType,
        status = this.status,
        isUrgent = this.isUrgent,
        isImportant = this.isImportant,
        categoryId = this.categoryId,
        createdAt = this.createdAt,
        plannedStartAt = this.plannedStartAt,
        finishedAt = this.finishedAt,
        totalDurationSec = this.totalDurationSec
    )
}

fun TaskStepEntity.toDomain(): TaskStep {
    return TaskStep(
        id = this.id,
        taskId = this.taskId,
        title = this.title,
        sortOrder = this.sortOrder,
        status = this.status,
        completedAt = this.completedAt,
        spentDurationSec = this.spentDurationSec,
        createdAt = this.createdAt
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = this.id,
        name = this.name,
        colorHex = this.colorHex,
        iconName = this.iconName
    )
}

fun PomodoroSessionEntity.toDomain(): PomodoroSession {
    return PomodoroSession(
        id = this.id,
        taskId = this.taskId,
        stepId = this.stepId,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        focusDurationSec = this.focusDurationSec,
        breakDurationSec = this.breakDurationSec,
        pausedDurationSec = this.pausedDurationSec,
        cycles = this.cycles,
        status = this.status
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        title = this.title,
        content = this.content,
        note = this.note,
        taskType = this.taskType,
        status = this.status,
        isUrgent = this.isUrgent,
        isImportant = this.isImportant,
        categoryId = this.categoryId,
        createdAt = this.createdAt,
        plannedStartAt = this.plannedStartAt,
        finishedAt = this.finishedAt,
        totalDurationSec = this.totalDurationSec
    )
}

fun TaskStep.toEntity(): TaskStepEntity {
    return TaskStepEntity(
        id = this.id,
        taskId = this.taskId,
        title = this.title,
        sortOrder = this.sortOrder,
        status = this.status,
        completedAt = this.completedAt,
        spentDurationSec = this.spentDurationSec,
        createdAt = this.createdAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        name = this.name,
        colorHex = this.colorHex,
        iconName = this.iconName
    )
}

fun PomodoroSession.toEntity(): PomodoroSessionEntity {
    return PomodoroSessionEntity(
        id = this.id,
        taskId = this.taskId,
        stepId = this.stepId,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        focusDurationSec = this.focusDurationSec,
        breakDurationSec = this.breakDurationSec,
        pausedDurationSec = this.pausedDurationSec,
        cycles = this.cycles,
        status = this.status
    )
}

@JvmName("taskEntityListToDomain")
fun List<TaskEntity>.toDomainList(): List<Task> {
    return this.map { it.toDomain() }
}

@JvmName("taskStepEntityListToDomain")
fun List<TaskStepEntity>.toDomainList(): List<TaskStep> {
    return this.map { it.toDomain() }
}

@JvmName("categoryEntityListToDomain")
fun List<CategoryEntity>.toDomainList(): List<Category> {
    return this.map { it.toDomain() }
}

@JvmName("pomodoroSessionEntityListToDomain")
fun List<PomodoroSessionEntity>.toDomainList(): List<PomodoroSession> {
    return this.map { it.toDomain() }
}
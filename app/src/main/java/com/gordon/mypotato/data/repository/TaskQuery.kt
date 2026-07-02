package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskType

data class TaskQuery(
    val categoryId: Long? = null,
    val taskType: TaskType? = null,
    val status: TaskStatus? = null,
    val isUrgent: Boolean? = null,
    val isImportant: Boolean? = null,
    val keyword: String? = null,
    val offset: Int = 0,
    val limit: Int = 50
)
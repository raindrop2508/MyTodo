package com.gordon.mypotato.data.backup

import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupSnapshot(
    val version: Int = CURRENT_VERSION,
    val categories: List<CategoryEntity>,
    val tasks: List<TaskEntity>,
    val steps: List<TaskStepEntity>,
    val sessions: List<PomodoroSessionEntity>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

object BackupJsonCodec {

    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("version", snapshot.version)
        root.put("categories", JSONArray().also { array ->
            snapshot.categories.forEach { array.put(categoryToJson(it)) }
        })
        root.put("tasks", JSONArray().also { array ->
            snapshot.tasks.forEach { array.put(taskToJson(it)) }
        })
        root.put("steps", JSONArray().also { array ->
            snapshot.steps.forEach { array.put(stepToJson(it)) }
        })
        root.put("sessions", JSONArray().also { array ->
            snapshot.sessions.forEach { array.put(sessionToJson(it)) }
        })
        return root.toString(2)
    }

    fun decode(json: String): BackupSnapshot {
        val root = JSONObject(json)
        val version = root.optInt("version", BackupSnapshot.CURRENT_VERSION)
        return BackupSnapshot(
            version = version,
            categories = readArray(root, "categories", ::categoryFromJson),
            tasks = readArray(root, "tasks", ::taskFromJson),
            steps = readArray(root, "steps", ::stepFromJson),
            sessions = readArray(root, "sessions", ::sessionFromJson),
        )
    }

    private fun <T> readArray(root: JSONObject, key: String, transform: (JSONObject) -> T): List<T> {
        val array = root.optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index -> transform(array.getJSONObject(index)) }
    }

    private fun categoryToJson(entity: CategoryEntity) = JSONObject().also { obj ->
        obj.put("id", entity.id)
        obj.put("name", entity.name)
        obj.put("colorHex", entity.colorHex)
        obj.putNullable(entity.iconName, "iconName")
    }

    private fun categoryFromJson(obj: JSONObject) = CategoryEntity(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        colorHex = obj.getString("colorHex"),
        iconName = obj.optNullableString("iconName"),
    )

    private fun taskToJson(entity: TaskEntity) = JSONObject().also { obj ->
        obj.put("id", entity.id)
        obj.put("title", entity.title)
        obj.putNullable(entity.content, "content")
        obj.putNullable(entity.note, "note")
        obj.put("taskType", entity.taskType)
        obj.put("status", entity.status)
        obj.put("isUrgent", entity.isUrgent)
        obj.put("isImportant", entity.isImportant)
        obj.put("categoryId", entity.categoryId)
        obj.put("createdAt", entity.createdAt)
        obj.putNullable(entity.plannedStartAt, "plannedStartAt")
        obj.putNullable(entity.finishedAt, "finishedAt")
        obj.put("totalDurationSec", entity.totalDurationSec)
    }

    private fun taskFromJson(obj: JSONObject) = TaskEntity(
        id = obj.getLong("id"),
        title = obj.getString("title"),
        content = obj.optNullableString("content"),
        note = obj.optNullableString("note"),
        taskType = obj.getInt("taskType"),
        status = obj.getInt("status"),
        isUrgent = obj.getBoolean("isUrgent"),
        isImportant = obj.getBoolean("isImportant"),
        categoryId = obj.getLong("categoryId"),
        createdAt = obj.getLong("createdAt"),
        plannedStartAt = obj.optNullableLong("plannedStartAt"),
        finishedAt = obj.optNullableLong("finishedAt"),
        totalDurationSec = obj.getLong("totalDurationSec"),
    )

    private fun stepToJson(entity: TaskStepEntity) = JSONObject().also { obj ->
        obj.put("id", entity.id)
        obj.put("taskId", entity.taskId)
        obj.put("title", entity.title)
        obj.put("sortOrder", entity.sortOrder)
        obj.put("status", entity.status)
        obj.putNullable(entity.completedAt, "completedAt")
        obj.put("spentDurationSec", entity.spentDurationSec)
        obj.put("createdAt", entity.createdAt)
    }

    private fun stepFromJson(obj: JSONObject) = TaskStepEntity(
        id = obj.getLong("id"),
        taskId = obj.getLong("taskId"),
        title = obj.getString("title"),
        sortOrder = obj.getInt("sortOrder"),
        status = obj.getInt("status"),
        completedAt = obj.optNullableLong("completedAt"),
        spentDurationSec = obj.getLong("spentDurationSec"),
        createdAt = obj.getLong("createdAt"),
    )

    private fun sessionToJson(entity: PomodoroSessionEntity) = JSONObject().also { obj ->
        obj.put("id", entity.id)
        obj.put("taskId", entity.taskId)
        obj.putNullable(entity.stepId, "stepId")
        obj.put("startedAt", entity.startedAt)
        obj.putNullable(entity.endedAt, "endedAt")
        obj.put("focusDurationSec", entity.focusDurationSec)
        obj.put("breakDurationSec", entity.breakDurationSec)
        obj.put("pausedDurationSec", entity.pausedDurationSec)
        obj.put("cycles", entity.cycles)
        obj.put("status", entity.status)
        obj.put("phase", entity.phase)
        obj.put("plannedDurationMs", entity.plannedDurationMs)
        obj.putNullable(entity.targetEndEpochMs, "targetEndEpochMs")
        obj.put("remainingMsWhenPaused", entity.remainingMsWhenPaused)
        obj.put("pauseStartedAtEpochMs", entity.pauseStartedAtEpochMs)
    }

    private fun sessionFromJson(obj: JSONObject) = PomodoroSessionEntity(
        id = obj.getLong("id"),
        taskId = obj.getLong("taskId"),
        stepId = obj.optNullableLong("stepId"),
        startedAt = obj.getLong("startedAt"),
        endedAt = obj.optNullableLong("endedAt"),
        focusDurationSec = obj.getLong("focusDurationSec"),
        breakDurationSec = obj.getLong("breakDurationSec"),
        pausedDurationSec = obj.optLong("pausedDurationSec", 0),
        cycles = obj.getInt("cycles"),
        status = obj.getInt("status"),
        phase = obj.optInt("phase", 0),
        plannedDurationMs = obj.optLong("plannedDurationMs", 0),
        targetEndEpochMs = obj.optNullableLong("targetEndEpochMs"),
        remainingMsWhenPaused = obj.optLong("remainingMsWhenPaused", 0),
        pauseStartedAtEpochMs = obj.optLong("pauseStartedAtEpochMs", 0),
    )

    private fun JSONObject.putNullable(value: String?, key: String) {
        if (value == null) {
            put(key, JSONObject.NULL)
        } else {
            put(key, value)
        }
    }

    private fun JSONObject.putNullable(value: Long?, key: String) {
        if (value == null) {
            put(key, JSONObject.NULL)
        } else {
            put(key, value)
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }
}

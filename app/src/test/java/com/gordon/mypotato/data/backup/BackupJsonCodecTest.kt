package com.gordon.mypotato.data.backup

import com.gordon.mypotato.data.entity.CategoryEntity
import com.gordon.mypotato.data.entity.PomodoroSessionEntity
import com.gordon.mypotato.data.entity.TaskEntity
import com.gordon.mypotato.data.entity.TaskStepEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupJsonCodecTest {

    @Test
    fun encodeDecode_roundTripPreservesEntities() {
        val snapshot = BackupSnapshot(
            version = BackupSnapshot.CURRENT_VERSION,
            categories = listOf(
                CategoryEntity(id = 1, name = "Work", colorHex = "#3366FF", iconName = "work"),
                CategoryEntity(id = 2, name = "Life", colorHex = "#22AA66", iconName = null),
            ),
            tasks = listOf(
                TaskEntity(
                    id = 10,
                    title = "Ship Stage E",
                    content = "Dark mode + i18n",
                    note = null,
                    taskType = 1,
                    status = 0,
                    isUrgent = true,
                    isImportant = true,
                    categoryId = 1,
                    createdAt = 1_700_000_000L,
                    plannedStartAt = 1_700_000_100L,
                    finishedAt = null,
                    totalDurationSec = 120,
                ),
            ),
            steps = listOf(
                TaskStepEntity(
                    id = 100,
                    taskId = 10,
                    title = "Write tests",
                    sortOrder = 0,
                    status = 0,
                    completedAt = null,
                    spentDurationSec = 0,
                    createdAt = 1_700_000_000L,
                ),
            ),
            sessions = listOf(
                PomodoroSessionEntity(
                    id = 1000,
                    taskId = 10,
                    stepId = 100,
                    startedAt = 1_700_000_200L,
                    endedAt = 1_700_001_700L,
                    focusDurationSec = 1500,
                    breakDurationSec = 0,
                    pausedDurationSec = 30,
                    cycles = 1,
                    status = 2,
                    phase = 0,
                    plannedDurationMs = 1_500_000,
                    targetEndEpochMs = null,
                    remainingMsWhenPaused = 0,
                    pauseStartedAtEpochMs = 0,
                ),
            ),
        )

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(snapshot))

        assertEquals(snapshot.version, decoded.version)
        assertEquals(snapshot.categories, decoded.categories)
        assertEquals(snapshot.tasks, decoded.tasks)
        assertEquals(snapshot.steps, decoded.steps)
        assertEquals(snapshot.sessions, decoded.sessions)
    }

    @Test
    fun decode_missingOptionalFields_usesNullDefaults() {
        val json = """
            {
              "version": 1,
              "categories": [{"id": 1, "name": "A", "colorHex": "#000000"}],
              "tasks": [{
                "id": 1,
                "title": "T",
                "taskType": 0,
                "status": 0,
                "isUrgent": false,
                "isImportant": false,
                "categoryId": 0,
                "createdAt": 10,
                "totalDurationSec": 0
              }],
              "steps": [],
              "sessions": [{
                "id": 1,
                "taskId": 1,
                "startedAt": 10,
                "focusDurationSec": 0,
                "breakDurationSec": 0,
                "cycles": 0,
                "status": 0
              }]
            }
        """.trimIndent()

        val decoded = BackupJsonCodec.decode(json)
        assertEquals(1, decoded.categories.size)
        assertNull(decoded.categories[0].iconName)
        assertNull(decoded.tasks[0].content)
        assertNull(decoded.tasks[0].plannedStartAt)
        assertNull(decoded.sessions[0].stepId)
        assertNull(decoded.sessions[0].endedAt)
        assertEquals(0L, decoded.sessions[0].pausedDurationSec)
    }
}

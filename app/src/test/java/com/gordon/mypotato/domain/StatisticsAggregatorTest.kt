package com.gordon.mypotato.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StatisticsAggregatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = ZonedDateTime.of(2026, 10, 18, 15, 0, 0, 0, zone).toInstant()

    @Test
    fun today_countsOnlyTasksAnchoredToday() {
        val page = aggregate(
            listOf(
                task(id = 1, status = TaskStatus.COMPLETED, createdAt = date(2026, 10, 17), finishedAt = date(2026, 10, 18, 10)),
                task(id = 2, status = TaskStatus.COMPLETED, createdAt = date(2026, 10, 16), finishedAt = date(2026, 10, 17, 10)),
                task(id = 3, status = TaskStatus.TODO, createdAt = date(2026, 10, 18, 9)),
                task(id = 4, status = TaskStatus.TODO, createdAt = date(2026, 10, 1)),
                task(id = 5, status = TaskStatus.IN_PROGRESS, createdAt = date(2026, 10, 18, 11)),
                task(id = 6, status = TaskStatus.ARCHIVED, createdAt = date(2026, 10, 18), finishedAt = date(2026, 10, 18, 12)),
            ),
            StatisticsDimension.TIME,
            StatisticsPeriod.TODAY,
        ) as StatisticsPage.TimeSpanPage

        assertEquals(1, page.completion.completedCount)
        assertEquals(1, page.completion.todoCount)
        assertEquals(1, page.completion.inProgressCount)
        assertEquals(33, page.completion.rate)
        assertEquals(1, page.trend.first { it.label == "9时" }.completedCount)
        assertEquals("今日 · 10.18", page.rangeLabel)
    }

    @Test
    fun week_putsMondayCompletionOnFirstBucket() {
        val page = aggregate(
            listOf(
                task(id = 1, status = TaskStatus.COMPLETED, createdAt = date(2026, 10, 12, 8), finishedAt = date(2026, 10, 12, 9)),
            ),
            StatisticsDimension.TIME,
            StatisticsPeriod.WEEK,
        ) as StatisticsPage.TimeSpanPage

        assertEquals(1, page.trend.first { it.label == "一" }.completedCount)
        assertEquals(0, page.trend.first { it.label == "日" }.completedCount)
        assertEquals("本周 · 10.12 – 10.18", page.rangeLabel)
    }

    @Test
    fun emptyPeriod_hasZeroThreeStateCounts() {
        val page = aggregate(
            emptyList(),
            StatisticsDimension.TIME,
            StatisticsPeriod.WEEK,
        ) as StatisticsPage.TimeSpanPage

        assertEquals(0, page.completion.completedCount)
        assertEquals(0, page.completion.todoCount)
        assertEquals(0, page.completion.inProgressCount)
        assertEquals(0, page.completion.rate)
    }

    @Test
    fun emptyPeriod_taskDimension_hasNoCategories() {
        val page = aggregate(
            emptyList(),
            StatisticsDimension.TASK,
            StatisticsPeriod.MONTH,
        ) as StatisticsPage.TaskPage

        assertEquals(0, page.categoryTotalCount)
        assertEquals(true, page.categories.isEmpty())
    }

    @Test
    fun heatmapLevel_capsAtFour() {
        assertEquals(0, StatisticsAggregator.heatmapLevel(0))
        assertEquals(1, StatisticsAggregator.heatmapLevel(1))
        assertEquals(2, StatisticsAggregator.heatmapLevel(2))
        assertEquals(3, StatisticsAggregator.heatmapLevel(3))
        assertEquals(4, StatisticsAggregator.heatmapLevel(4))
        assertEquals(4, StatisticsAggregator.heatmapLevel(5))
    }

    @Test
    fun yearHeatmap_marksCompletionDayAsLevelOne() {
        val page = aggregate(
            listOf(
                task(id = 1, status = TaskStatus.COMPLETED, createdAt = date(2026, 10, 18, 8), finishedAt = date(2026, 10, 18, 9)),
            ),
            StatisticsDimension.TIME,
            StatisticsPeriod.YEAR,
        ) as StatisticsPage.TimeYearPage

        val sunday = page.heatmap.weeks.last().last()
        assertEquals(1, sunday)
        assertEquals("1 个", page.heatmap.yearCompletedLabel)
        assertEquals(53, page.heatmap.weeks.size)
    }

    private fun aggregate(tasks: List<Task>, dimension: StatisticsDimension, period: StatisticsPeriod): StatisticsPage {
        return StatisticsAggregator.aggregate(
            tasks = tasks,
            categories = emptyList(),
            dimension = dimension,
            period = period,
            now = now,
            zone = zone,
        )
    }

    private fun date(year: Int, month: Int, day: Int, hour: Int = 0): Long {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toEpochSecond()
    }

    private fun task(
        id: Long,
        status: TaskStatus,
        createdAt: Long,
        finishedAt: Long? = null,
    ): Task {
        return Task(
            id = id,
            title = "任务$id",
            content = null,
            note = null,
            taskType = TaskType.ONCE.value,
            status = status.value,
            isUrgent = false,
            isImportant = false,
            categoryId = 0,
            createdAt = createdAt,
            plannedStartAt = null,
            finishedAt = finishedAt,
            totalDurationSec = 0,
        )
    }
}

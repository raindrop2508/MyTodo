package com.gordon.mypotato.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToLong

object StatisticsAggregator {

    fun aggregate(
        tasks: List<Task>,
        categories: List<Category>,
        dimension: StatisticsDimension,
        period: StatisticsPeriod,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): StatisticsPage {
        val today = now.atZone(zone).toLocalDate()
        val range = periodRange(period, today)
        val counted = tasks.mapNotNull { task -> classify(task, range, zone) }
        return when (dimension) {
            StatisticsDimension.TIME -> when (period) {
                StatisticsPeriod.YEAR -> yearPage(tasks, counted, today, zone)
                else -> spanPage(period, counted, range, zone)
            }
            StatisticsDimension.TASK -> taskPage(counted, categories, period, range)
        }
    }

    fun heatmapLevel(completedCount: Int): Int {
        return completedCount.coerceIn(0, HEATMAP_MAX_LEVEL)
    }

    private fun spanPage(
        period: StatisticsPeriod,
        counted: List<CountedTask>,
        range: DateRange,
        zone: ZoneId,
    ): StatisticsPage.TimeSpanPage {
        val overview = overview(counted)
        return StatisticsPage.TimeSpanPage(
            rangeLabel = rangeLabel(period, range),
            completion = overview,
            trend = trend(period, counted, zone),
            efficiency = SpanEfficiency(
                completionRate = overview.rate,
                averageDurationLabel = averageDurationLabel(counted),
                longestStreakLabel = "${longestStreak(counted, range)}天",
            ),
        )
    }

    private fun yearPage(
        tasks: List<Task>,
        counted: List<CountedTask>,
        today: LocalDate,
        zone: ZoneId,
    ): StatisticsPage.TimeYearPage {
        val yearRange = periodRange(StatisticsPeriod.YEAR, today)
        val overview = overview(counted)
        val heatmap = buildHeatmap(tasks, today, zone)
        return StatisticsPage.TimeYearPage(
            rangeLabel = "${today.year} 全年",
            heatmap = heatmap.data,
            efficiency = YearEfficiency(
                completionRate = overview.rate,
                dailyAverageLabel = formatOneDecimal(heatmap.completedInYear / DAYS_IN_YEAR, "个"),
                longestStreakLabel = "${longestStreak(counted, yearRange)}天",
            ),
            quality = quality(overview),
        )
    }

    private fun taskPage(
        counted: List<CountedTask>,
        categories: List<Category>,
        period: StatisticsPeriod,
        range: DateRange,
    ): StatisticsPage.TaskPage {
        val names = categories.associate { it.id to it.name }
        val categoryRates = counted
            .groupBy { it.task.categoryId }
            .map { (categoryId, items) ->
                val name = if (categoryId == 0L) UNCATEGORIZED else names[categoryId] ?: UNCATEGORIZED
                CategoryRate(name = name, rate = rate(items.count { it.bucket == StatusBucket.COMPLETED }, items.size))
            }
            .sortedByDescending { it.rate }
        return StatisticsPage.TaskPage(
            rangeLabel = rangeLabel(period, range),
            categoryTotalCount = categoryRates.size,
            categories = categoryRates,
            priorities = listOf(
                priorityRate(counted, "紧急且重要") { it.isUrgent && it.isImportant },
                priorityRate(counted, "重要") { !it.isUrgent && it.isImportant },
                priorityRate(counted, "紧急") { it.isUrgent && !it.isImportant },
                priorityRate(counted, "其他") { !it.isUrgent && !it.isImportant },
            ),
        )
    }

    private fun buildHeatmap(tasks: List<Task>, today: LocalDate, zone: ZoneId): HeatmapBuild {
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstMonday = thisMonday.minusWeeks((HEATMAP_WEEKS - 1).toLong())
        val counts = mutableMapOf<LocalDate, Int>()
        tasks.forEach { task ->
            if (task.status != TaskStatus.COMPLETED.value) return@forEach
            val date = finishDate(task, zone) ?: return@forEach
            counts[date] = (counts[date] ?: 0) + 1
        }
        val weeks = List(HEATMAP_WEEKS) { weekIndex ->
            val monday = firstMonday.plusWeeks(weekIndex.toLong())
            List(DAY_COUNT) { dayIndex ->
                heatmapLevel(counts[monday.plusDays(dayIndex.toLong())] ?: 0)
            }
        }
        val monthLabels = mutableListOf<HeatmapMonthLabel>()
        var lastMonth = -1
        repeat(HEATMAP_WEEKS) { weekIndex ->
            val month = firstMonday.plusWeeks(weekIndex.toLong()).monthValue
            if (month != lastMonth) {
                monthLabels += HeatmapMonthLabel(weekIndex, "${month}月")
                lastMonth = month
            }
        }
        val yearStart = LocalDate.of(today.year, 1, 1)
        val yearEnd = LocalDate.of(today.year, 12, 31)
        val yearCompleted = counts.filterKeys { !it.isBefore(yearStart) && !it.isAfter(yearEnd) }.values.sum()
        val activeDays = counts.keys.count { !it.isBefore(yearStart) && !it.isAfter(yearEnd) }
        val lastSunday = thisMonday.plusDays(6)
        return HeatmapBuild(
            data = HeatmapData(
                weeks = weeks,
                monthLabels = monthLabels,
                spanLabel = "${formatYearMonth(firstMonday)} – ${formatYearMonth(lastSunday)}",
                yearCompletedLabel = "$yearCompleted 个",
                activeDaysLabel = "$activeDays 天",
                longestStreakLabel = "${longestStreakDates(counts.keys.filter { !it.isBefore(yearStart) && !it.isAfter(yearEnd) }.toSet(), yearStart, yearEnd)} 天",
            ),
            completedInYear = yearCompleted,
        )
    }

    private fun classify(task: Task, range: DateRange, zone: ZoneId): CountedTask? {
        if (task.status == TaskStatus.ARCHIVED.value) return null
        val anchor = when (task.status) {
            TaskStatus.COMPLETED.value -> finishDate(task, zone)
            TaskStatus.TODO.value, TaskStatus.IN_PROGRESS.value -> instantDate(task.createdAt, zone)
            else -> null
        } ?: return null
        if (anchor.isBefore(range.start) || !anchor.isBefore(range.endExclusive)) return null
        val bucket = when (task.status) {
            TaskStatus.COMPLETED.value -> StatusBucket.COMPLETED
            TaskStatus.IN_PROGRESS.value -> StatusBucket.IN_PROGRESS
            else -> StatusBucket.TODO
        }
        return CountedTask(task, bucket, anchor)
    }

    private fun overview(counted: List<CountedTask>): CompletionOverview {
        val completed = counted.count { it.bucket == StatusBucket.COMPLETED }
        val todo = counted.count { it.bucket == StatusBucket.TODO }
        val inProgress = counted.count { it.bucket == StatusBucket.IN_PROGRESS }
        return CompletionOverview(
            rate = rate(completed, completed + todo + inProgress),
            completedCount = completed,
            todoCount = todo,
            inProgressCount = inProgress,
        )
    }

    private fun quality(overview: CompletionOverview): QualityDistribution {
        val total = overview.completedCount + overview.todoCount + overview.inProgressCount
        if (total == 0) return QualityDistribution(0, 0, 0)
        val completedRate = overview.completedCount * 100 / total
        val todoRate = overview.todoCount * 100 / total
        return QualityDistribution(
            completedRate = completedRate,
            todoRate = todoRate,
            inProgressRate = 100 - completedRate - todoRate,
        )
    }

    private fun trend(
        period: StatisticsPeriod,
        counted: List<CountedTask>,
        zone: ZoneId,
    ): List<TrendBucket> {
        val completed = counted.filter { it.bucket == StatusBucket.COMPLETED }
        return when (period) {
            StatisticsPeriod.TODAY -> TODAY_LABELS.mapIndexed { index, label ->
                TrendBucket(label, completed.count { hourBucket(it.task, zone) == index })
            }
            StatisticsPeriod.WEEK -> WEEK_LABELS.mapIndexed { index, label ->
                TrendBucket(label, completed.count { it.anchor.dayOfWeek.value == index + 1 })
            }
            StatisticsPeriod.MONTH -> MONTH_LABELS.mapIndexed { index, label ->
                TrendBucket(label, completed.count { monthWeekIndex(it.anchor) == index })
            }
            StatisticsPeriod.YEAR -> emptyList()
        }
    }

    private fun hourBucket(task: Task, zone: ZoneId): Int {
        val hour = Instant.ofEpochSecond(task.finishedAt ?: return -1).atZone(zone).hour
        return when (hour) {
            in 0..8 -> 0
            in 9..11 -> 1
            in 12..14 -> 2
            in 15..17 -> 3
            in 18..20 -> 4
            else -> 5
        }
    }

    private fun monthWeekIndex(date: LocalDate): Int {
        return when (date.dayOfMonth) {
            in 1..7 -> 0
            in 8..14 -> 1
            in 15..21 -> 2
            else -> 3
        }
    }

    private fun averageDurationLabel(counted: List<CountedTask>): String {
        val durations = counted.mapNotNull { item ->
            if (item.bucket != StatusBucket.COMPLETED) return@mapNotNull null
            val stored = item.task.totalDurationSec
            if (stored > 0) {
                stored
            } else {
                val finishedAt = item.task.finishedAt ?: return@mapNotNull null
                (finishedAt - item.task.createdAt).coerceAtLeast(0)
            }
        }
        if (durations.isEmpty()) return "0分钟"
        val average = durations.sum() / durations.size
        return when {
            average >= SECONDS_PER_DAY -> formatOneDecimal(average / SECONDS_PER_DAY.toDouble(), "天")
            average >= SECONDS_PER_HOUR -> formatOneDecimal(average / SECONDS_PER_HOUR.toDouble(), "小时")
            else -> "${average / SECONDS_PER_MINUTE}分钟"
        }
    }

    private fun longestStreak(counted: List<CountedTask>, range: DateRange): Int {
        val dates = counted.filter { it.bucket == StatusBucket.COMPLETED }.map { it.anchor }.toSet()
        val endInclusive = range.endExclusive.minusDays(1)
        return longestStreakDates(dates, range.start, endInclusive)
    }

    private fun longestStreakDates(dates: Set<LocalDate>, start: LocalDate, endInclusive: LocalDate): Int {
        var best = 0
        var current = 0
        var day = start
        while (!day.isAfter(endInclusive)) {
            if (day in dates) {
                current += 1
                if (current > best) best = current
            } else {
                current = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    private fun priorityRate(counted: List<CountedTask>, name: String, match: (Task) -> Boolean): PriorityRate {
        val items = counted.filter { match(it.task) }
        val completed = items.count { it.bucket == StatusBucket.COMPLETED }
        return PriorityRate(name = name, rate = rate(completed, items.size))
    }

    private fun rate(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return completed * 100 / total
    }

    private fun periodRange(period: StatisticsPeriod, today: LocalDate): DateRange {
        return when (period) {
            StatisticsPeriod.TODAY -> DateRange(today, today.plusDays(1))
            StatisticsPeriod.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                DateRange(monday, monday.plusWeeks(1))
            }
            StatisticsPeriod.MONTH -> {
                val first = today.withDayOfMonth(1)
                DateRange(first, first.plusMonths(1))
            }
            StatisticsPeriod.YEAR -> {
                val first = LocalDate.of(today.year, 1, 1)
                DateRange(first, first.plusYears(1))
            }
        }
    }

    private fun rangeLabel(period: StatisticsPeriod, range: DateRange): String {
        val endInclusive = range.endExclusive.minusDays(1)
        return when (period) {
            StatisticsPeriod.TODAY -> "今日 · ${formatMonthDay(range.start)}"
            StatisticsPeriod.WEEK -> "本周 · ${formatMonthDay(range.start)} – ${formatMonthDay(endInclusive)}"
            StatisticsPeriod.MONTH -> "本月 · ${formatMonthDay(range.start)} – ${formatMonthDay(endInclusive)}"
            StatisticsPeriod.YEAR -> "${range.start.year} 全年"
        }
    }

    private fun finishDate(task: Task, zone: ZoneId): LocalDate? {
        val finishedAt = task.finishedAt ?: return null
        return instantDate(finishedAt, zone)
    }

    private fun instantDate(epochSeconds: Long, zone: ZoneId): LocalDate {
        return Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDate()
    }

    private fun formatMonthDay(date: LocalDate): String {
        return "${date.monthValue}.${date.dayOfMonth}"
    }

    private fun formatYearMonth(date: LocalDate): String {
        return "${date.year}.${date.monthValue.toString().padStart(2, '0')}"
    }

    private fun formatOneDecimal(value: Double, suffix: String): String {
        val scaled = (value * 10).roundToLong()
        val whole = scaled / 10
        val fraction = kotlin.math.abs(scaled % 10)
        return "$whole.$fraction$suffix"
    }

    private data class DateRange(
        val start: LocalDate,
        val endExclusive: LocalDate,
    )

    private data class CountedTask(
        val task: Task,
        val bucket: StatusBucket,
        val anchor: LocalDate,
    )

    private data class HeatmapBuild(
        val data: HeatmapData,
        val completedInYear: Int,
    )

    private enum class StatusBucket {
        COMPLETED,
        TODO,
        IN_PROGRESS,
    }

}

private const val HEATMAP_WEEKS = 53
private const val HEATMAP_MAX_LEVEL = 4
private const val DAY_COUNT = 7
private const val DAYS_IN_YEAR = 365.0
private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_MINUTE = 60L
private const val UNCATEGORIZED = "未分类"
private val TODAY_LABELS = listOf("6时", "9时", "12时", "15时", "18时", "21时")
private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private val MONTH_LABELS = listOf("第1周", "第2周", "第3周", "第4周")

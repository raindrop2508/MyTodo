package com.gordon.mypotato.domain

enum class StatisticsDimension {
    TIME,
    TASK,
}

enum class StatisticsPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
}

data class CompletionOverview(
    val rate: Int,
    val completedCount: Int,
    val overdueCount: Int,
    val pendingCount: Int,
)

data class TrendBucket(
    val label: String,
    val onTimeCount: Int,
    val overdueCount: Int,
)

data class SpanEfficiency(
    val onTimeRate: Int,
    val averageDurationLabel: String,
    val longestStreakLabel: String,
)

data class YearEfficiency(
    val onTimeRate: Int,
    val dailyAverageLabel: String,
    val longestStreakLabel: String,
)

data class HeatmapMonthLabel(
    val columnIndex: Int,
    val label: String,
)

data class HeatmapData(
    val weeks: List<List<Int>>,
    val monthLabels: List<HeatmapMonthLabel>,
    val spanLabel: String,
    val yearCompletedLabel: String,
    val activeDaysLabel: String,
    val longestStreakLabel: String,
)

data class QualityDistribution(
    val onTimeRate: Int,
    val overdueRate: Int,
    val unfinishedRate: Int,
)

data class CategoryRate(
    val name: String,
    val rate: Int,
)

data class PriorityRate(
    val name: String,
    val rate: Int,
)

sealed class StatisticsPage {
    abstract val rangeLabel: String

    data class TimeSpanPage(
        override val rangeLabel: String,
        val completion: CompletionOverview,
        val trend: List<TrendBucket>,
        val efficiency: SpanEfficiency,
    ) : StatisticsPage()

    data class TimeYearPage(
        override val rangeLabel: String,
        val heatmap: HeatmapData,
        val efficiency: YearEfficiency,
        val quality: QualityDistribution,
    ) : StatisticsPage()

    data class TaskPage(
        override val rangeLabel: String,
        val categoryTotalCount: Int,
        val categories: List<CategoryRate>,
        val priorities: List<PriorityRate>,
    ) : StatisticsPage()
}

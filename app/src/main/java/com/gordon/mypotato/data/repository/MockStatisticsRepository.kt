package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.CategoryRate
import com.gordon.mypotato.domain.CompletionOverview
import com.gordon.mypotato.domain.HeatmapData
import com.gordon.mypotato.domain.HeatmapMonthLabel
import com.gordon.mypotato.domain.PriorityRate
import com.gordon.mypotato.domain.QualityDistribution
import com.gordon.mypotato.domain.SpanEfficiency
import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import com.gordon.mypotato.domain.TrendBucket
import com.gordon.mypotato.domain.YearEfficiency
import kotlin.random.Random

class MockStatisticsRepository : StatisticsRepository {

    override fun load(dimension: StatisticsDimension, period: StatisticsPeriod): StatisticsPage {
        return when (dimension) {
            StatisticsDimension.TIME -> when (period) {
                StatisticsPeriod.YEAR -> yearPage()
                else -> spanPage(period)
            }
            StatisticsDimension.TASK -> taskPage(period)
        }
    }

    private fun spanPage(period: StatisticsPeriod): StatisticsPage.TimeSpanPage {
        return when (period) {
            StatisticsPeriod.TODAY -> StatisticsPage.TimeSpanPage(
                rangeLabel = "今日 · 9.18",
                completion = CompletionOverview(rate = 67, completedCount = 4, overdueCount = 1, pendingCount = 1),
                trend = listOf(
                    TrendBucket("6时", 1, 0),
                    TrendBucket("9时", 2, 1),
                    TrendBucket("12时", 1, 0),
                    TrendBucket("15时", 0, 0),
                    TrendBucket("18时", 1, 0),
                    TrendBucket("21时", 0, 0),
                ),
                efficiency = SpanEfficiency(
                    onTimeRate = 75,
                    averageDurationLabel = "0.6天",
                    longestStreakLabel = "3天",
                ),
            )
            StatisticsPeriod.WEEK -> StatisticsPage.TimeSpanPage(
                rangeLabel = "本周 · 9.14 – 9.20",
                completion = CompletionOverview(rate = 78, completedCount = 18, overdueCount = 2, pendingCount = 5),
                trend = listOf(
                    TrendBucket("一", 3, 1),
                    TrendBucket("二", 4, 0),
                    TrendBucket("三", 2, 1),
                    TrendBucket("四", 5, 0),
                    TrendBucket("五", 3, 0),
                    TrendBucket("六", 1, 0),
                    TrendBucket("日", 0, 0),
                ),
                efficiency = SpanEfficiency(
                    onTimeRate = 82,
                    averageDurationLabel = "1.8天",
                    longestStreakLabel = "12天",
                ),
            )
            StatisticsPeriod.MONTH -> StatisticsPage.TimeSpanPage(
                rangeLabel = "本月 · 9.1 – 9.30",
                completion = CompletionOverview(rate = 74, completedCount = 46, overdueCount = 6, pendingCount = 10),
                trend = listOf(
                    TrendBucket("第1周", 12, 2),
                    TrendBucket("第2周", 9, 3),
                    TrendBucket("第3周", 14, 1),
                    TrendBucket("第4周", 11, 0),
                ),
                efficiency = SpanEfficiency(
                    onTimeRate = 79,
                    averageDurationLabel = "2.1天",
                    longestStreakLabel = "9天",
                ),
            )
            StatisticsPeriod.YEAR -> error("Year page is built by yearPage()")
        }
    }

    private fun yearPage(): StatisticsPage.TimeYearPage {
        return StatisticsPage.TimeYearPage(
            rangeLabel = "2026 全年",
            heatmap = HeatmapData(
                weeks = heatmapWeeks(),
                monthLabels = listOf(
                    HeatmapMonthLabel(0, "9月"),
                    HeatmapMonthLabel(8, "11月"),
                    HeatmapMonthLabel(17, "1月"),
                    HeatmapMonthLabel(26, "3月"),
                    HeatmapMonthLabel(34, "5月"),
                    HeatmapMonthLabel(43, "7月"),
                    HeatmapMonthLabel(51, "9月"),
                ),
                spanLabel = "2025.09 – 2026.09",
                yearCompletedLabel = "967 个",
                activeDaysLabel = "297 天",
                longestStreakLabel = "26 天",
            ),
            efficiency = YearEfficiency(
                onTimeRate = 85,
                dailyAverageLabel = "2.6个",
                longestStreakLabel = "26天",
            ),
            quality = QualityDistribution(onTimeRate = 82, overdueRate = 12, unfinishedRate = 6),
        )
    }

    private fun taskPage(period: StatisticsPeriod): StatisticsPage.TaskPage {
        val categories = listOf(
            CategoryRate("工作", 92),
            CategoryRate("学习", 75),
            CategoryRate("健康", 60),
            CategoryRate("购物", 48),
            CategoryRate("阅读", 42),
            CategoryRate("理财", 35),
            CategoryRate("家务", 28),
            CategoryRate("其他", 20),
        )
        val priorities = listOf(
            PriorityRate("紧急且重要", 85),
            PriorityRate("重要", 78),
            PriorityRate("紧急", 70),
            PriorityRate("其他", 60),
        )
        val scale = when (period) {
            StatisticsPeriod.TODAY -> -18
            StatisticsPeriod.WEEK -> 0
            StatisticsPeriod.MONTH -> -6
            StatisticsPeriod.YEAR -> 4
        }
        return StatisticsPage.TaskPage(
            rangeLabel = rangeLabel(period),
            categoryTotalCount = categories.size,
            categories = categories.map { it.copy(rate = (it.rate + scale).coerceIn(0, 100)) },
            priorities = priorities.map { it.copy(rate = (it.rate + scale).coerceIn(0, 100)) },
        )
    }

    private fun rangeLabel(period: StatisticsPeriod): String {
        return when (period) {
            StatisticsPeriod.TODAY -> "今日 · 9.18"
            StatisticsPeriod.WEEK -> "本周 · 9.14 – 9.20"
            StatisticsPeriod.MONTH -> "本月 · 9.1 – 9.30"
            StatisticsPeriod.YEAR -> "2026 全年"
        }
    }

    private fun heatmapWeeks(): List<List<Int>> {
        val random = Random(2026)
        return List(WEEK_COUNT) { week ->
            List(DAY_COUNT) { day ->
                if ((week + day) % 11 == 0) 0 else random.nextInt(0, 5)
            }
        }
    }

    private companion object {
        private const val WEEK_COUNT = 53
        private const val DAY_COUNT = 7
    }
}

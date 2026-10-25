package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.StatisticsAggregator
import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId

class RoomStatisticsRepository(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = { Instant.now() },
) : StatisticsRepository {

    override fun observe(dimension: StatisticsDimension, period: StatisticsPeriod): Flow<StatisticsPage> {
        return combine(taskRepository.getTasks(), categoryRepository.getCategories()) { tasks, categories ->
            StatisticsAggregator.aggregate(tasks, categories, dimension, period, now(), zone)
        }
    }
}

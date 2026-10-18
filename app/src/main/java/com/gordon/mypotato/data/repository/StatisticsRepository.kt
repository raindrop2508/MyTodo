package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observe(dimension: StatisticsDimension, period: StatisticsPeriod): Flow<StatisticsPage>
}

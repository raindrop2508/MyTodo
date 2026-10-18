package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod

interface StatisticsRepository {
    fun load(dimension: StatisticsDimension, period: StatisticsPeriod): StatisticsPage
}

package com.gordon.mypotato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.StatisticsRepository
import com.gordon.mypotato.domain.StatisticsAggregator
import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository,
) : ViewModel() {

    private val dimension = MutableStateFlow(StatisticsDimension.TIME)
    private val period = MutableStateFlow(StatisticsPeriod.WEEK)

    val uiState: StateFlow<StatisticsUiState> = kotlinx.coroutines.flow.combine(dimension, period) { dimension, period ->
        dimension to period
    }.flatMapLatest { (dimension, period) ->
        statisticsRepository.observe(dimension, period).map { page ->
            StatisticsUiState(dimension = dimension, period = period, page = page)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatisticsUiState(
            dimension = StatisticsDimension.TIME,
            period = StatisticsPeriod.WEEK,
            page = StatisticsAggregator.aggregate(
                tasks = emptyList(),
                categories = emptyList(),
                dimension = StatisticsDimension.TIME,
                period = StatisticsPeriod.WEEK,
                now = Instant.now(),
            ),
        ),
    )

    fun setDimension(value: StatisticsDimension) {
        dimension.value = value
    }

    fun setPeriod(value: StatisticsPeriod) {
        period.value = value
    }
}

data class StatisticsUiState(
    val dimension: StatisticsDimension,
    val period: StatisticsPeriod,
    val page: StatisticsPage,
)

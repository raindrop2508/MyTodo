package com.gordon.mypotato.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.repository.StatisticsRepository
import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository,
) : ViewModel() {

    private val dimension = MutableStateFlow(StatisticsDimension.TIME)
    private val period = MutableStateFlow(StatisticsPeriod.WEEK)

    val uiState: StateFlow<StatisticsUiState> = combine(dimension, period) { dimension, period ->
        StatisticsUiState(
            dimension = dimension,
            period = period,
            page = statisticsRepository.load(dimension, period),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatisticsUiState(
            dimension = StatisticsDimension.TIME,
            period = StatisticsPeriod.WEEK,
            page = statisticsRepository.load(StatisticsDimension.TIME, StatisticsPeriod.WEEK),
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

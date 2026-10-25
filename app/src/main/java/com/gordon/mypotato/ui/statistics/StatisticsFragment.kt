package com.gordon.mypotato.ui.statistics

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentStatisticsBinding
import com.gordon.mypotato.databinding.ItemStatisticsRateRowBinding
import com.gordon.mypotato.domain.StatisticsDimension
import com.gordon.mypotato.domain.StatisticsPage
import com.gordon.mypotato.domain.StatisticsPeriod
import com.gordon.mypotato.viewmodel.StatisticsUiState
import com.gordon.mypotato.viewmodel.StatisticsViewModel
import com.gordon.mypotato.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding: FragmentStatisticsBinding
        get() = _binding!!

    private lateinit var viewModel: StatisticsViewModel
    private var categoriesExpanded: Boolean = false
    private var latestCategories: StatisticsPage.TaskPage? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(requireContext()))[StatisticsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFilters()
        setupHeatmapLegend()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { render(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        latestCategories = null
    }

    private fun setupFilters() {
        binding.chipGroupDimension.setOnCheckedStateChangeListener { _, checkedIds ->
            val dimension = when (checkedIds.firstOrNull()) {
                R.id.chip_dimension_task -> StatisticsDimension.TASK
                else -> StatisticsDimension.TIME
            }
            viewModel.setDimension(dimension)
        }
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            val period = when (checkedIds.firstOrNull()) {
                R.id.chip_period_today -> StatisticsPeriod.TODAY
                R.id.chip_period_month -> StatisticsPeriod.MONTH
                R.id.chip_period_year -> StatisticsPeriod.YEAR
                else -> StatisticsPeriod.WEEK
            }
            viewModel.setPeriod(period)
        }
        binding.tvCategoryToggle.setOnClickListener {
            categoriesExpanded = !categoriesExpanded
            latestCategories?.let { bindCategories(it) }
        }
    }

    private fun setupHeatmapLegend() {
        val legend = binding.layoutHeatmapLegend
        if (legend.childCount > 0) return
        val size = (8 * resources.displayMetrics.density).toInt()
        val gap = (4 * resources.displayMetrics.density).toInt()
        repeat(StatisticsHeatmapView.LEVEL_COUNT) { level ->
            val cell = View(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            if (level > 0) params.marginStart = gap
            cell.layoutParams = params
            cell.background = GradientDrawable().apply {
                cornerRadius = 2 * resources.displayMetrics.density
                setColor(StatisticsHeatmapView.colorForLevel(requireContext(), level))
            }
            legend.addView(cell)
        }
    }

    private fun render(state: StatisticsUiState) {
        checkChip(binding.chipGroupDimension, dimensionChip(state.dimension))
        checkChip(binding.chipGroupPeriod, periodChip(state.period))
        binding.tvStatisticsRange.text = state.page.rangeLabel
        val empty = isPeriodEmpty(state.page)
        binding.tvStatisticsEmpty.isVisible = empty
        binding.groupTimeSpan.isVisible = !empty && state.page is StatisticsPage.TimeSpanPage
        binding.groupTimeYear.isVisible = !empty && state.page is StatisticsPage.TimeYearPage
        binding.groupTask.isVisible = !empty && state.page is StatisticsPage.TaskPage
        if (empty) return
        when (val page = state.page) {
            is StatisticsPage.TimeSpanPage -> bindSpan(state.period, page)
            is StatisticsPage.TimeYearPage -> bindYear(page)
            is StatisticsPage.TaskPage -> {
                latestCategories = page
                bindCategories(page)
                bindPriorities(page)
            }
        }
    }

    private fun isPeriodEmpty(page: StatisticsPage): Boolean {
        return when (page) {
            is StatisticsPage.TimeSpanPage -> {
                val c = page.completion
                c.completedCount + c.todoCount + c.inProgressCount == 0
            }
            is StatisticsPage.TimeYearPage -> {
                page.quality.completedRate == 0 &&
                    page.quality.todoRate == 0 &&
                    page.quality.inProgressRate == 0
            }
            is StatisticsPage.TaskPage -> page.categories.isEmpty()
        }
    }

    private fun bindSpan(period: StatisticsPeriod, page: StatisticsPage.TimeSpanPage) {
        binding.tvCompletionPeriod.text = periodLabel(period)
        binding.viewCompletionDonut.submitRate(page.completion.rate)
        binding.tvCompletedCount.text = page.completion.completedCount.toString()
        binding.tvTodoCount.text = page.completion.todoCount.toString()
        binding.tvInProgressCount.text = page.completion.inProgressCount.toString()
        binding.viewTrendChart.submitData(
            page.trend.map { bucket ->
                StatisticsBarItem(
                    label = bucket.label,
                    segments = listOf(
                        StatisticsBarSegment("completed", bucket.completedCount, R.color.state_success_green),
                    ),
                )
            },
        )
        binding.tvSpanOnTimeRate.text = getString(R.string.statistics_percent, page.efficiency.completionRate)
        binding.tvSpanDuration.text = page.efficiency.averageDurationLabel
        binding.tvSpanStreak.text = page.efficiency.longestStreakLabel
    }

    private fun bindYear(page: StatisticsPage.TimeYearPage) {
        binding.tvHeatmapRange.text = page.rangeLabel
        binding.viewHeatmap.submit(page.heatmap.weeks, page.heatmap.monthLabels)
        binding.viewHeatmap.doOnNextLayout {
            if (_binding == null) return@doOnNextLayout
            binding.scrollHeatmap.scrollTo(binding.viewHeatmap.width, 0)
        }
        binding.tvHeatmapSpan.text = page.heatmap.spanLabel
        binding.tvYearCompleted.text = page.heatmap.yearCompletedLabel
        binding.tvActiveDays.text = page.heatmap.activeDaysLabel
        binding.tvHeatmapStreak.text = page.heatmap.longestStreakLabel
        binding.tvYearOnTimeRate.text = getString(R.string.statistics_percent, page.efficiency.completionRate)
        binding.tvYearDailyAverage.text = page.efficiency.dailyAverageLabel
        binding.tvYearStreak.text = page.efficiency.longestStreakLabel
        setWeight(binding.viewQualityOnTime, page.quality.completedRate)
        setWeight(binding.viewQualityOverdue, page.quality.todoRate)
        setWeight(binding.viewQualityUnfinished, page.quality.inProgressRate)
        binding.tvQualityOnTime.text = getString(R.string.statistics_quality_on_time, page.quality.completedRate)
        binding.tvQualityOverdue.text = getString(R.string.statistics_quality_overdue, page.quality.todoRate)
        binding.tvQualityUnfinished.text = getString(R.string.statistics_quality_unfinished, page.quality.inProgressRate)
    }

    private fun bindCategories(page: StatisticsPage.TaskPage) {
        binding.tvCategoryCount.text = getString(R.string.statistics_category_count, page.categoryTotalCount)
        val visible = if (categoriesExpanded || page.categories.size <= COLLAPSED_CATEGORY_COUNT) {
            page.categories
        } else {
            page.categories.take(COLLAPSED_CATEGORY_COUNT)
        }
        binding.layoutCategories.removeAllViews()
        visible.forEach { category ->
            addRateRow(binding.layoutCategories, category.name, category.rate)
        }
        val showToggle = page.categories.size > COLLAPSED_CATEGORY_COUNT
        binding.tvCategoryToggle.isVisible = showToggle
        if (showToggle) {
            binding.tvCategoryToggle.text = if (categoriesExpanded) {
                getString(R.string.statistics_collapse)
            } else {
                getString(R.string.statistics_expand_all, page.categories.size)
            }
        }
    }

    private fun bindPriorities(page: StatisticsPage.TaskPage) {
        binding.layoutPriorities.removeAllViews()
        page.priorities.forEach { priority ->
            addRateRow(binding.layoutPriorities, priority.name, priority.rate)
        }
    }

    private fun addRateRow(parent: LinearLayout, name: String, rate: Int) {
        val row = ItemStatisticsRateRowBinding.inflate(layoutInflater, parent, false)
        row.tvRateName.text = name
        row.tvRateValue.text = getString(R.string.statistics_percent, rate)
        setWeight(row.viewRateFill, rate)
        setWeight(row.viewRateRest, 100 - rate)
        parent.addView(row.root)
    }

    private fun checkChip(group: com.google.android.material.chip.ChipGroup, chipId: Int) {
        if (group.checkedChipId != chipId) {
            group.check(chipId)
        }
    }

    private fun setWeight(view: View, weight: Int) {
        val params = view.layoutParams as LinearLayout.LayoutParams
        params.weight = weight.coerceAtLeast(0).toFloat()
        view.layoutParams = params
    }

    private fun dimensionChip(dimension: StatisticsDimension): Int {
        return when (dimension) {
            StatisticsDimension.TIME -> R.id.chip_dimension_time
            StatisticsDimension.TASK -> R.id.chip_dimension_task
        }
    }

    private fun periodChip(period: StatisticsPeriod): Int {
        return when (period) {
            StatisticsPeriod.TODAY -> R.id.chip_period_today
            StatisticsPeriod.WEEK -> R.id.chip_period_week
            StatisticsPeriod.MONTH -> R.id.chip_period_month
            StatisticsPeriod.YEAR -> R.id.chip_period_year
        }
    }

    private fun periodLabel(period: StatisticsPeriod): String {
        return when (period) {
            StatisticsPeriod.TODAY -> getString(R.string.statistics_tab_today)
            StatisticsPeriod.WEEK -> getString(R.string.statistics_tab_week)
            StatisticsPeriod.MONTH -> getString(R.string.statistics_tab_month)
            StatisticsPeriod.YEAR -> getString(R.string.statistics_tab_year)
        }
    }

    private companion object {
        private const val COLLAPSED_CATEGORY_COUNT = 5
    }
}

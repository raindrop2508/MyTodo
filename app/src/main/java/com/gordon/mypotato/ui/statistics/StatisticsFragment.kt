package com.gordon.mypotato.ui.statistics

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentStatisticsBinding
import com.gordon.mypotato.databinding.ItemStatisticsTimelineSlotBinding
import com.google.android.material.button.MaterialButton

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding: FragmentStatisticsBinding
        get() = _binding!!

    private var currentMode: StatisticsMode = StatisticsMode.DAY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupModeToggle()
        renderMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvStatisticsTitle.text = getString(R.string.statistics_title)
        binding.tvStatisticsSubtitle.text = getString(R.string.statistics_subtitle)
    }

    private fun setupModeToggle() {
        binding.groupStatisticsMode.check(R.id.btn_mode_day)
        binding.groupStatisticsMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentMode = when (checkedId) {
                R.id.btn_mode_month -> StatisticsMode.MONTH
                R.id.btn_mode_year -> StatisticsMode.YEAR
                else -> StatisticsMode.DAY
            }
            renderMode()
        }
    }

    private fun renderMode() {
        renderModeButtons()
        when (currentMode) {
            StatisticsMode.DAY -> renderDayMode()
            StatisticsMode.MONTH -> renderChartMode(
                summary = SummaryStats(1860, 24),
                title = getString(R.string.statistics_month_chart_title),
                items = buildMonthlyChartItems()
            )
            StatisticsMode.YEAR -> renderChartMode(
                summary = SummaryStats(19480, 268),
                title = getString(R.string.statistics_year_chart_title),
                items = buildYearlyChartItems()
            )
        }
    }

    private fun renderDayMode() {
        renderSummary(SummaryStats(165, 4))
        binding.cardDailyTimeline.visibility = View.VISIBLE
        binding.cardChart.visibility = View.GONE
        binding.tvDailySectionTitle.text = getString(R.string.statistics_day_section_title)
        renderDailyTimeline(buildDailyTimelineItems())
    }

    private fun renderChartMode(summary: SummaryStats, title: String, items: List<StatisticsBarItem>) {
        renderSummary(summary)
        binding.cardDailyTimeline.visibility = View.GONE
        binding.cardChart.visibility = View.VISIBLE
        binding.tvChartTitle.text = title
        binding.viewStatisticsChart.submitData(items)
        renderLegend(items)
    }

    private fun renderSummary(summary: SummaryStats) {
        binding.tvFocusValue.text = summary.focusMinutes.toString()
        binding.tvCompletedValue.text = summary.completedCount.toString()
    }

    private fun renderModeButtons() {
        updateModeButton(binding.btnModeDay, currentMode == StatisticsMode.DAY)
        updateModeButton(binding.btnModeMonth, currentMode == StatisticsMode.MONTH)
        updateModeButton(binding.btnModeYear, currentMode == StatisticsMode.YEAR)
    }

    private fun updateModeButton(button: MaterialButton, selected: Boolean) {
        val selectedBackground = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val selectedForeground = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val defaultBackground = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        val defaultForeground = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val defaultStroke = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)

        button.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBackground else defaultBackground)
        button.strokeColor = ColorStateList.valueOf(if (selected) selectedBackground else defaultStroke)
        button.setTextColor(if (selected) selectedForeground else defaultForeground)
    }

    private fun renderDailyTimeline(items: List<DailyTimelineItem>) {
        binding.layoutDailyTimeline.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val itemBinding = ItemStatisticsTimelineSlotBinding.inflate(inflater, binding.layoutDailyTimeline, false)
            itemBinding.tvSlotTime.text = item.timeLabel
            if (item.isEmpty) {
                itemBinding.cardSlotTask.visibility = View.GONE
                itemBinding.viewSlotLine.visibility = View.VISIBLE
            } else {
                itemBinding.cardSlotTask.visibility = View.VISIBLE
                itemBinding.viewSlotLine.visibility = View.GONE
                itemBinding.tvSlotTaskTitle.text = item.taskTitle
                itemBinding.tvSlotCategory.text = item.categoryName
                itemBinding.viewCategoryColor.setBackgroundColor(ContextCompat.getColor(requireContext(), item.colorRes))
            }
            binding.layoutDailyTimeline.addView(itemBinding.root)
        }
    }

    private fun renderLegend(items: List<StatisticsBarItem>) {
        binding.layoutChartLegend.removeAllViews()
        val legendEntries = LinkedHashMap<String, Int>()
        items.flatMap { it.segments }.forEach { segment ->
            legendEntries.putIfAbsent(segment.name, segment.colorRes)
        }

        val entries = if (legendEntries.isEmpty()) {
            linkedMapOf(getString(R.string.statistics_single_series_label) to R.color.purple_500)
        } else {
            legendEntries
        }

        binding.tvLegendTitle.visibility = View.VISIBLE
        entries.forEach { (name, colorRes) ->
            binding.layoutChartLegend.addView(buildLegendRow(name, colorRes))
        }
    }

    private fun buildLegendRow(name: String, colorRes: Int): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val colorDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
            setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }

        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dp(8) }
            text = name
            setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }

        row.addView(colorDot)
        row.addView(textView)
        return row
    }

    private fun buildDailyTimelineItems(): List<DailyTimelineItem> {
        val categoryWork = getString(R.string.today_bottom_sheet_category_work)
        val categoryStudy = getString(R.string.today_bottom_sheet_category_study)
        val categoryLife = getString(R.string.today_bottom_sheet_category_life)
        val scheduled = mapOf(
            "08:00" to DailyTimelineItem("08:00", "晨间站会同步", categoryWork, R.color.task_category_work, false),
            "09:30" to DailyTimelineItem("09:30", "实现统计页时间轴", categoryStudy, R.color.task_category_study, false),
            "10:00" to DailyTimelineItem("10:00", "实现统计页时间轴", categoryStudy, R.color.task_category_study, false),
            "14:00" to DailyTimelineItem("14:00", "整理周任务清单", categoryWork, R.color.task_category_work, false),
            "18:30" to DailyTimelineItem("18:30", "晚间散步记录", categoryLife, R.color.task_category_life, false)
        )

        val result = mutableListOf<DailyTimelineItem>()
        for (hour in 0..23) {
            for (minute in listOf(0, 30)) {
                val label = String.format("%02d:%02d", hour, minute)
                result += scheduled[label]
                    ?: DailyTimelineItem(label, "", getString(R.string.statistics_timeline_empty_label), R.color.purple_500, true)
            }
        }
        return result
    }

    private fun buildMonthlyChartItems(): List<StatisticsBarItem> {
        val work = getString(R.string.today_bottom_sheet_category_work)
        val study = getString(R.string.today_bottom_sheet_category_study)
        val life = getString(R.string.today_bottom_sheet_category_life)
        val health = getString(R.string.today_bottom_sheet_category_health)
        val focus = getString(R.string.statistics_single_series_label)

        return (1..30).map { day ->
            val segments = mutableListOf<StatisticsBarSegment>()
            if (day % 6 == 0) {
                segments += StatisticsBarSegment(focus, 80 + (day % 3) * 20, R.color.purple_500)
            } else {
                if (day % 2 == 0) segments += StatisticsBarSegment(work, 50 + (day % 4) * 10, R.color.task_category_work)
                if (day % 3 == 0) segments += StatisticsBarSegment(study, 30 + (day % 5) * 8, R.color.task_category_study)
                if (day % 5 == 0) segments += StatisticsBarSegment(life, 20 + (day % 4) * 10, R.color.task_category_life)
                if (day % 7 == 0) segments += StatisticsBarSegment(health, 18 + (day % 3) * 8, R.color.task_category_health)
                if (segments.isEmpty()) {
                    segments += StatisticsBarSegment(focus, 40 + (day % 6) * 12, R.color.purple_500)
                }
            }
            StatisticsBarItem(label = day.toString(), segments = segments)
        }
    }

    private fun buildYearlyChartItems(): List<StatisticsBarItem> {
        val work = getString(R.string.today_bottom_sheet_category_work)
        val study = getString(R.string.today_bottom_sheet_category_study)
        val life = getString(R.string.today_bottom_sheet_category_life)
        val focus = getString(R.string.statistics_single_series_label)

        return (1..12).map { month ->
            val segments = mutableListOf<StatisticsBarSegment>()
            if (month % 4 == 0) {
                segments += StatisticsBarSegment(focus, 980 + month * 30, R.color.purple_500)
            } else {
                segments += StatisticsBarSegment(work, 420 + month * 26, R.color.task_category_work)
                if (month % 2 == 0) segments += StatisticsBarSegment(study, 210 + month * 18, R.color.task_category_study)
                if (month % 3 == 0) segments += StatisticsBarSegment(life, 160 + month * 16, R.color.task_category_life)
            }
            StatisticsBarItem(label = "${month}月", segments = segments)
        }
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val outValue = TypedValue()
        if (requireContext().theme.resolveAttribute(attrRes, outValue, true)) {
            return outValue.data
        }
        return ContextCompat.getColor(requireContext(), R.color.purple_500)
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private enum class StatisticsMode {
        DAY,
        MONTH,
        YEAR
    }

    private data class SummaryStats(
        val focusMinutes: Int,
        val completedCount: Int
    )

    private data class DailyTimelineItem(
        val timeLabel: String,
        val taskTitle: String,
        val categoryName: String,
        val colorRes: Int,
        val isEmpty: Boolean
    )
}


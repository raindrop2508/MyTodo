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
        setupModes()
        renderMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvStatisticsTitle.text = getString(R.string.statistics_title)
    }

    /**
     * 功能：绑定统计模式（时间、类别等）切换逻辑。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupModes() {
        binding.chipGroupTime.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_time_today
            val mode = when (checkedId) {
                R.id.chip_time_today -> StatisticsMode.DAY
                R.id.chip_time_week -> StatisticsMode.MONTH // Placeholder
                R.id.chip_time_month -> StatisticsMode.YEAR // Placeholder
                else -> StatisticsMode.DAY
            }
            if (currentMode != mode) {
                currentMode = mode
                renderMode()
            }
        }
    }

    private fun renderMode() {
        val focusMinutes = 165
        val completedCount = 12
        val ongoingCount = 5
        val rate = "48%"

        binding.tvStatCompleted.text = completedCount.toString()
        binding.tvStatOngoing.text = ongoingCount.toString()
        binding.tvStatRate.text = rate

        // Render Chart
        val colorOrange = R.color.state_chart_orange
        val chartData = when (currentMode) {
            StatisticsMode.DAY -> listOf(
                StatisticsBarItem("6时", listOf(StatisticsBarSegment("P1", 15, colorOrange))),
                StatisticsBarItem("9时", listOf(StatisticsBarSegment("P2", 45, colorOrange), StatisticsBarSegment("P3", 20, colorOrange))),
                StatisticsBarItem("12时", listOf(StatisticsBarSegment("P1", 30, colorOrange))),
                StatisticsBarItem("15时", listOf(StatisticsBarSegment("P3", 50, colorOrange), StatisticsBarSegment("P4", 10, colorOrange))),
                StatisticsBarItem("18时", listOf(StatisticsBarSegment("P1", 20, colorOrange), StatisticsBarSegment("P2", 20, colorOrange))),
                StatisticsBarItem("21时", listOf(StatisticsBarSegment("P4", 30, colorOrange)))
            )
            StatisticsMode.MONTH -> listOf(
                StatisticsBarItem("第1周", listOf(StatisticsBarSegment("P1", 120, colorOrange), StatisticsBarSegment("P2", 80, colorOrange), StatisticsBarSegment("P3", 60, colorOrange), StatisticsBarSegment("P4", 40, colorOrange))),
                StatisticsBarItem("第2周", listOf(StatisticsBarSegment("P1", 150, colorOrange), StatisticsBarSegment("P2", 60, colorOrange), StatisticsBarSegment("P3", 90, colorOrange), StatisticsBarSegment("P4", 30, colorOrange))),
                StatisticsBarItem("第3周", listOf(StatisticsBarSegment("P1", 100, colorOrange), StatisticsBarSegment("P2", 100, colorOrange), StatisticsBarSegment("P3", 50, colorOrange), StatisticsBarSegment("P4", 50, colorOrange))),
                StatisticsBarItem("第4周", listOf(StatisticsBarSegment("P1", 180, colorOrange), StatisticsBarSegment("P2", 40, colorOrange), StatisticsBarSegment("P3", 80, colorOrange), StatisticsBarSegment("P4", 20, colorOrange)))
            )
            StatisticsMode.YEAR -> listOf(
                StatisticsBarItem("一季度", listOf(StatisticsBarSegment("P1", 400, colorOrange), StatisticsBarSegment("P2", 300, colorOrange), StatisticsBarSegment("P3", 200, colorOrange), StatisticsBarSegment("P4", 100, colorOrange))),
                StatisticsBarItem("二季度", listOf(StatisticsBarSegment("P1", 500, colorOrange), StatisticsBarSegment("P2", 200, colorOrange), StatisticsBarSegment("P3", 300, colorOrange), StatisticsBarSegment("P4", 150, colorOrange))),
                StatisticsBarItem("三季度", listOf(StatisticsBarSegment("P1", 450, colorOrange), StatisticsBarSegment("P2", 250, colorOrange), StatisticsBarSegment("P3", 250, colorOrange), StatisticsBarSegment("P4", 120, colorOrange))),
                StatisticsBarItem("四季度", listOf(StatisticsBarSegment("P1", 600, colorOrange), StatisticsBarSegment("P2", 150, colorOrange), StatisticsBarSegment("P3", 350, colorOrange), StatisticsBarSegment("P4", 80, colorOrange)))
            )
        }
        
        binding.viewStatisticsChart.submitData(chartData)
    }

    private fun setupMockData() {
        // Prepare colors if needed
    }

    private enum class StatisticsMode {
        DAY,
        MONTH,
        YEAR
    }

    private companion object {
        private const val TAG = "StatisticsFragment"
    }
}


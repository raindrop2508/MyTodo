package com.gordon.mypotato.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.gordon.mypotato.R
import com.gordon.mypotato.domain.HeatmapMonthLabel

class StatisticsHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val cellRect = RectF()
    private var weeks: List<List<Int>> = emptyList()
    private var monthLabels: List<HeatmapMonthLabel> = emptyList()

    fun submit(weeks: List<List<Int>>, monthLabels: List<HeatmapMonthLabel>) {
        this.weeks = weeks
        this.monthLabels = monthLabels
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val metrics = gridMetrics()
        val columns = weeks.size.coerceAtLeast(1)
        val width = (paddingLeft + paddingRight + columns * metrics.cell + (columns - 1).coerceAtLeast(0) * metrics.gap).toInt()
        val height = (paddingTop + paddingBottom + metrics.labelHeight + DAY_COUNT * metrics.cell + (DAY_COUNT - 1) * metrics.gap).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (weeks.isEmpty()) return
        labelPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        labelPaint.textSize = sp(11f)
        val metrics = gridMetrics()
        val top = paddingTop + metrics.labelHeight

        monthLabels.forEach { month ->
            if (month.columnIndex !in weeks.indices) return@forEach
            val x = paddingLeft + month.columnIndex * (metrics.cell + metrics.gap)
            canvas.drawText(month.label, x, paddingTop + metrics.labelHeight - dp(4f), labelPaint)
        }

        weeks.forEachIndexed { column, days ->
            val left = paddingLeft + column * (metrics.cell + metrics.gap)
            for (row in 0 until DAY_COUNT) {
                val level = days.getOrElse(row) { 0 }.coerceIn(0, LEVEL_COUNT - 1)
                val cellTop = top + row * (metrics.cell + metrics.gap)
                cellRect.set(left, cellTop, left + metrics.cell, cellTop + metrics.cell)
                cellPaint.color = colorForLevel(context, level)
                canvas.drawRoundRect(cellRect, dp(2f), dp(2f), cellPaint)
            }
        }
    }

    private fun gridMetrics(): GridMetrics {
        return GridMetrics(cell = dp(12f), gap = dp(3f), labelHeight = dp(16f))
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val outValue = TypedValue()
        if (context.theme.resolveAttribute(attrRes, outValue, true)) {
            return outValue.data
        }
        return ContextCompat.getColor(context, R.color.ink_muted)
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }

    private data class GridMetrics(
        val cell: Float,
        val gap: Float,
        val labelHeight: Float,
    )

    companion object {
        const val LEVEL_COUNT = 5
        private const val DAY_COUNT = 7

        fun colorForLevel(context: Context, level: Int): Int {
            val empty = resolveAttr(context, com.google.android.material.R.attr.colorSurfaceVariant)
            val strong = ContextCompat.getColor(context, R.color.brand_indigo)
            val fraction = when (level.coerceIn(0, LEVEL_COUNT - 1)) {
                0 -> 0f
                1 -> 0.35f
                2 -> 0.55f
                3 -> 0.78f
                else -> 1f
            }
            return blend(empty, strong, fraction)
        }

        private fun resolveAttr(context: Context, attrRes: Int): Int {
            val outValue = TypedValue()
            if (context.theme.resolveAttribute(attrRes, outValue, true)) {
                return outValue.data
            }
            return ContextCompat.getColor(context, R.color.warm_surface_variant)
        }

        private fun blend(from: Int, to: Int, fraction: Float): Int {
            val inverse = 1f - fraction
            return Color.argb(
                255,
                (Color.red(from) * inverse + Color.red(to) * fraction).toInt(),
                (Color.green(from) * inverse + Color.green(to) * fraction).toInt(),
                (Color.blue(from) * inverse + Color.blue(to) * fraction).toInt(),
            )
        }
    }
}

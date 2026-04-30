package com.gordon.mypotato.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.gordon.mypotato.R
import kotlin.math.max

class StatisticsStackBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.25f)
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(11f)
    }

    private val items = mutableListOf<StatisticsBarItem>()

    fun submitData(newItems: List<StatisticsBarItem>) {
        items.clear()
        items.addAll(newItems)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateThemeColors()

        if (items.isEmpty()) return

        val chartLeft = paddingLeft.toFloat()
        val chartTop = paddingTop.toFloat() + dp(8f)
        val chartRight = width - paddingRight.toFloat()
        val chartBottom = height - paddingBottom.toFloat() - dp(26f)
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return

        drawGrid(canvas, chartLeft, chartTop, chartRight, chartBottom)

        val itemCount = items.size
        val gap = if (itemCount > 20) dp(4f) else dp(8f)
        val barWidth = ((chartWidth - gap * (itemCount - 1)) / itemCount).coerceAtLeast(dp(4f))
        val maxTotal = max(items.maxOfOrNull { item -> item.segments.sumOf { it.minutes } } ?: 0, 1)

        items.forEachIndexed { index, item ->
            val left = chartLeft + index * (barWidth + gap)
            val right = left + barWidth
            var currentBottom = chartBottom

            item.segments.forEach { segment ->
                if (segment.minutes <= 0) return@forEach
                val ratio = segment.minutes.toFloat() / maxTotal.toFloat()
                val segmentHeight = ratio * chartHeight
                val top = (currentBottom - segmentHeight).coerceAtLeast(chartTop)
                barPaint.color = ContextCompat.getColor(context, segment.colorRes)
                canvas.drawRoundRect(RectF(left, top, right, currentBottom), dp(4f), dp(4f), barPaint)
                currentBottom = top
            }

            if (shouldDrawLabel(index, itemCount)) {
                canvas.drawText(item.label, left + barWidth / 2f, height - paddingBottom.toFloat(), labelPaint)
            }
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val lines = 4
        repeat(lines + 1) { step ->
            val y = bottom - (bottom - top) * step / lines
            canvas.drawLine(left, y, right, y, if (step == 0) baselinePaint else gridPaint)
        }
    }

    private fun shouldDrawLabel(index: Int, total: Int): Boolean {
        val stride = when {
            total > 24 -> 4
            total > 14 -> 2
            else -> 1
        }
        return index % stride == 0 || index == total - 1
    }

    private fun updateThemeColors() {
        gridPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
        baselinePaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOutline)
        labelPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val outValue = TypedValue()
        if (context.theme.resolveAttribute(attrRes, outValue, true)) {
            return outValue.data
        }
        return ContextCompat.getColor(context, R.color.purple_500)
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }
}

data class StatisticsBarItem(
    val label: String,
    val segments: List<StatisticsBarSegment>
)

data class StatisticsBarSegment(
    val name: String,
    val minutes: Int,
    @ColorRes val colorRes: Int
)

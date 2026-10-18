package com.gordon.mypotato.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.gordon.mypotato.R

class StatisticsDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val arcRect = RectF()
    private var rate: Int = 0

    fun submitRate(value: Int) {
        rate = value.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = dp(10f)
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        trackPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorSurfaceVariant)
        progressPaint.color = ContextCompat.getColor(context, R.color.brand_indigo)
        textPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        textPaint.textSize = sp(22f)
        labelPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        labelPaint.textSize = sp(12f)

        val inset = stroke / 2f + dp(4f)
        arcRect.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
        canvas.drawArc(arcRect, -90f, 360f * rate / 100f, false, progressPaint)

        val centerX = width / 2f
        val valueBaseline = height / 2f - dp(2f)
        canvas.drawText("$rate%", centerX, valueBaseline, textPaint)
        canvas.drawText(context.getString(R.string.statistics_completion_rate), centerX, valueBaseline + sp(16f), labelPaint)
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val outValue = TypedValue()
        if (context.theme.resolveAttribute(attrRes, outValue, true)) {
            return outValue.data
        }
        return ContextCompat.getColor(context, R.color.brand_indigo)
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }
}

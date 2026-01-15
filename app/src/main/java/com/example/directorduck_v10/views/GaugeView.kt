package com.example.directorduck_v10.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(10f)
        color = 0xFFE5E7EB.toInt() // 浅灰
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(10f)
        color = 0xFF1E88E5.toInt() // 蓝
    }

    private val rect = RectF()

    // 仪表盘：从 135° 开始，扫过 270°（半圆偏下的仪表盘样式）
    private val startAngle = 135f
    private val sweepAngle = 270f

    private var value = 0f // 0~100

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 让它看起来更像仪表盘：宽大于高
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desiredH = (w * 0.6f).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = dp(14f)
        val w = width.toFloat()
        val h = height.toFloat()

        val size = min(w, h * 1.6f) // 保证弧形不挤
        val left = (w - size) / 2f + padding
        val top = padding
        val right = left + size - padding * 2
        val bottom = top + size - padding * 2

        rect.set(left, top, right, bottom)

        // 背景弧
        canvas.drawArc(rect, startAngle, sweepAngle, false, bgPaint)

        // 进度弧
        val progressSweep = sweepAngle * (value.coerceIn(0f, 100f) / 100f)
        canvas.drawArc(rect, startAngle, progressSweep, false, progressPaint)
    }

    fun setValueInstant(v: Int) {
        value = v.toFloat().coerceIn(0f, 100f)
        invalidate()
    }

    fun animateTo(v: Int, duration: Long = 900L) {
        val target = v.coerceIn(0, 100).toFloat()
        ValueAnimator.ofFloat(value, target).apply {
            this.duration = duration
            addUpdateListener {
                value = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}

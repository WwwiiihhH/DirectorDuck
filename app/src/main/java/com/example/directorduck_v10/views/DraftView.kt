package com.example.directorduck_v10.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DraftView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val paths = mutableListOf<Path>()
    private var currentPath: Path? = null

    // 让背景“灰色半透明”
    private val overlayColor = Color.parseColor("#66000000") // 40% 黑(偏灰)透明蒙层

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 画蒙层
        canvas.drawColor(overlayColor)

        // 画所有笔迹
        for (p in paths) canvas.drawPath(p, paint)
        currentPath?.let { canvas.drawPath(it, paint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(x, y) }
                invalidate()
                return true // ✅ 消费事件，拦截底层点击
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let { paths.add(it) }
                currentPath = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clear() {
        paths.clear()
        currentPath = null
        invalidate()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            invalidate()
        }
    }

    fun setStrokeWidth(px: Float) {
        paint.strokeWidth = px
        invalidate()
    }

    fun setStrokeColor(color: Int) {
        paint.color = color
        invalidate()
    }
}

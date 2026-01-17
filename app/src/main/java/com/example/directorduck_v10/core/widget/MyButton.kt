package com.example.directorduck_v10.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton

class MyButton : AppCompatButton {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        // 初始化时设置点击事件监听器
        setOnClickListener {
            playClickAnimation()
            // 可以在这里添加处理点击事件的逻辑
        }
    }

    private fun playClickAnimation() {
        // 添加点击时的动画效果
        animate().apply {
            scaleX(0.97f)
            scaleY(0.97f)
            duration = 30
        }.withEndAction {
            animate().apply {
                scaleX(1.0f)
                scaleY(1.0f)
                duration = 30
            }.start()
        }.start()
    }

    // 如果需要处理触摸事件的其他情况，可以重写 onTouchEvent 方法
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                playClickAnimation()
            }
        }
        return super.onTouchEvent(event)
    }
}
package com.example.directorduck_v10.defItems

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.directorduck_v10.R

class OptionItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var tvLabel: TextView
    private lateinit var tvText: TextView
    private var isSelectedState = false

    init {
        inflate(context, R.layout.item_option, this)

        tvLabel = findViewById(R.id.tvOptionLabel)
        tvText = findViewById(R.id.tvOptionText)

        // 设置点击监听器
        setOnClickListener {
            isSelectedState = !isSelectedState
            updateSelectionState()
            onOptionClicked?.invoke()
        }
    }

    var onOptionClicked: (() -> Unit)? = null

    fun setText(text: String) {
        tvText.text = text
    }

    override fun setSelected(selected: Boolean) {
        isSelectedState = selected
        updateSelectionState()
        super.setSelected(selected)
    }

    override fun isSelected(): Boolean {
        return isSelectedState
    }

    private fun updateSelectionState() {
        if (isSelectedState) {
            tvLabel.setTextColor(context.getColor(R.color.white))
            tvLabel.background = context.getDrawable(R.drawable.bg_option_circle_selected)
        } else {
            tvLabel.setTextColor(context.getColor(R.color.gray))
            tvLabel.background = context.getDrawable(R.drawable.bg_option_circle)
        }
    }
}
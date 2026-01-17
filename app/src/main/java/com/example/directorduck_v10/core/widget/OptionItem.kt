package com.example.directorduck_v10.core.widget

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
    lateinit var tvText: TextView

    private var allowToggle = true

    private var state = STATE_NORMAL

    init {
        inflate(context, R.layout.item_option, this)

        tvLabel = findViewById(R.id.tvOptionLabel)
        tvText = findViewById(R.id.tvOptionText)

        setOnClickListener {
            if (!allowToggle) return@setOnClickListener
            isSelected = !isSelected
            onOptionClicked?.invoke()
        }
    }

    var onOptionClicked: (() -> Unit)? = null

    fun setText(text: String) {
        tvText.text = text
    }

    fun setLabel(label: String) {
        tvLabel.text = label
    }

    fun disableToggle() {
        allowToggle = false
        isClickable = false
        isFocusable = false
        // 保证不会再被点击切换
    }

    fun setReviewState(newState: Int) {
        state = newState
        applyState()
    }

    override fun setSelected(selected: Boolean) {
        state = if (selected) STATE_SELECTED else STATE_NORMAL
        applyState()
        super.setSelected(selected)
    }

    override fun isSelected(): Boolean {
        return state == STATE_SELECTED
    }

    private fun applyState() {
        when (state) {
            STATE_SELECTED -> {
                tvLabel.setTextColor(context.getColor(R.color.white))
                tvLabel.background = context.getDrawable(R.drawable.bg_option_circle_selected)
                tvText.setTextColor(context.getColor(R.color.black))
            }
            STATE_CORRECT -> {
                tvLabel.setTextColor(context.getColor(R.color.white))
                tvLabel.background = context.getDrawable(R.drawable.bg_option_circle_correct)
//                tvText.setTextColor(context.getColor(R.color.))
            }
            STATE_WRONG -> {
                tvLabel.setTextColor(context.getColor(R.color.white))
                tvLabel.background = context.getDrawable(R.drawable.bg_option_circle_wrong)
//                tvText.setTextColor(context.getColor(R.color.wrong_red))
            }
            else -> {
                tvLabel.setTextColor(context.getColor(R.color.gray))
                tvLabel.background = context.getDrawable(R.drawable.bg_option_circle)
                tvText.setTextColor(context.getColor(R.color.black))
            }
        }
    }

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_SELECTED = 1
        const val STATE_CORRECT = 2
        const val STATE_WRONG = 3
    }
}

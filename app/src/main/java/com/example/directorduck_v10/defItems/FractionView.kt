package com.example.directorduck_v10.defItems

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.fragments.practice.compare.Fraction

class FractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvNum: TextView
    private val tvDen: TextView

    init {
        orientation = VERTICAL
        inflate(context, R.layout.view_fraction, this)
        tvNum = findViewById(R.id.tvNum)
        tvDen = findViewById(R.id.tvDen)
    }

    fun setFraction(f: Fraction) {
        tvNum.text = f.num.toString()
        tvDen.text = f.den.toString()
    }

    fun setFraction(num: Long, den: Long) {
        tvNum.text = num.toString()
        tvDen.text = den.toString()
    }
}

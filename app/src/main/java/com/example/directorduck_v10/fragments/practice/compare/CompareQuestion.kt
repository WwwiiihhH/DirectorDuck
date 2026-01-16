package com.example.directorduck_v10.fragments.practice.compare

import java.math.BigInteger

data class CompareQuestion(
    val left: Fraction,
    val right: Fraction
) {
    val correctSymbol: String
        get() = when (compareFractions(left, right)) {
            1 -> ">"
            -1 -> "<"
            else -> "?" // 理论上不会出现（我们生成时已避免相等）
        }

    companion object {
        /**
         * 比较 a/b 和 c/d：
         * 用 BigInteger 做交叉相乘，防止位数大时溢出
         */
        fun compareFractions(a: Fraction, b: Fraction): Int {
            val left = BigInteger.valueOf(a.num).multiply(BigInteger.valueOf(b.den))
            val right = BigInteger.valueOf(b.num).multiply(BigInteger.valueOf(a.den))
            return left.compareTo(right).coerceIn(-1, 1)
        }
    }
}

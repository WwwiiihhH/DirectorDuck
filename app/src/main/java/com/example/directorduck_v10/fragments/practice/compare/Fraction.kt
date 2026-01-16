package com.example.directorduck_v10.fragments.practice.compare

data class Fraction(
    val num: Long,
    val den: Long
) {
    init {
        require(den != 0L) { "denominator cannot be 0" }
    }
}

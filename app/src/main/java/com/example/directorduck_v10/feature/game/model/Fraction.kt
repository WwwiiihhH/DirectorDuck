package com.example.directorduck_v10.feature.game.model

data class Fraction(
    val num: Long,
    val den: Long
) {
    init {
        require(den != 0L) { "denominator cannot be 0" }
    }
}

package com.example.directorduck_v10.feature.game.model

import com.example.directorduck_v10.feature.game.model.Fraction

data class WrongEntry(
    val left: Fraction,
    val right: Fraction,
    val user: String,
    val correct: String
)

package com.example.directorduck_v10.core.network.dto.deepseek

data class QuestionSolveRequest(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val analysis: String? = null,
    val correctAnswer: String? = null,
    val userAnswer: String? = null
)

data class QuestionSolveData(
    val solution: String
)

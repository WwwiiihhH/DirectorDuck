package com.example.directorduck_v10.data.model

data class QuestionDetail(
    val uuid: String?,
    val questionText: String,
    val questionImage: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,
    val analysis: String?
)

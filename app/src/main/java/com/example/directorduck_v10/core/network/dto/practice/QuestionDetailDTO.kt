package com.example.directorduck_v10.core.network.dto.practice

data class QuestionDetailDTO(
    val id: Long,
    val uuid: String?,
    val questionText: String?,
    val questionImage: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,   // "A/B/C/D"
    val analysis: String?,
    val difficultyLevel: Int?     // 你后端是 Byte，这里用 Int 接就行
)

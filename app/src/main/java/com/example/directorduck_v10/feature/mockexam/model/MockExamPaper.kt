package com.example.directorduck_v10.feature.mockexam.model

data class MockExamPaper(
    val id: Long = 0,
    val sessionId: Long = 0,
    val totalQuestions: Int = 0,
    val generatedAt: String? = null
)

data class MockExamQuestionDTO(
    val uuid: String,
    val subcategoryId: Int = 0,
    val questionText: String = "",
    val questionImage: String? = null,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val moduleCode: String? = null,
    val orderIndex: Int = 0
)



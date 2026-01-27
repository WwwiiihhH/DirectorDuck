package com.example.directorduck_v10.feature.mockexam.model

data class MockExamSubmitRequest(
    val userId: Long,
    val username: String,
    val answers: List<String>   // ✅ 固定长度 130；未作答用 ""
)

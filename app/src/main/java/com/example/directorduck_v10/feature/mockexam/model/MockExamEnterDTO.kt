package com.example.directorduck_v10.feature.mockexam.model

data class MockExamEnterDTO(
    val sessionId: Long,
    val allowed: Boolean,
    val message: String,
    val serverNow: String,
    val startTime: String,
    val endTime: String,
    val remainSecondsToEnd: Long?
)

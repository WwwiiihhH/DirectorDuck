package com.example.directorduck_v10.feature.mockexam.model

data class MockExamStatusDTO(
    val sessionId: Long,
    val serverNow: String,
    val startTime: String,
    val endTime: String,
    val registerDeadline: String?,
    val status: Int,                 // 0未开始 1进行中 2已结束
    val joined: Boolean?,
    val canEnter: Boolean?,
    val remainSecondsToStart: Long?,
    val remainSecondsToEnd: Long?
)

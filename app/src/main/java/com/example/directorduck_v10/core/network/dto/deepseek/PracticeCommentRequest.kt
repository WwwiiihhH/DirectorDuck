package com.example.directorduck_v10.core.network.dto.deepseek

data class PracticeCommentRequest(
    val categoryName: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val correctRate: Int,
    val timeSpentSeconds: Long,
    val wrongUuids: List<String>,
    val topSlowQuestions: List<SlowQuestion>,

    val attemptStartEpoch: Long,
    val attemptEndEpoch: Long
)

data class SlowQuestion(
    val questionId: Long,
    val seconds: Long
)

// 适配你后端 Result<T>
data class ApiResult<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class PracticeCommentData(
    val comment: String
)

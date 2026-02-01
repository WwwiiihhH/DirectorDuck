package com.example.directorduck_v10.core.network.dto.deepseek

import java.io.Serializable

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
    val questionAttempts: List<QuestionAttempt> = emptyList(),

    val attemptStartEpoch: Long,
    val attemptEndEpoch: Long
)

data class SlowQuestion(
    val questionId: Long,
    val seconds: Long
)

data class QuestionAttempt(
    val questionId: Long,
    val uuid: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val userAnswer: String,
    val correctAnswer: String,
    val status: String
) : Serializable

// 适配你后端 Result<T>
data class ApiResult<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class PracticeCommentData(
    val comment: String
)

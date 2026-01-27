package com.example.directorduck_v10.feature.mockexam.model

import com.google.gson.annotations.SerializedName

data class ResultListMockExamWrongQuestionDTO(
    val code: Int,
    val message: String,
    val data: List<MockExamWrongQuestionDTO>?
)

data class MockExamWrongQuestionDTO(
    val orderIndex: Int,
    val moduleCode: String?,
    val questionText: String?,
    val questionImage: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    @SerializedName("userAnswer")
    val userAnswer: String?, // 用户填写的答案
    @SerializedName("correctAnswer")
    val correctAnswer: String?, // 正确答案
    val analysis: String? // 解析
)
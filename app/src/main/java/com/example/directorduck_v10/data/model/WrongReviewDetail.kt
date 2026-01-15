package com.example.directorduck_v10.data.model

/**
 * 题目展示需要的字段（题干/选项/图片/正确答案/解析）
 * 注意：correctAnswer/analysis 以 /answer/uuid 接口为准
 */
data class WrongReviewDetail(
    val questionText: String?,
    val questionImage: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,
    val analysis: String?
)
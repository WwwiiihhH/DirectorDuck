package com.example.directorduck_v10.feature.mockexam.model

import android.os.Parcelable // 必须导入
import kotlinx.parcelize.Parcelize // 必须导入

// 1. 添加注解
@Parcelize
data class MockExamResultDTO(
    val sessionId: Long,
    val userId: Long,
    val username: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val score: Double,
    val submittedAt: String? // 根据你的实际类型
) : Parcelable // 2. 实现接口
package com.example.directorduck_v10.feature.mockexam.model

import java.io.Serializable

data class MockExamSessionDTO(
    val id: Long,
    val title: String,
    val startTime: String,
    val endTime: String,
    val registerDeadline: String?,
    val durationMinutes: Int?,
    val status: Int?
) : Serializable {
    // 之前已有的字段
    var joined: Boolean = false
    var joinCount: Long = 0

    // ✅ 新增：标记该用户是否已完成作答（已交卷）
    var isCompleted: Boolean = false
}
package com.example.directorduck_v10.data.model

// 通用响应
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
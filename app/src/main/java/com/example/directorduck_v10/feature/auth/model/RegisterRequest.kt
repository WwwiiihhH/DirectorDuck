package com.example.directorduck_v10.feature.auth.model

// 注册请求
data class RegisterRequest(
    val username: String,
    val phone: String,
    val email: String,
    val password: String
)
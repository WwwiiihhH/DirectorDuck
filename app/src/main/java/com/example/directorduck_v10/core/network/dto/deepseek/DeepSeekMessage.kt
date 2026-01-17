package com.example.directorduck_v10.core.network.dto.deepseek

data class DeepSeekMessage(
    val role: String,   // "user" / "assistant" / "system"
    val content: String
)

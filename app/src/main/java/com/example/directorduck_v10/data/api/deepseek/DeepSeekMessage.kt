package com.example.directorduck_v10.data.api.deepseek

data class DeepSeekMessage(
    val role: String,   // "user" / "assistant" / "system"
    val content: String
)

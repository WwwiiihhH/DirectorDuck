package com.example.directorduck_v10.core.network.dto.deepseek

// ✅ 对应你后端 Result<T>
data class ResultWrapper<T>(
    val code: Int,
    val message: String,
    val data: T?
)

// ====== Android -> 你后端代理请求 ======
data class ProxyChatRequest(
    val model: String,              // "deepseek-chat" 或 "deepseek-reasoner"
    val question: String,
    val history: List<Message>? = null
)

data class ProxyChatResponse(
    val model: String,
    val answer: String,
    val raw: String? = null
)

// 多轮对话可用（先留着）
data class Message(
    val role: String,               // system/user/assistant
    val content: String
)

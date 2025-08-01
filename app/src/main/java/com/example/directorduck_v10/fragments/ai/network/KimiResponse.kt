package com.example.directorduck_v10.fragments.ai.network

data class KimiResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class Message(
    val role: String,
    val content: String
)

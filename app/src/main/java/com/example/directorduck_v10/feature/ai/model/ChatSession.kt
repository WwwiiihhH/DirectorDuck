package com.example.directorduck_v10.feature.ai.model

data class ChatSession(
    val id: String,
    var title: String,
    val messages: MutableList<ChatMessage>,
    var updatedAt: Long,
    val createdAt: Long
)

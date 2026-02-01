package com.example.directorduck_v10.feature.ai.data

import android.content.Context
import com.example.directorduck_v10.feature.ai.model.ChatSession
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class ChatHistoryStore(
    context: Context,
    userId: Long?
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "sessions_" + (userId?.toString() ?: "guest")

    fun load(): MutableList<ChatSession> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val list: List<ChatSession> = gson.fromJson(json, type) ?: emptyList()
            list.toMutableList()
        } catch (_: JsonSyntaxException) {
            mutableListOf()
        }
    }

    fun save(sessions: List<ChatSession>) {
        val json = gson.toJson(sessions)
        prefs.edit().putString(key, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "ai_chat_history"
    }
}

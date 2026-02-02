package com.example.directorduck_v10.core.state

import android.content.Context
import com.example.directorduck_v10.data.model.User
import com.google.gson.Gson

object UserSessionStore {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USER = "user_json"
    private val gson = Gson()

    fun save(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    fun load(context: Context): User? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER).apply()
    }
}

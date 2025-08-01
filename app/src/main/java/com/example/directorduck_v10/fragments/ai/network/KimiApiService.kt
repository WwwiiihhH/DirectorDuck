package com.example.directorduck_v10.fragments.ai.network

import android.util.Log
import com.example.directorduck_v10.fragments.ai.model.ChatMessage
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

object KimiApiService {

    private val client = OkHttpClient()

    // 修改为你 Spring Boot 的服务器地址和端口，例如本地调试就是 10.0.2.2:8080（AVD 模拟器）
//    private const val API_URL = "http://192.168.0.109:8080/api/kimi"
    private const val API_URL = "http://47.111.144.28:8080/api/kimi"

    fun askKimi(history: List<ChatMessage>, newQuestion: String, callback: (String?) -> Unit) {
        // 构造 JSON 请求体
        val requestBodyMap = mapOf(
            "history" to history,
            "question" to newQuestion
        )

        val json = Gson().toJson(requestBodyMap)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("KimiApiService", "请求失败：${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("KimiApiService", "响应失败，code=${response.code}")
                    callback(null)
                    return
                }

                val reply = response.body?.string()
                callback(reply)
            }
        })
    }
}

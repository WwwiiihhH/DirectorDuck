package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.Notice
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NoticeService {
    @GET("/api/notices")
    fun getAllNotices(): Call<ApiResponse<List<Notice>>>

    @GET("/api/notices/category/{category}")
    fun getNoticesByCategory(@Path("category") category: String): Call<ApiResponse<List<Notice>>>

    @GET("/api/notices/search")
    fun searchNotices(@Query("title") title: String): Call<ApiResponse<List<Notice>>>

    @GET("/api/notices/{id}")
    fun getNoticeById(@Path("id") id: Int): Call<ApiResponse<Notice>>
}
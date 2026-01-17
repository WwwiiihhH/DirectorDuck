package com.example.directorduck_v10.core.network.service

import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.auth.model.RegisterRequest
import com.example.directorduck_v10.data.model.User
import retrofit2.Call
import retrofit2.http.*

interface UserService {

    // 注册接口 - 发送 JSON，返回包含 User 的 ApiResponse
    @POST("/api/user/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<User>>


    // 登录接口 - 表单方式，返回包含 User 的 ApiResponse
    @FormUrlEncoded
    @POST("/api/user/login")
    fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): Call<ApiResponse<User>>
}


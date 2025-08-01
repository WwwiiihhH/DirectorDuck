package com.example.directorduck_v10.data.network

import com.example.directorduck_v10.data.api.UserService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
//    private const val BASE_URL = "http://192.168.0.109:8080"
    private const val BASE_URL = "http://47.111.144.28:8080"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val userService: UserService = retrofit.create(UserService::class.java)
}

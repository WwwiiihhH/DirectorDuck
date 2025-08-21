package com.example.directorduck_v10.data.network

import com.example.directorduck_v10.data.api.CommentService
import com.example.directorduck_v10.data.api.CourseService
import com.example.directorduck_v10.data.api.PostService
import com.example.directorduck_v10.data.api.PracticeService
import com.example.directorduck_v10.data.api.UserService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.0.108:8080"

//    private const val BASE_URL = "http://47.111.144.28:8080"


    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val userService: UserService = retrofit.create(UserService::class.java)

    val courseService: CourseService = retrofit.create(CourseService::class.java)

    val postService: PostService = retrofit.create(PostService::class.java)

    val commentService: CommentService = retrofit.create(CommentService::class.java)

    val practiceService: PracticeService = retrofit.create(PracticeService::class.java)
}

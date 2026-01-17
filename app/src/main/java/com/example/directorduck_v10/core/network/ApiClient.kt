package com.example.directorduck_v10.core.network

import com.example.directorduck_v10.core.network.service.CommentService
import com.example.directorduck_v10.core.network.service.CourseService
import com.example.directorduck_v10.core.network.service.DeepSeekService
import com.example.directorduck_v10.core.network.service.FavoriteService
import com.example.directorduck_v10.core.network.service.NoticeService
import com.example.directorduck_v10.core.network.service.PostService
import com.example.directorduck_v10.core.network.service.PracticeService
import com.example.directorduck_v10.core.network.service.UserService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // ✅ Retrofit 的 baseUrl 必须以 / 结尾
    private const val BASE_URL = "http://192.168.0.106:8080/"

    // 你也可以按需切换
    // private const val BASE_URL = "http://47.111.144.28:8080/"
    // private const val BASE_URL = "http://59.110.16.30:8080/"

    private const val TIMEOUT_SECONDS = 180L

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            // level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val userService: UserService by lazy { retrofit.create(UserService::class.java) }
    val courseService: CourseService by lazy { retrofit.create(CourseService::class.java) }
    val postService: PostService by lazy { retrofit.create(PostService::class.java) }
    val commentService: CommentService by lazy { retrofit.create(CommentService::class.java) }
    val practiceService: PracticeService by lazy { retrofit.create(PracticeService::class.java) }
    val noticeService: NoticeService by lazy { retrofit.create(NoticeService::class.java) }
    val deepSeekService: DeepSeekService by lazy { retrofit.create(DeepSeekService::class.java) }
    val favoriteService: FavoriteService by lazy { retrofit.create(FavoriteService::class.java) }

    /** 返回带 / 结尾的 baseUrl（给 Retrofit 用的） */
    fun getBaseUrl(): String = BASE_URL

    /** 返回不带 / 结尾的 host（适合你做 "$host${path}" 拼接） */
    fun getHostUrl(): String = BASE_URL.trimEnd('/')

    fun getQuizImageBaseUrl(): String = "${getBaseUrl()}quizuploads/"

    // ✅ 可选：如果你帖子图片都走 /images/ 或 /uploads/，也可以加一个统一的
    // fun getPostImageBaseUrl(): String = "${getHostUrl()}/images/"
}

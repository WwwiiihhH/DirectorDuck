package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.QuestionDetail
import com.example.directorduck_v10.fragments.practice.data.Category
import com.example.directorduck_v10.fragments.practice.data.Question
import com.example.directorduck_v10.fragments.practice.data.Subcategory
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PracticeService {
    @GET("/api/questions/categories")
    suspend fun getCategories(): Response<ApiResponse<List<Category>>>

    @GET("/api/questions/subcategories/{categoryId}")
    suspend fun getSubcategories(@Path("categoryId") categoryId: Int): Response<ApiResponse<List<Subcategory>>>

    // 添加随机获取题目的接口
    @POST("/api/questions/random")
    suspend fun getRandomQuestions(@Body request: RandomQuestionRequest): Response<ApiResponse<List<Question>>>
    // --- 添加获取题目答案的接口 ---
    @GET("/api/questions/answer/uuid/{uuid}") // 修改了路径
    suspend fun getQuestionAnswerByUuid(@Path("uuid") uuid: String): Response<ApiResponse<QuestionAnswerDto>>




    @GET("/api/questions/detail/uuid/{uuid}")
    suspend fun getQuestionDetailByUuid(
        @Path("uuid") uuid: String
    ): retrofit2.Response<ApiResponse<com.example.directorduck_v10.data.api.question.QuestionDetailDTO>>


}

// 随机题目请求数据类
data class RandomQuestionRequest(
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val count: Int
)

// --- 定义接收答案数据的DTO ---
data class QuestionAnswerDto(
    val uuid: String,
    val correctAnswer: String,
    val analysis: String,
    val difficultyLevel: Int
)
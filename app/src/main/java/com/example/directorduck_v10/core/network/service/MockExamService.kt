package com.example.directorduck_v10.core.network.service

import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.mockexam.model.*
import retrofit2.Response
import retrofit2.http.*

interface MockExamService {

    @GET("api/mock-exams")
    suspend fun listSessions(): Response<ApiResponse<List<MockExamSessionDTO>>>

    @POST("api/mock-exams/{sessionId}/join")
    suspend fun join(
        @Path("sessionId") sessionId: Long,
        @Body req: MockExamJoinRequest
    ): Response<ApiResponse<Any>>

    @GET("api/mock-exams/{sessionId}/join/exists")
    suspend fun exists(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long
    ): Response<ApiResponse<Boolean>>

    @GET("api/mock-exams/{sessionId}/participants/count")
    suspend fun countParticipants(
        @Path("sessionId") sessionId: Long
    ): Response<ApiResponse<Long>>

    @GET("api/mock-exams/{sessionId}/status")
    suspend fun status(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long? = null
    ): Response<ApiResponse<MockExamStatusDTO>>

    @POST("api/mock-exams/{sessionId}/enter")
    suspend fun enter(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long
    ): Response<ApiResponse<MockExamEnterDTO>>

    /** 生成本场试卷（幂等） */
    @POST("api/mock-exams/{sessionId}/paper/generate")
    suspend fun generatePaper(
        @Path("sessionId") sessionId: Long
    ): Response<ApiResponse<MockExamPaper>>

    /** 获取本场试卷题目（固定顺序返回） */
    @GET("api/mock-exams/{sessionId}/paper/questions")
    suspend fun getPaperQuestions(
        @Path("sessionId") sessionId: Long
    ): Response<ApiResponse<List<MockExamQuestionDTO>>>

    /** ✅ 交卷判分：answers 是 130 个字符串 */
    @POST("api/mock-exams/{sessionId}/submit")
    suspend fun submitPaper(
        @Path("sessionId") sessionId: Long,
        @Body body: MockExamSubmitRequest
    ): Response<ApiResponse<MockExamResultDTO>>

    @GET("/api/mock-exams/{sessionId}/wrong-questions")
    suspend fun getWrongQuestions(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long
    ): Response<ResultListMockExamWrongQuestionDTO>

    // ✅ 新增 1：查询用户是否已完成作答
    @GET("api/mock-exams/{sessionId}/is-completed")
    suspend fun checkCompletion(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long
    ): Response<ApiResponse<Boolean>>

    // ✅ 新增 2：查询本场成绩 (用于从列表页跳转结果页)
    @GET("api/mock-exams/{sessionId}/result")
    suspend fun getResult(
        @Path("sessionId") sessionId: Long,
        @Query("userId") userId: Long
    ): Response<ApiResponse<MockExamResultDTO>>

    // ✅ 新增：获取场次详情（复用 MockExamSessionDTO）
    @GET("api/mock-exams/{id}")
    suspend fun getSessionDetail(
        @Path("id") sessionId: Long
    ): Response<ApiResponse<MockExamSessionDTO>>
}

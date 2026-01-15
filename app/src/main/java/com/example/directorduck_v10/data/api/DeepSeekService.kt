package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.api.deepseek.ApiResult
import com.example.directorduck_v10.data.api.deepseek.PracticeCommentData
import com.example.directorduck_v10.data.api.deepseek.PracticeCommentRequest
import com.example.directorduck_v10.data.api.deepseek.ProxyChatRequest
import com.example.directorduck_v10.data.api.deepseek.ProxyChatResponse
import com.example.directorduck_v10.data.api.deepseek.ResultWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekService {

    // ⚠️ 如果你后端路径不是这个，改成你的实际路径
    @POST("/api/deepseek/chat")
    suspend fun chat(@Body req: ProxyChatRequest): Response<ResultWrapper<ProxyChatResponse>>

    @POST("api/deepseek/practice-comment")
    suspend fun practiceComment(@Body req: PracticeCommentRequest): retrofit2.Response<ApiResult<PracticeCommentData>>

}

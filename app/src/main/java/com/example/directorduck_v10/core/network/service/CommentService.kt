package com.example.directorduck_v10.core.network.service

import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.community.model.Comment
import retrofit2.Call
import retrofit2.http.*

interface CommentService {
    
    // 创建评论
    @POST("/api/comments/create")
    fun createComment(@Body commentRequest: CommentRequest): Call<ApiResponse<Comment>>
    
    // 获取帖子的所有评论
    @GET("/api/comments/post/{postId}")
    fun getCommentsByPostId(@Path("postId") postId: Long): Call<ApiResponse<List<Comment>>>
    
    // 获取帖子的评论数量
    @GET("/api/comments/post/{postId}/count")
    fun getCommentCountByPostId(@Path("postId") postId: Long): Call<ApiResponse<Long>>
}

// 评论请求数据类
data class CommentRequest(
    val content: String,
    val postId: Long,
    val userId: Long,
    val username: String
)
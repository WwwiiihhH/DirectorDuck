package com.example.directorduck_v10.core.network.service

import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.community.model.Post
import com.example.directorduck_v10.feature.community.model.PostResponse
import com.example.directorduck_v10.feature.community.model.PostDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface PostService {

    // 获取动态列表 - 使用PostResponse类型（旧接口，保持兼容）
    @GET("/api/posts/list")
    fun getAllPosts(): Call<ApiResponse<List<PostResponse>>>

    // 获取带点赞信息的动态列表 - 新接口
    @GET("/api/posts/list-with-likes")
    fun getAllPostsWithLikes(@Query("userId") userId: Long?): Call<ApiResponse<List<PostDTO>>>

    // 创建动态
    @Multipart
    @POST("/api/posts/create")
    fun createPost(
        @Part("content") content: RequestBody,
        @Part("publisherId") publisherId: RequestBody,
        @Part("publisherUsername") publisherUsername: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Call<Post>

    // 点赞/取消点赞
    @POST("/api/posts/{postId}/like")
    fun toggleLike(
        @Path("postId") postId: Long,
        @Query("userId") userId: Long
    ): Call<ApiResponse<String>>

    // 获取帖子点赞数
    @GET("/api/posts/{postId}/like-count")
    fun getPostLikeCount(@Path("postId") postId: Long): Call<ApiResponse<Long>>

    // 检查是否已点赞
    @GET("/api/posts/{postId}/is-liked")
    fun isUserLikedPost(
        @Path("postId") postId: Long,
        @Query("userId") userId: Long
    ): Call<ApiResponse<Boolean>>
}
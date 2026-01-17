package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.FavoritePageDTO
import com.example.directorduck_v10.data.model.FavoriteQuestionDetailDTO
import retrofit2.Response
import retrofit2.http.*
interface FavoriteService {

    @POST("api/favorites/{uuid}")
    suspend fun addFavorite(
        @Path("uuid") uuid: String,
        @Query("userId") userId: Long
    ): Response<ApiResponse<String>>

    @DELETE("api/favorites/{uuid}")
    suspend fun removeFavorite(
        @Path("uuid") uuid: String,
        @Query("userId") userId: Long
    ): Response<ApiResponse<String>>

    @GET("api/favorites/exists/{uuid}")
    suspend fun exists(
        @Path("uuid") uuid: String,
        @Query("userId") userId: Long
    ): Response<ApiResponse<Boolean>>

    // 收藏列表（分页）：GET /api/favorites?userId=1&page=1&size=20
    @GET("api/favorites")
    suspend fun listFavorites(
        @Query("userId") userId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<FavoritePageDTO<FavoriteQuestionDetailDTO>>>
}
package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.fragments.practice.data.Category
import com.example.directorduck_v10.fragments.practice.data.Subcategory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PracticeService {
    @GET("/api/questions/categories")
    suspend fun getCategories(): Response<ApiResponse<List<Category>>>

    @GET("/api/questions/subcategories/{categoryId}")
    suspend fun getSubcategories(@Path("categoryId") categoryId: Int): Response<ApiResponse<List<Subcategory>>>
}
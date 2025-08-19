package com.example.directorduck_v10.data.api

import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.Course
import retrofit2.Call
import retrofit2.http.GET

interface CourseService {
    @GET("/api/courses")
    fun getAllCourses(): Call<ApiResponse<List<Course>>>
}

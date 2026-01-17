package com.example.directorduck_v10.core.network.service

import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.course.model.Course
import retrofit2.Call
import retrofit2.http.GET

interface CourseService {
    @GET("/api/courses")
    fun getAllCourses(): Call<ApiResponse<List<Course>>>
}

package com.example.directorduck_v10.fragments.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.Course
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.FragmentCourseBinding
import com.example.directorduck_v10.fragments.course.adapters.CategoryAdapter
import com.example.directorduck_v10.fragments.course.adapters.CourseAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseFragment : Fragment() {

    private var _binding: FragmentCourseBinding? = null
    private val binding get() = _binding!!

    private val categories = listOf(0, 1, 2, 3, 4, 5, 6)
    private val categoryNames = mapOf(
        0 to "言语理解",
        1 to "数量关系",
        2 to "逻辑推理",
        3 to "资料分析",
        4 to "政治理论",
        5 to "常识",
        6 to "申论"
    )
    private var allCourses: List<Course> = listOf()
    private lateinit var courseAdapter: CourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourseBinding.inflate(inflater, container, false)

        setupCategoryRecyclerView()
        setupCourseRecyclerView(emptyList())
        fetchCoursesFromServer()

        return binding.root
    }

    private fun setupCategoryRecyclerView() {
        val categoryAdapter = CategoryAdapter(categories) { selectedCategory ->
            val filtered = allCourses.filter { it.category == selectedCategory }

            setupCourseRecyclerView(filtered)
        }
        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupCourseRecyclerView(courses: List<Course>) {
        courseAdapter = CourseAdapter(courses)
        binding.courseRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = courseAdapter
        }
    }

    private fun fetchCoursesFromServer() {
        ApiClient.courseService.getAllCourses().enqueue(object : Callback<ApiResponse<List<Course>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<Course>>>,
                response: Response<ApiResponse<List<Course>>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    allCourses = response.body()?.data ?: emptyList()
                    // 默认分类：言语理解
                    val defaultCourses = allCourses.filter { it.category == 0 }

                    setupCourseRecyclerView(defaultCourses)
                } else {
                    Toast.makeText(requireContext(), "课程获取失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Course>>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


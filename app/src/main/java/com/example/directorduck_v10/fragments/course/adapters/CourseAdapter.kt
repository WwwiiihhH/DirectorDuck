package com.example.directorduck_v10.fragments.course.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.CoursePlayerActivity
import com.example.directorduck_v10.data.model.Course
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ItemCourseBinding

class CourseAdapter(private val courses: List<Course>) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(course: Course) {
            binding.courseTitle.text = course.title
            binding.courseTeacher.text = course.teacher
            binding.courseDesc.text = course.description

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, CoursePlayerActivity::class.java)

                // 使用ApiClient中的基础URL
                val fullVideoUrl = if (course.videoUrl.startsWith("http")) {
                    course.videoUrl // 如果已经是完整URL，直接使用
                } else {
                    ApiClient.getBaseUrl() + course.videoUrl // 拼接基础URL
                }

                intent.putExtra("videoUrl", fullVideoUrl)
                Log.d("CourseAdapter", "Video URL: $fullVideoUrl")
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount() = courses.size
}
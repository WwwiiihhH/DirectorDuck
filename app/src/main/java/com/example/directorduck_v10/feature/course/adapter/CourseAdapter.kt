package com.example.directorduck_v10.feature.course.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R // 确保引用 R
import com.example.directorduck_v10.feature.course.ui.CoursePlayerActivity
import com.example.directorduck_v10.feature.course.model.Course
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ItemCourseBinding

class CourseAdapter(private val courses: List<Course>) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    private val categoryNames = mapOf(
        0 to "言语", 1 to "数量", 2 to "逻辑",
        3 to "资料", 4 to "政治", 5 to "常识", 6 to "申论"
    )

    inner class ViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(course: Course) {
            binding.courseTitle.text = course.title
            binding.courseTeacher.text = course.teacher
            binding.courseDesc.text = course.description

            // 设置分类标签
            binding.tvCategoryTag.text = categoryNames[course.category] ?: "课程"

            // --- 模拟封面图逻辑 (如果有真实图片URL最好) ---
            // 这里建议你准备几张不同的 drawable (icon_math, icon_logic 等)
            // 如果没有，就全部显示默认图
            binding.ivCover.setImageResource(R.drawable.student) // 替换为你项目里的默认图

            binding.root.setOnClickListener {
                // 点击逻辑保持不变...
                val context = binding.root.context
                val intent = Intent(context, CoursePlayerActivity::class.java)
                val fullVideoUrl = ApiClient.buildAbsoluteUrl(course.videoUrl)
                intent.putExtra("videoUrl", fullVideoUrl)
                context.startActivity(intent)
            }
        }
    }
    // ... 其他方法保持不变
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount() = courses.size
}

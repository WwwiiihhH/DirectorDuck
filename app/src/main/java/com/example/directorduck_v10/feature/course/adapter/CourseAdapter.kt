package com.example.directorduck_v10.feature.course.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // 引入 Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.directorduck_v10.R
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

            // --- 1. 获取资源 ID ---
            val coverResId = getCourseCover(course.title)

            // --- 2. 使用 Glide 异步加载图片 (解决卡顿的关键) ---
            Glide.with(binding.root.context)
                .load(coverResId)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(16))) // 可选：设置圆角和裁剪模式
                .placeholder(R.drawable.student) // 加载中显示的图
                .error(R.drawable.student)       // 加载失败显示的图
                .into(binding.ivCover)

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, CoursePlayerActivity::class.java)
                val fullVideoUrl = ApiClient.buildAbsoluteUrl(course.videoUrl)
                intent.putExtra("videoUrl", fullVideoUrl)
                context.startActivity(intent)
            }
        }
    }

    /**
     * 根据课程标题映射本地图片资源
     */
    private fun getCourseCover(title: String): Int {
        return when (title) {
            "中心理解专项拔高" -> R.drawable.zxlj1
            "主旨判断技巧精讲" -> R.drawable.zzpd1
            "成语填空高频考点" -> R.drawable.cytk
            "数学运算秒杀技巧" -> R.drawable.sxys
            "数字推理专项训练" -> R.drawable.sztl1
            "图形推理高频图式" -> R.drawable.txtl1
            "图表混合题解法解析" -> R.drawable.tbhh
            "时政热点串讲2025" -> R.drawable.zzll1
            "法律常识基础篇" -> R.drawable.flcs1
            "申论范文结构拆解" -> R.drawable.slfw1
            else -> R.drawable.student // 默认兜底图
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
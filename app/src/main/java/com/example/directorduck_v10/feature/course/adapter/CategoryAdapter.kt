package com.example.directorduck_v10.feature.course.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.databinding.ItemCourseCategoryBinding

class CategoryAdapter(
    private val categories: List<Int>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = 0

    private val categoryNames = mapOf(
        0 to "言语理解", 1 to "数量关系", 2 to "逻辑推理",
        3 to "资料分析", 4 to "政治理论", 5 to "常识", 6 to "申论"
    )

    inner class ViewHolder(val binding: ItemCourseCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Int, position: Int) {
            binding.categoryName.text = categoryNames[category] ?: "其他"

            // 核心修改：使用 View 的 selected 状态，让 XML 选择器自动处理颜色
            binding.categoryName.isSelected = (position == selectedPosition)

            binding.categoryName.setOnClickListener {
                val previousIndex = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedPosition)
                onClick(category)
            }
        }
    }
    // ... onCreateViewHolder 和 getItemCount 保持不变
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size
}
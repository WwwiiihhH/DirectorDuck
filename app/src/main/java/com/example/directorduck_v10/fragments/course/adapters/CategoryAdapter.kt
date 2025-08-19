package com.example.directorduck_v10.fragments.course.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R

import com.example.directorduck_v10.databinding.ItemCourseCategoryBinding

class CategoryAdapter(
    private val categories: List<Int>, // 改成 Int
    private val onClick: (Int) -> Unit // 传 Int 编号
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedIndex = 0
    private val categoryNames = mapOf(
        0 to "言语理解",
        1 to "数量关系",
        2 to "逻辑推理",
        3 to "资料分析",
        4 to "政治理论",
        5 to "常识",
        6 to "申论"
    )

    inner class ViewHolder(val binding: ItemCourseCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Int, position: Int) {
            binding.categoryName.text = categoryNames[category] ?: "未知分类"

            binding.categoryName.setBackgroundResource(
                if (position == selectedIndex) R.drawable.selected_background else R.drawable.default_background
            )

            binding.categoryName.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = position
                notifyItemChanged(previousIndex)
                notifyItemChanged(position)
                onClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount() = categories.size
}

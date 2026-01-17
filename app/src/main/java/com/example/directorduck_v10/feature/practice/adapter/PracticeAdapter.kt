package com.example.directorduck_v10.feature.practice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.practice.model.PracticeItem

class PracticeAdapter(
    private var items: List<PracticeItem> = emptyList(),
    private val onCategoryClick: (Int) -> Unit,  // 点击类别名称的回调
    private val onCategoryToggle: (Int) -> Unit, // 点击展开图标的回调
    private val onSubcategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_SUBCATEGORY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PracticeItem.CategoryItem -> TYPE_CATEGORY
            is PracticeItem.SubcategoryItem -> TYPE_SUBCATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }
            TYPE_SUBCATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_subcategory, parent, false)
                SubcategoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PracticeItem.CategoryItem -> {
                (holder as CategoryViewHolder).bind(item, onCategoryClick, onCategoryToggle)
            }
            is PracticeItem.SubcategoryItem -> {
                (holder as SubcategoryViewHolder).bind(item, onSubcategoryClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<PracticeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val expandIcon: ImageView = itemView.findViewById(R.id.ivExpandIcon)

        fun bind(
            item: PracticeItem.CategoryItem,
            onCategoryClick: (Int) -> Unit,
            onCategoryToggle: (Int) -> Unit
        ) {
            categoryName.text = item.category.categoryName

            // 设置展开/收起图标
            val iconRes = if (item.isExpanded) {
                R.drawable.ic_expand_less
            } else {
                R.drawable.ic_expand_more
            }
            expandIcon.setImageResource(iconRes)

            // 点击类别名称只触发回调，不控制展开/收起
            categoryName.setOnClickListener {
                onCategoryClick(item.category.id)
            }

            // 点击展开图标控制展开/收起
            expandIcon.setOnClickListener {
                onCategoryToggle(item.category.id)
            }
        }
    }

    inner class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subcategoryName: TextView = itemView.findViewById(R.id.tvSubcategoryName)

        fun bind(item: PracticeItem.SubcategoryItem, onClick: (Int) -> Unit) {
            subcategoryName.text = item.subcategory.subcategoryName
            itemView.setOnClickListener {
                onClick(item.subcategory.id)
            }
        }
    }
}
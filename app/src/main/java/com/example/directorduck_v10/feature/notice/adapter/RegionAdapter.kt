package com.example.directorduck_v10.feature.notice.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R // 确保导入正确的 R 文件

class RegionAdapter(
    private val regions: List<String>,
    private val onRegionSelected: (String) -> Unit
) : RecyclerView.Adapter<RegionAdapter.RegionViewHolder>() {

    // 默认选中第一个（全国）
    private var selectedPosition = 0

    inner class RegionViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region_filter, parent, false) as TextView
        return RegionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        val region = regions[position]
        holder.textView.text = region

        // 处理选中/未选中样式
        if (position == selectedPosition) {
            holder.textView.setBackgroundResource(R.drawable.bg_region_selected)
            holder.textView.setTextColor(Color.WHITE)
        } else {
            holder.textView.setBackgroundResource(R.drawable.bg_region_unselected)
            holder.textView.setTextColor(Color.parseColor("#757575"))
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // 刷新UI状态
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            // 回调点击事件
            onRegionSelected(region)
        }
    }

    override fun getItemCount(): Int = regions.size
}
package com.example.directorduck_v10.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R

class AnswerSheetAdapter(
    private val total: Int,
    private val isAnswered: (position: Int) -> Boolean,
    private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<AnswerSheetAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv) {
        init {
            tv.setOnClickListener {
                val pos = adapterPosition  // ✅ 老版本兼容
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_answer_number, parent, false) as TextView
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = (position + 1).toString()

        val answered = isAnswered(position)
        if (answered) {
            holder.tv.setBackgroundResource(R.drawable.bg_answer_num_answered)
            holder.tv.setTextColor(0xFFFFFFFF.toInt()) // 白字
        } else {
            holder.tv.setBackgroundResource(R.drawable.bg_answer_num_default)
            holder.tv.setTextColor(0xFF333333.toInt())
        }
    }

    override fun getItemCount(): Int = total

    fun refresh() {
        notifyDataSetChanged()
    }
}

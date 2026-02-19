package com.example.directorduck_v10.feature.game.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.databinding.ItemCompareWrongEntryBinding
import com.example.directorduck_v10.feature.game.model.WrongEntry

class CompareWrongAdapter(
    private val items: List<WrongEntry>
) : RecyclerView.Adapter<CompareWrongAdapter.VH>() {

    class VH(val b: ItemCompareWrongEntryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCompareWrongEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvExpr.text =
            "第${position + 1}题  ${item.left.num}/${item.left.den}  ?  ${item.right.num}/${item.right.den}"
        holder.b.tvDetail.text = "你的答案：${item.user}    正确答案：${item.correct}"
    }
}

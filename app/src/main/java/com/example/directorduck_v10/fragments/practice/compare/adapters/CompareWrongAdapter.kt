package com.example.directorduck_v10.fragments.practice.compare.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.databinding.ItemCompareWrongEntryBinding
import com.example.directorduck_v10.fragments.practice.compare.WrongEntry

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
        val it = items[position]
        holder.b.tvExpr.text = "${it.left.num}/${it.left.den}  ?  ${it.right.num}/${it.right.den}"
        holder.b.tvDetail.text = "你的：${it.user}    正确：${it.correct}"
    }
}

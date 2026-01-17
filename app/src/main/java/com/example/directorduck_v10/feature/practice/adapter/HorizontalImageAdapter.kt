package com.example.directorduck_v10.feature.practice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.practice.model.ImageItem

class HorizontalImageAdapter(
    private val items: List<ImageItem>,
    private val onClick: (ImageItem) -> Unit
) : RecyclerView.Adapter<HorizontalImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.img)
        val name: TextView = view.findViewById(R.id.name)

        init {
            view.setOnClickListener {
                onClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horizontal_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = items[position]
        holder.img.setImageResource(item.imageRes)
        holder.name.text = item.title
    }

    override fun getItemCount(): Int = items.size
}

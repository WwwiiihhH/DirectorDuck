package com.example.directorduck_v10.feature.favorite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.favorite.model.FavoriteQuestionDetailDTO

class FavoriteQuestionPagerAdapter(
    private val items: List<FavoriteQuestionDetailDTO>,
    private val getSelectedOption: (uuid: String) -> String?,
    private val onAnswered: (uuid: String, selected: String) -> Unit
) : RecyclerView.Adapter<FavoriteQuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteQuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_question, parent, false)
        return FavoriteQuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteQuestionViewHolder, position: Int) {
        val q = items[position]
        val saved = getSelectedOption(q.uuid ?: "")
        holder.bind(
            question = q,
            position = position,
            total = items.size,
            savedSelected = saved,
            onAnswered = { selected ->
                val uuid = q.uuid ?: return@bind
                onAnswered(uuid, selected)
            }
        )
    }

    override fun getItemCount(): Int = items.size
}
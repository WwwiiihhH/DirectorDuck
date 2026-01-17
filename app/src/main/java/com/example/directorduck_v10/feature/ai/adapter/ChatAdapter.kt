package com.example.directorduck_v10.feature.ai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.ai.model.ChatMessage

class ChatAdapter(
    private val data: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
            AiVH(v)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = data[position]
        if (holder is UserVH) holder.bind(msg)
        if (holder is AiVH) holder.bind(msg)
    }

    fun addMessage(msg: ChatMessage) {
        data.add(msg)
        notifyItemInserted(data.size - 1)
    }

    fun updateLastAiMessage(newText: String) {
        for (i in data.size - 1 downTo 0) {
            if (!data[i].isUser) {
                data[i] = data[i].copy(text = newText)
                notifyItemChanged(i)
                break
            }
        }
    }

    fun updateMessageAt(position: Int, newText: String) {
        if (position < 0 || position >= data.size) return
        val old = data[position]
        data[position] = old.copy(text = newText)
        notifyItemChanged(position)
    }

    class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvMessageUser)
        fun bind(m: ChatMessage) { tv.text = m.text }
    }

    class AiVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvMessageAi)
        fun bind(m: ChatMessage) { tv.text = m.text }
    }
}

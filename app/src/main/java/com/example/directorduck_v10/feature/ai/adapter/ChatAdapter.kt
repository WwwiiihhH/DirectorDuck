package com.example.directorduck_v10.feature.ai.adapter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.ai.model.ChatMessage

class ChatAdapter(
    private var data: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2

        fun buildBoldSpan(text: String): CharSequence {
            val sb = StringBuilder()
            val spans = mutableListOf<Pair<Int, Int>>()
            var i = 0
            while (i < text.length) {
                if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        val start = sb.length
                        sb.append(text.substring(i + 2, end))
                        val spanEnd = sb.length
                        spans.add(start to spanEnd)
                        i = end + 2
                        continue
                    }
                }
                sb.append(text[i])
                i++
            }
            val spannable = SpannableStringBuilder(sb.toString())
            for ((s, e) in spans) {
                if (s < e) {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        s,
                        e,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return spannable
        }
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

    fun setMessages(messages: MutableList<ChatMessage>) {
        data = messages
        notifyDataSetChanged()
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
        fun bind(m: ChatMessage) { tv.text = ChatAdapter.buildBoldSpan(m.text) }
    }
}

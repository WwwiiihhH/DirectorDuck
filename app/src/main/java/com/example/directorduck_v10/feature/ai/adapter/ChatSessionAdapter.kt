package com.example.directorduck_v10.feature.ai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.ai.model.ChatSession

class ChatSessionAdapter(
    private var sessions: MutableList<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.SessionVH>() {

    private var selectedId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_session, parent, false)
        return SessionVH(v)
    }

    override fun getItemCount(): Int = sessions.size

    override fun onBindViewHolder(holder: SessionVH, position: Int) {
        val item = sessions[position]
        holder.bind(item, item.id == selectedId, onClick)
    }

    fun submit(newSessions: List<ChatSession>) {
        sessions = newSessions.toMutableList()
        notifyDataSetChanged()
    }

    fun setSelected(id: String?) {
        selectedId = id
        notifyDataSetChanged()
    }

    class SessionVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSessionTitle)

        fun bind(
            session: ChatSession,
            selected: Boolean,
            onClick: (ChatSession) -> Unit
        ) {
            tvTitle.text = session.title.ifBlank { "新对话" }

            itemView.setBackgroundResource(
                if (selected) R.drawable.bg_session_item_selected else R.drawable.bg_session_item
            )

            itemView.setOnClickListener { onClick(session) }
        }
    }
}

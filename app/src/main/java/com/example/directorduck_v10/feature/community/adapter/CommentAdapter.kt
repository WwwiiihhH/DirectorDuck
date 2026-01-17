package com.example.directorduck_v10.feature.community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.feature.community.model.Comment
import com.example.directorduck_v10.databinding.ItemCommentBinding

class CommentAdapter(
    private val commentList: MutableList<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        with(holder.binding) {
            tvCommentUser.text = comment.username
            tvCommentTime.text = comment.formatTime()
            tvCommentContent.text = comment.content
        }
    }

    override fun getItemCount(): Int = commentList.size

    fun updateComments(newComments: List<Comment>) {
        commentList.clear()
        commentList.addAll(newComments)
        notifyDataSetChanged()
    }

    fun addComment(comment: Comment) {
        commentList.add(0, comment) // 添加到列表顶部
        notifyItemInserted(0)
    }
}
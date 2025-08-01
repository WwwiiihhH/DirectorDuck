package com.example.directorduck_v10.fragments.Information.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.PostDetailActivity
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.databinding.ItemForumPostBinding


class PostAdapter(private val postList: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemForumPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemForumPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        with(holder.binding) {
            tvUsername.text = post.username
            tvTime.text = post.time
            tvContent.text = post.content

            if (post.imageResId != null) {
                ivPostImage.setImageResource(post.imageResId)
                ivPostImage.visibility = View.VISIBLE
            } else {
                ivPostImage.visibility = View.GONE
            }

            // 评论绑定
            val commentViews = listOf(
                Triple(layoutComment1, tvComment1User, tvComment1Content),
                Triple(layoutComment2, tvComment2User, tvComment2Content),
                Triple(layoutComment3, tvComment3User, tvComment3Content)
            )

            for (i in commentViews.indices) {
                if (i < post.comments.size) {
                    val (layout, userView, contentView) = commentViews[i]
                    layout.visibility = View.VISIBLE
                    userView.text = "${post.comments[i].username}："
                    contentView.text = post.comments[i].content
                } else {
                    commentViews[i].first.visibility = View.GONE
                }
            }

            // 点击跳转详情
            root.setOnClickListener {
                val context = root.context
                val intent = Intent(context, PostDetailActivity::class.java)
                intent.putExtra("post", post)
                context.startActivity(intent)
            }


        }
    }


    override fun getItemCount(): Int = postList.size
}
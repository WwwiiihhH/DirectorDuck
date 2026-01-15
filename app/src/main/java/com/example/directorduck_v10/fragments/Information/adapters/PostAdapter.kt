package com.example.directorduck_v10.fragments.Information.adapters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ItemForumPostBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostAdapter(
    private val postList: MutableList<Post>,
    private val currentUserId: Long? = null,
    private val onLikeChanged: (() -> Unit)? = null,
    private val onPostClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemForumPostBinding) : RecyclerView.ViewHolder(binding.root)

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

            // 处理图片显示
            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.visibility = View.VISIBLE

                // ✅ 统一从 ApiClient 获取 host（不带末尾 /）
                val fullImageUrl = ApiClient.getHostUrl() + post.imageUrl!!

                Log.d("PostAdapter", "Loading image from: $fullImageUrl")

                Glide.with(holder.itemView.context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("PostAdapter", "Failed to load image: $fullImageUrl")
                            e?.logRootCauses("PostAdapter")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("PostAdapter", "Successfully loaded image: $fullImageUrl")
                            return false
                        }
                    })
                    .into(ivPostImage)
            } else {
                ivPostImage.visibility = View.GONE
            }

            updateLikeUI(post)
            showComments(post)

            ivLike.setOnClickListener {
                if (currentUserId != null) toggleLike(post, position, holder)
                else Toast.makeText(holder.itemView.context, "请先登录", Toast.LENGTH_SHORT).show()
            }

            root.setOnClickListener { onPostClick?.invoke(post) }
        }
    }

    private fun ItemForumPostBinding.showComments(post: Post) {
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

        if (post.comments.isEmpty()) {
            layoutComment1.visibility = View.GONE
            layoutComment2.visibility = View.GONE
            layoutComment3.visibility = View.GONE
        }
    }

    private fun ItemForumPostBinding.updateLikeUI(post: Post) {
        ivLike.setImageResource(if (post.isLiked) R.drawable.like_pressed else R.drawable.like)
    }

    private fun toggleLike(post: Post, position: Int, holder: PostViewHolder) {
        if (currentUserId == null) return
        holder.binding.ivLike.isEnabled = false

        ApiClient.postService.toggleLike(post.id, currentUserId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    holder.binding.ivLike.isEnabled = true
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            val newLikeState = !post.isLiked
                            val newLikeCount = if (newLikeState) post.likeCount + 1 else post.likeCount - 1

                            val updatedPost = post.copy(
                                isLiked = newLikeState,
                                likeCount = newLikeCount
                            )

                            postList[position] = updatedPost
                            notifyItemChanged(position)

                            onLikeChanged?.invoke()
                            Toast.makeText(
                                holder.itemView.context,
                                if (newLikeState) "点赞成功" else "取消点赞成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showError(holder, "操作失败: ${apiResponse?.message ?: "未知错误"}")
                        }
                    } else {
                        showError(holder, "网络请求失败: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    holder.binding.ivLike.isEnabled = true
                    showError(holder, "网络连接失败: ${t.message}")
                    Log.e("PostAdapter", "点赞操作失败", t)
                }
            })
    }

    private fun showError(holder: PostViewHolder, message: String) {
        Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newPosts: List<Post>) {
        postList.clear()
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun updatePost(position: Int, updatedPost: Post) {
        if (position in 0 until postList.size) {
            postList[position] = updatedPost
            notifyItemChanged(position)
        }
    }
}

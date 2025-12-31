package com.example.directorduck_v10.fragments.Information.adapters

import android.content.Intent
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
import com.example.directorduck_v10.PostDetailActivity
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ItemForumPostBinding
import android.graphics.drawable.Drawable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostAdapter(
    private val postList: MutableList<Post>,
    private val currentUserId: Long? = null,
    private val onLikeChanged: (() -> Unit)? = null,
    private val onPostClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    // 配置localhost服务器地址
    companion object {
        private const val BASE_URL = "http://192.168.0.106:8080"

//        // 修改为云服务器地址
//        private const val BASE_URL = "http://59.110.16.30:8080"
    }

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
                val fullImageUrl = "$BASE_URL${post.imageUrl}"

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

            // 更新点赞UI
            updateLikeUI(post)

            // 显示评论
            showComments(post)

            // 点赞按钮点击事件
            ivLike.setOnClickListener {
                if (currentUserId != null) {
                    toggleLike(post, position, holder)
                } else {
                    Toast.makeText(holder.itemView.context, "请先登录", Toast.LENGTH_SHORT).show()
                }
            }

            // 点击跳转详情
            root.setOnClickListener {
                onPostClick?.invoke(post)
            }
        }
    }


//    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
//        val post = postList[position]
//        with(holder.binding) {
//            tvUsername.text = post.username
//            tvTime.text = post.time
//            tvContent.text = post.content
//
//            // 处理图片显示
//            if (!post.imageUrl.isNullOrEmpty()) {
//                ivPostImage.visibility = View.VISIBLE
//
//                // --- 关键修改：智能处理图片 URL ---
//                val imageUrlToLoad = processImageUrl(post.imageUrl)
//
//                Log.d("PostAdapter", "Loading image from processed URL: $imageUrlToLoad")
//
//                Glide.with(holder.itemView.context)
//                    .load(imageUrlToLoad) // 使用处理后的 URL
//                    .placeholder(R.drawable.ic_launcher_background)
//                    .error(R.drawable.ic_launcher_foreground)
//                    .listener(object : RequestListener<Drawable> {
//                        override fun onLoadFailed(
//                            e: GlideException?,
//                            model: Any?,
//                            target: Target<Drawable>?,
//                            isFirstResource: Boolean
//                        ): Boolean {
//                            Log.e("PostAdapter", "Failed to load image from processed URL: $imageUrlToLoad")
//                            e?.logRootCauses("PostAdapter")
//                            return false // 返回 false 允许 error placeholder 显示
//                        }
//
//                        override fun onResourceReady(
//                            resource: Drawable?,
//                            model: Any?,
//                            target: Target<Drawable>?,
//                            dataSource: com.bumptech.glide.load.DataSource?,
//                            isFirstResource: Boolean
//                        ): Boolean {
//                            Log.d("PostAdapter", "Successfully loaded image from processed URL: $imageUrlToLoad")
//                            return false // 返回 false 允许 resource 显示
//                        }
//                    })
//                    .into(ivPostImage)
//            } else {
//                ivPostImage.visibility = View.GONE
//            }
//
//            // 更新点赞UI
//            updateLikeUI(post)
//
//            // 显示评论
//            showComments(post)
//
//            // 点赞按钮点击事件
//            ivLike.setOnClickListener {
//                if (currentUserId != null) {
//                    toggleLike(post, position, holder)
//                } else {
//                    Toast.makeText(holder.itemView.context, "请先登录", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            // 点击跳转详情
//            root.setOnClickListener {
//                onPostClick?.invoke(post)
//            }
//        }
//    }
//
//    // --- 新增方法：智能处理图片 URL ---
//    /**
//     * 根据后端返回的 imageUrl (可能是相对路径或完整 URL)，
//     * 生成 Glide 可以加载的正确 URL。
//     *
//     * @param imageUrl 后端返回的 imageUrl 字符串
//     * @return 可供 Glide 加载的完整 URL 字符串
//     */
//    private fun processImageUrl(imageUrl: String): String {
//        return when {
//            // 1. 如果 imageUrl 已经是完整 URL (以 http:// 或 https:// 开头)，直接使用
//            imageUrl.startsWith("http://", ignoreCase = true) ||
//                    imageUrl.startsWith("https://", ignoreCase = true) -> {
//                Log.d("PostAdapter", "Image URL is already full: $imageUrl")
//                imageUrl
//            }
//            // 2. 如果 imageUrl 是绝对路径 (以 / 开头)
//            imageUrl.startsWith("/") -> {
//                // 优先检查是否是 /images/ 路径
//                if (imageUrl.startsWith("/images/", ignoreCase = true)) {
//                    // 确保使用 /images/ 路径
//                    "$BASE_URL$imageUrl"
//                } else if (imageUrl.startsWith("/uploads/", ignoreCase = true)) {
//                    // 如果是 /uploads/ 路径，替换为 /images/ 路径
//                    val correctedUrl = imageUrl.replaceFirst("/uploads/", "/images/", ignoreCase = true)
//                    Log.d("PostAdapter", "Corrected uploads path '$imageUrl' to '$correctedUrl'")
//                    "$BASE_URL$correctedUrl"
//                } else {
//                    // 其他绝对路径，直接拼接
//                    "$BASE_URL$imageUrl"
//                }
//            }
//            // 3. 其他情况 (相对路径，不以 / 开头)
//            else -> {
//                // 为了安全，给它加上 /images/ 前缀
//                "$BASE_URL/images/$imageUrl"
//            }
//        }
//    }


    private fun ItemForumPostBinding.showComments(post: Post) {
        // 评论显示
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

        // 如果没有评论，隐藏所有评论布局
        if (post.comments.isEmpty()) {
            layoutComment1.visibility = View.GONE
            layoutComment2.visibility = View.GONE
            layoutComment3.visibility = View.GONE
        }
    }

    private fun ItemForumPostBinding.updateLikeUI(post: Post) {
        // 更新点赞按钮状态
        if (post.isLiked) {
            ivLike.setImageResource(R.drawable.like_pressed)
        } else {
            ivLike.setImageResource(R.drawable.like)
        }
    }

    private fun toggleLike(post: Post, position: Int, holder: PostViewHolder) {
        if (currentUserId == null) return
        holder.binding.ivLike.isEnabled = false

        ApiClient.postService.toggleLike(post.id, currentUserId).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                holder.binding.ivLike.isEnabled = true
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val newLikeState = !post.isLiked
                        val newLikeCount = if (newLikeState) post.likeCount + 1 else post.likeCount - 1

                        // 创建更新后的帖子对象
                        val updatedPost = post.copy(
                            isLiked = newLikeState,
                            likeCount = newLikeCount
                        )

                        // 更新数据源
                        postList[position] = updatedPost

                        // 通知适配器刷新此位置
                        notifyItemChanged(position)

                        onLikeChanged?.invoke()
                        val message = if (newLikeState) "点赞成功" else "取消点赞成功"
                        Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
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

    // 更新整个列表数据的方法
    fun updatePosts(newPosts: List<Post>) {
        postList.clear()
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    // 更新单个帖子的方法
    fun updatePost(position: Int, updatedPost: Post) {
        if (position in 0 until postList.size) {
            postList[position] = updatedPost
            notifyItemChanged(position)
        }
    }
}
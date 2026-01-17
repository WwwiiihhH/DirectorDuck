package com.example.directorduck_v10.feature.community.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.network.service.CommentRequest
import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.community.model.Comment
import com.example.directorduck_v10.feature.community.model.Post
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityPostDetailBinding
import com.example.directorduck_v10.feature.community.adapter.CommentAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var currentPost: Post? = null
    private var currentUserId: Long? = null
    private var currentUsername: String? = null
    private lateinit var commentAdapter: CommentAdapter
    private val comments = mutableListOf<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递的Post数据和用户ID
        currentPost = intent.getParcelableExtra<Post>("post")
        currentUserId = intent.getLongExtra("userId", -1)
        currentUsername = intent.getStringExtra("username")

        if (currentUserId == -1L) currentUserId = null

        if (currentPost != null) {
            setupPostDetails(currentPost!!)
            setupRecyclerView()
            loadComments()
        } else {
            finish()
        }

        setupClickListeners()
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(comments)
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentAdapter
        }
    }

    private fun setupPostDetails(post: Post) {
        with(binding) {
            // 设置发布者信息
            tvUsername.text = post.username

            // 设置发布时间
            tvTime.text = post.time

            // 设置帖子内容
            tvContent.text = post.content

            // 处理图片显示
            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.visibility = View.VISIBLE

                val fullImageUrl = ApiClient.getHostUrl() + post.imageUrl

                Glide.with(this@PostDetailActivity)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivPostImage)
            } else {
                ivPostImage.visibility = View.GONE
            }


            // 设置点赞状态和数量
            updateLikeUI(post.isLiked)
            tvLikeCount.text = post.likeCount.toString()
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            // 返回时传递更新后的Post数据
            currentPost?.let { post ->
                intent.putExtra("updatedPost", post)
                setResult(RESULT_OK, intent)
            }
            finish()
        }

        binding.ivLike.setOnClickListener {
            if (currentUserId != null) {
                toggleLike()
            } else {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSend.setOnClickListener {
            sendComment()
        }
    }

    private fun toggleLike() {
        val post = currentPost ?: return
        if (currentUserId == null) return

        // 禁用按钮防止重复点击
        binding.ivLike.isEnabled = false

        ApiClient.postService.toggleLike(post.id, currentUserId!!).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                binding.ivLike.isEnabled = true

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val newLikeState = !post.isLiked
                        val newLikeCount = if (newLikeState) post.likeCount + 1 else post.likeCount - 1

                        // 更新当前帖子数据
                        currentPost = post.copy(
                            isLiked = newLikeState,
                            likeCount = newLikeCount
                        )

                        // 更新UI
                        updateLikeUI(newLikeState)
                        binding.tvLikeCount.text = newLikeCount.toString()

                        val message = if (newLikeState) "点赞成功" else "取消点赞成功"
                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                    } else {
                        showError("操作失败: ${apiResponse?.message ?: "未知错误"}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                binding.ivLike.isEnabled = true
                showError("网络连接失败: ${t.message}")
                Log.e("PostDetailActivity", "点赞操作失败", t)
            }
        })
    }

    private fun loadComments() {
        val post = currentPost ?: return

        ApiClient.commentService.getCommentsByPostId(post.id).enqueue(object : Callback<ApiResponse<List<Comment>>> {
            override fun onResponse(call: Call<ApiResponse<List<Comment>>>, response: Response<ApiResponse<List<Comment>>>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val commentList = apiResponse.data ?: emptyList()
                        updateCommentsList(commentList)   // ✅ 只更新列表，不做占位
                    } else {
                        showError("获取评论失败: ${apiResponse?.message ?: "未知错误"}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Comment>>>, t: Throwable) {
                showError("网络连接失败: ${t.message}")
                Log.e("PostDetailActivity", "获取评论失败", t)
            }
        })
    }


    private fun sendComment() {
        val post = currentPost ?: return
        val content = binding.etComment.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == null || currentUsername.isNullOrEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val commentRequest = CommentRequest(
            content = content,
            postId = post.id,
            userId = currentUserId!!,
            username = currentUsername!!
        )

        // 禁用发送按钮防止重复点击
        binding.btnSend.isEnabled = false

        ApiClient.commentService.createComment(commentRequest).enqueue(object : Callback<ApiResponse<Comment>> {
            override fun onResponse(call: Call<ApiResponse<Comment>>, response: Response<ApiResponse<Comment>>) {
                binding.btnSend.isEnabled = true

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val newComment = apiResponse.data
                        if (newComment != null) {
                            // 添加新评论到列表
                            commentAdapter.addComment(newComment)
                            // 清空输入框
                            binding.etComment.setText("")


                            Toast.makeText(this@PostDetailActivity, "评论成功", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showError("评论失败: ${apiResponse?.message ?: "未知错误"}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Comment>>, t: Throwable) {
                binding.btnSend.isEnabled = true
                showError("网络连接失败: ${t.message}")
                Log.e("PostDetailActivity", "发送评论失败", t)
            }
        })
    }

    private fun updateCommentsList(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        commentAdapter.notifyDataSetChanged()
    }



    private fun updateLikeUI(isLiked: Boolean) {
        if (isLiked) {
            binding.ivLike.setImageResource(R.drawable.like_pressed)
        } else {
            binding.ivLike.setImageResource(R.drawable.like)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        // 返回时传递更新后的Post数据
        currentPost?.let { post ->
            intent.putExtra("updatedPost", post)
            setResult(RESULT_OK, intent)
        }
        super.onBackPressed()
    }
}
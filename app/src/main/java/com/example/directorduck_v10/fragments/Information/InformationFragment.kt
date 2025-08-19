package com.example.directorduck_v10.fragments.Information

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.EditPostActivity
import com.example.directorduck_v10.PostDetailActivity
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.data.model.PostResponse
import com.example.directorduck_v10.data.model.PostDTO
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.FragmentInformationBinding
import com.example.directorduck_v10.fragments.Information.adapters.PostAdapter
import com.example.directorduck_v10.viewmodel.SharedUserViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InformationFragment : Fragment() {
    private var _binding: FragmentInformationBinding? = null
    private val binding get() = _binding!!
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()

    private val postLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("PostLauncher", "返回结果: resultCode=${result.resultCode}")
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Log.d("PostLauncher", "调用 loadPosts() 刷新列表")
            loadPosts()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInformationBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupClickListeners()
        loadPosts()

        return binding.root
    }

    // 修改setupRecyclerView方法中的onPostClick回调
    private fun setupRecyclerView() {
        val currentUser = sharedUserViewModel.user.value
        val currentUserId = currentUser?.id
        val currentUsername = currentUser?.username

        postAdapter = PostAdapter(
            postList = posts,
            currentUserId = currentUserId,
            onLikeChanged = {
                Log.d("InformationFragment", "点赞状态已改变")
            },
            onPostClick = { post ->
                // 处理帖子点击事件
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra("post", post)
                intent.putExtra("userId", currentUserId ?: -1L)
                intent.putExtra("username", currentUsername ?: "")
                postDetailLauncher.launch(intent)
            }
        )

        binding.rvPost.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupClickListeners() {
        binding.PostButton.setOnClickListener {
            Log.d("PostButton", "按钮被点击")
            val user = sharedUserViewModel.user.value
            Log.d("PostButton", "user=$user")
            if (user != null) {
                val intent = Intent(requireContext(), EditPostActivity::class.java)
                intent.putExtra("user", user)
                Log.d("PostButton", "准备启动 EditPostActivity")
                try {
                    postLauncher.launch(intent)
                    Log.d("PostButton", "启动 EditPostActivity 成功")
                } catch (e: Exception) {
                    Log.e("PostButton", "启动 EditPostActivity 失败", e)
                }
            } else {
                Log.e("PostButton", "用户信息未加载，user 为 null")
                Toast.makeText(requireContext(), "用户信息未加载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPosts() {
        showLoading(true)

        val currentUserId = sharedUserViewModel.user.value?.id

        // 使用新的带点赞信息和评论信息的API
        ApiClient.postService.getAllPostsWithLikes(currentUserId).enqueue(object : Callback<ApiResponse<List<PostDTO>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<PostDTO>>>,
                response: Response<ApiResponse<List<PostDTO>>>
            ) {
                showLoading(false)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val postDTOList = apiResponse.data ?: emptyList()
                        val displayPosts = postDTOList.map { it.toDisplayPost() }
                        updatePostsList(displayPosts)
                        Log.d("InformationFragment", "成功加载 ${displayPosts.size} 条动态（含点赞和评论信息）")
                    } else {
                        showError("获取动态失败: ${apiResponse?.message ?: "未知错误"}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PostDTO>>>, t: Throwable) {
                showLoading(false)
                showError("网络连接失败: ${t.message}")
                Log.e("InformationFragment", "加载动态失败", t)
            }
        })
    }

    // 备用方法：使用旧API加载帖子（不含点赞信息）
    private fun loadPostsWithoutLikeInfo() {
        ApiClient.postService.getAllPosts().enqueue(object : Callback<ApiResponse<List<PostResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<PostResponse>>>,
                response: Response<ApiResponse<List<PostResponse>>>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        val postResponseList = apiResponse.data ?: emptyList()
                        val displayPosts = postResponseList.map { it.toDisplayPost() }
                        updatePostsList(displayPosts)
                        Log.d("InformationFragment", "使用旧API成功加载 ${displayPosts.size} 条动态")
                    } else {
                        showError("获取动态失败: ${apiResponse?.message ?: "未知错误"}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PostResponse>>>, t: Throwable) {
                showError("网络连接失败: ${t.message}")
                Log.e("InformationFragment", "加载动态失败", t)
            }
        })
    }

    private fun updatePostsList(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts.sortedByDescending { it.time })
        postAdapter.notifyDataSetChanged()
    }

    // 在 InformationFragment 类中添加新的启动器
    private val postDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val updatedPost = result.data?.getParcelableExtra<Post>("updatedPost")
            updatedPost?.let { post ->
                // 找到对应的帖子位置并更新
                val position = posts.indexOfFirst { it.id == post.id }
                if (position != -1) {
                    posts[position] = post
                    postAdapter.notifyItemChanged(position)
                    Log.d("InformationFragment", "更新了帖子 ${post.id} 的点赞状态")
                }
            }
        }
    }


    private fun showLoading(isLoading: Boolean) {
        // 如果有加载指示器，在这里控制显示/隐藏
        // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
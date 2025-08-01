package com.example.directorduck_v10.fragments.Information


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.model.Comment
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.databinding.FragmentInformationBinding
import com.example.directorduck_v10.fragments.Information.adapters.PostAdapter


class InformationFragment : Fragment() {
    private var _binding: FragmentInformationBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInformationBinding.inflate(inflater, container, false)

        setupRecyclerView()

        return binding.root
    }


    private fun setupRecyclerView() {
        val samplePosts = listOf(
            Post(
                username = "小鹤鹤蛋",
                time = "2025.7.24",
                content = "#备考经验我分享# 行测学习总结为三点...",
                imageResId = R.drawable.postexample,
                comments = listOf(
                    Comment("用户1", "谢谢分享！"),
                    Comment("用户2", "我也这么觉得。"),
                    Comment("用户3", "受益匪浅。")
                )
            ),
            Post(
                username = "无图用户",
                time = "2025.7.20",
                content = "这是一个没有图片的帖子，测试一下布局效果。",
                imageResId = null,
                comments = listOf(
                    Comment("评论者A", "图呢？"),
                    Comment("评论者B", "说得好")
                )
            ),
            Post(
                username = "纯文字用户",
                time = "2025.7.21",
                content = "只有文字，没有评论",
                imageResId = null,
                comments = emptyList()
            ),Post(
                username = "小鹤鹤蛋",
                time = "2025.7.24",
                content = "#备考经验我分享# 行测学习总结为三点...",
                imageResId = R.drawable.postexample,
                comments = listOf(
                    Comment("用户1", "谢谢分享！"),
                    Comment("用户2", "我也这么觉得。"),
                    Comment("用户3", "受益匪浅。")
                )
            ),
            Post(
                username = "无图用户",
                time = "2025.7.20",
                content = "这是一个没有图片的帖子，测试一下布局效果。",
                imageResId = null,
                comments = listOf(
                    Comment("评论者A", "图呢？"),
                    Comment("评论者B", "说得好")
                )
            ),
            Post(
                username = "纯文字用户",
                time = "2025.7.21",
                content = "只有文字，没有评论",
                imageResId = null,
                comments = emptyList()
            ),Post(
                username = "小鹤鹤蛋",
                time = "2025.7.24",
                content = "#备考经验我分享# 行测学习总结为三点...",
                imageResId = R.drawable.postexample,
                comments = listOf(
                    Comment("用户1", "谢谢分享！"),
                    Comment("用户2", "我也这么觉得。"),
                    Comment("用户3", "受益匪浅。")
                )
            ),
            Post(
                username = "无图用户",
                time = "2025.7.20",
                content = "这是一个没有图片的帖子，测试一下布局效果。",
                imageResId = null,
                comments = listOf(
                    Comment("评论者A", "图呢？"),
                    Comment("评论者B", "说得好")
                )
            ),
            Post(
                username = "纯文字用户",
                time = "2025.7.21",
                content = "只有文字，没有评论",
                imageResId = null,
                comments = emptyList()
            )
        )

        postAdapter = PostAdapter(samplePosts)
        binding.rvPost.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
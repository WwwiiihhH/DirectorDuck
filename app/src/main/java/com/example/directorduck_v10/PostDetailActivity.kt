package com.example.directorduck_v10

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.databinding.ActivityPostDetailBinding

class PostDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityPostDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val post = intent.getParcelableExtra<Post>("post")
        post?.let {
            binding.tvUsername.text = it.username
            binding.tvTime.text = it.time
            binding.tvContent.text = it.content

            if (it.imageResId != null) {
                binding.ivImage.setImageResource(it.imageResId)
                binding.ivImage.visibility = android.view.View.VISIBLE
            } else {
                binding.ivImage.visibility = android.view.View.GONE
            }
        }

        binding.ivBack.setOnClickListener { finish() }
    }
}
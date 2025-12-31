package com.example.directorduck_v10

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.adapters.NoticeAdapter
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.Notice
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityNoticeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoticeActivity : BaseActivity() {

    private lateinit var binding: ActivityNoticeBinding
    private lateinit var noticeAdapter: NoticeAdapter
    private val notices = mutableListOf<Notice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadNotices()
    }

    private fun setupRecyclerView() {
        noticeAdapter = NoticeAdapter(notices) { notice ->
            // 点击公告项的处理逻辑
            val intent = Intent(this, NoticeDetailActivity::class.java)
            intent.putExtra("notice", notice)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NoticeActivity)
            adapter = noticeAdapter
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadNotices() {
        showLoading(true)

        ApiClient.noticeService.getAllNotices().enqueue(object : Callback<ApiResponse<List<Notice>>> {
            override fun onResponse(call: Call<ApiResponse<List<Notice>>>, response: Response<ApiResponse<List<Notice>>>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val noticeList = apiResponse.data ?: emptyList()
                        noticeAdapter.updateNotices(noticeList)
                    } else {
                        showError("获取公告失败: ${apiResponse?.message}")
                    }
                } else {
                    showError("网络请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Notice>>>, t: Throwable) {
                showLoading(false)
                showError("网络连接失败: ${t.message}")
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
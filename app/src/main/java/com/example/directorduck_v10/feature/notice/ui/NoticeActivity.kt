package com.example.directorduck_v10.feature.notice.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.feature.notice.adapter.NoticeAdapter
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiResponse
import com.example.directorduck_v10.feature.notice.model.Notice
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityNoticeBinding
import com.example.directorduck_v10.feature.notice.adapter.RegionAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoticeActivity : BaseActivity() {

    private lateinit var binding: ActivityNoticeBinding
    private lateinit var noticeAdapter: NoticeAdapter
    private val notices = mutableListOf<Notice>()
    private lateinit var regionAdapter: RegionAdapter

    // 关键：保留一份完整的原始数据，用于反复筛选
    private val allNotices = mutableListOf<Notice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRegionFilter() // 新增
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

                        // 关键：保存到原始列表
                        allNotices.clear()
                        allNotices.addAll(noticeList)

                        // 默认显示全部
                        noticeAdapter.updateNotices(allNotices)

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


    // 1. 设置地区筛选器
    private fun setupRegionFilter() {
        // 定义省份列表，"全国"放在第一个
        val provinces = listOf(
            "全国", "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
            "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北",
            "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州", "云南", "西藏",
            "陕西", "甘肃", "青海", "宁夏", "新疆"
        )

        regionAdapter = RegionAdapter(provinces) { selectedRegion ->
            // 点击回调：执行筛选
            filterNoticesByRegion(selectedRegion)
        }

        binding.rvRegionFilter.apply {
            layoutManager = LinearLayoutManager(this@NoticeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = regionAdapter
        }
    }

    // 2. 实现筛选逻辑
    private fun filterNoticesByRegion(region: String) {
        if (allNotices.isEmpty()) return

        val filteredList = if (region == "全国") {
            // 如果选的是全国，显示所有数据
            allNotices
        } else {
            // 否则，根据标题是否包含省份名称进行筛选
            // 假设 Notice 的 title 包含省份名，例如 "2025年广东省公务员..."
            allNotices.filter { notice ->
                notice.title.contains(region) || notice.content.contains(region)
            }
        }

        // 更新列表显示
        noticeAdapter.updateNotices(filteredList)

        // 如果筛选后没有数据，可以考虑显示一个空状态视图 (Optional)
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "暂无${region}相关公告", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
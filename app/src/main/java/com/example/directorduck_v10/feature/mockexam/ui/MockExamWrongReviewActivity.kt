package com.example.directorduck_v10.feature.mockexam.ui

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.isOk
import com.example.directorduck_v10.databinding.ActivityWrongReviewBinding // ✅ 复用练习模块的布局
import com.example.directorduck_v10.feature.mockexam.adapter.MockExamWrongReviewAdapter
import com.example.directorduck_v10.feature.mockexam.model.MockExamWrongQuestionDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MockExamWrongReviewActivity : BaseActivity() {

    private lateinit var binding: ActivityWrongReviewBinding

    private var sessionId: Long = -1
    private var userId: Long = -1

    private val wrongList = mutableListOf<MockExamWrongQuestionDTO>()
    private lateinit var adapter: MockExamWrongReviewAdapter

    // 简单的加载框
    private var loadingDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ 复用布局文件
        binding = ActivityWrongReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        userId = intent.getLongExtra(EXTRA_USER_ID, -1)

        if (sessionId == -1L || userId == -1L) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 修改一下标题
        binding.tvTopTitle.text = "模考错题回顾"

        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // 初始化 Adapter
        adapter = MockExamWrongReviewAdapter(wrongList)
        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // 初始页码
        binding.tvIndicator.text = "0/0"

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (wrongList.isNotEmpty()) {
                    binding.tvIndicator.text = "${position + 1}/${wrongList.size}"
                }
            }
        })
    }

    private fun loadData() {
        showLoading("正在获取错题本...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.mockExamService.getWrongQuestions(sessionId, userId)
                }

                hideLoading()

                if (!response.isSuccessful) {
                    Toast.makeText(this@MockExamWrongReviewActivity, "获取失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val body = response.body()

                // 🔴 修改点：这里不能用 .isOk()，直接判断 code 是否为 200 (或 0，视你后端约定)
                // 假设成功状态码是 200
                if (body == null || body.code != 200) {
                    Toast.makeText(this@MockExamWrongReviewActivity, body?.message ?: "无数据", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val list = body.data
                if (list.isNullOrEmpty()) {
                    Toast.makeText(this@MockExamWrongReviewActivity, "本次模考没有错题！🎉", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                wrongList.clear()
                wrongList.addAll(list)
                adapter.notifyDataSetChanged()

                binding.tvIndicator.text = "1/${wrongList.size}"

            } catch (e: Exception) {
                hideLoading()
                Toast.makeText(this@MockExamWrongReviewActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showLoading(msg: String) {
        if (loadingDialog == null) {
            loadingDialog = ProgressDialog(this)
            loadingDialog?.setCancelable(false)
        }
        loadingDialog?.setMessage(msg)
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    companion object {
        private const val EXTRA_SESSION_ID = "sessionId"
        private const val EXTRA_USER_ID = "userId"

        fun start(context: Context, sessionId: Long, userId: Long) {
            val intent = Intent(context, MockExamWrongReviewActivity::class.java)
            intent.putExtra(EXTRA_SESSION_ID, sessionId)
            intent.putExtra(EXTRA_USER_ID, userId)
            context.startActivity(intent)
        }
    }
}
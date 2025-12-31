package com.example.directorduck_v10

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.directorduck_v10.data.model.Notice
import com.example.directorduck_v10.databinding.ActivityNoticeDetailBinding

class NoticeDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityNoticeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递过来的Notice数据
        val notice = intent.getParcelableExtra<Notice>("notice")

        if (notice != null) {
            setupNoticeDetails(notice)
        } else {
            Toast.makeText(this, "公告数据为空", Toast.LENGTH_SHORT).show()
            finish() // 关闭页面
        }

        setupClickListeners()
    }

    private fun setupNoticeDetails(notice: Notice) {
        with(binding) {
            // 设置标题信息
            tvTitle.text = notice.title
            label1.text = notice.category
            label2.text = notice.publishTime.substring(0, 10).replace("-", ".") // 格式化时间

            // 设置时间卡片信息
            tvRecruitCount.text = "招聘 ${notice.recruitCount} 人"
            tvPositionCount.text = "${notice.positionCount} 个岗位"
            tvApplyTimeValue.text = notice.applyTime
            tvPaymentTimeValue.text = notice.paymentTime
            tvAdmitCardValue.text = notice.admitCardTime
            tvExamTimeValue.text = notice.examTime

            // 设置公告内容
            tvNoticeContent.text = notice.content


        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}
package com.example.directorduck_v10.feature.mockexam.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.databinding.ActivityMockExamResultBinding
import com.example.directorduck_v10.feature.mockexam.model.MockExamResultDTO
import java.util.Locale
import java.util.concurrent.TimeUnit

class MockExamResultActivity : BaseActivity() {

    private lateinit var binding: ActivityMockExamResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockExamResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionTitle = intent.getStringExtra(EXTRA_SESSION_TITLE).orEmpty()
        val elapsedMillis = intent.getLongExtra(EXTRA_ELAPSED_MILLIS, 0L)
        val result: MockExamResultDTO? = intent.getParcelableExtra(EXTRA_RESULT)

        setupClicks()

        if (result == null) {
            Toast.makeText(this, "数据加载错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayData(result, sessionTitle, elapsedMillis)
    }

    private fun setupClicks() {
        binding.ivBack.setOnClickListener { finish() }

        // ✅ 点击“待提升”卡片 (红色卡片)，跳转错题回顾
        // 假设待提升卡片外层是 cardIncorrect 或者你包裹了一个 View
        // 如果你的 XML 里没有给红色卡片加 ID，你需要给它加一个 ID，比如 android:id="@+id/cardIncorrect"

        // 找到你在 XML 里定义的红色待提升卡片
        val cardIncorrect = findViewById<androidx.cardview.widget.CardView>(R.id.cardIncorrect) // 或者是 binding.cardIncorrect

        // 也可以直接给 binding 设置，如果 ViewBinding 正常的话
        // binding.cardIncorrect.setOnClickListener { ... }

        // 由于你的布局里用的是 binding，建议直接用 binding：
        binding.cardIncorrect?.setOnClickListener {
            // 从 intent 中获取必要参数
            // 注意：ResultDTO 中包含 userId, sessionId
            val result: MockExamResultDTO? = intent.getParcelableExtra("result")

            if (result != null) {
                // 如果错题数 > 0 才跳转
                val wrongCount = result.totalQuestions - result.correctCount
                if (wrongCount > 0) {
                    MockExamWrongReviewActivity.start(
                        this,
                        result.sessionId,
                        result.userId
                    )
                } else {
                    Toast.makeText(this, "太棒了，全对！没有错题 🎉", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "数据异常，无法查看错题", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayData(result: MockExamResultDTO, title: String, elapsedMillis: Long) {
        // 1. 基础信息
        binding.tvSessionTitle.text = if (title.isNotBlank()) title else "模考结果"
        val cleanTime = result.submittedAt?.replace("T", " ") ?: "--"
        binding.tvSubmitTime.text = "交卷时间：$cleanTime"

        val timeStr = formatMmSs(elapsedMillis)
        binding.tvTimeSpent.text = "答题耗时: $timeStr"

        // 2. 底部小卡片数据
        // 卡片1：正确数
        binding.tvCorrectCount.text = result.correctCount.toString()

        // 卡片2：待提升 (总数 - 正确数)
        val incorrectOrUnanswered = result.totalQuestions - result.correctCount
        binding.tvIncorrectCount.text = incorrectOrUnanswered.toString()

        // 卡片3：正确率 (以前是得分，现在因为得分放大了，这里放正确率)
        val rateInt = if (result.totalQuestions > 0) {
            ((result.correctCount.toDouble() / result.totalQuestions) * 100).toInt()
        } else {
            0
        }
        binding.tvCorrectRateSmall.text = "$rateInt%"

        // 3. 核心：仪表盘显示【得分】
        // 假设满分是 100（如果是行测可能 100，申论 100），这里为了仪表盘好看，指针按 0-100 的比例转动
        // 如果你的分数可能超过 100 (例如 150)，你需要调整 gaugeView 的 max 逻辑，或者归一化到 100
        val gaugeValue = if (result.score > 100) 100 else result.score.toInt()

        // 启动得分动画
        animateScore(gaugeValue, result.score)
    }

    private fun animateScore(gaugeTarget: Int, finalScore: Double) {
        // 1. 仪表盘指针动画 (0 ~ 100 整数)
        binding.gaugeView.setValueInstant(0)
        binding.gaugeView.animateTo(gaugeTarget, 1200L)

        // 2. 大数字文本动画 (0.0 ~ finalScore 浮点数)
        val animator = ValueAnimator.ofFloat(0f, finalScore.toFloat())
        animator.duration = 1200L
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            // 格式化为 1 位小数，例如 "78.5"
            binding.tvScoreBig.text = String.format(Locale.getDefault(), "%.1f", value)
        }
        animator.start()
    }

    private fun formatMmSs(ms: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    companion object {
        private const val EXTRA_RESULT = "result"
        private const val EXTRA_ELAPSED_MILLIS = "elapsedMillis"
        private const val EXTRA_SESSION_TITLE = "sessionTitle"

        fun start(context: Context, result: MockExamResultDTO, elapsedMillis: Long, sessionTitle: String) {
            val it = Intent(context, MockExamResultActivity::class.java)
            it.putExtra(EXTRA_RESULT, result)
            it.putExtra(EXTRA_ELAPSED_MILLIS, elapsedMillis)
            it.putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            context.startActivity(it)
        }
    }
}
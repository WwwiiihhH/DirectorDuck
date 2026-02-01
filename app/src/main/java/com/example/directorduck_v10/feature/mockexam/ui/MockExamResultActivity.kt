package com.example.directorduck_v10.feature.mockexam.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.dto.deepseek.PracticeCommentRequest
import com.example.directorduck_v10.databinding.ActivityMockExamResultBinding
import com.example.directorduck_v10.feature.mockexam.model.MockExamResultDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

class MockExamResultActivity : BaseActivity() {

    private lateinit var binding: ActivityMockExamResultBinding
    private var skeletonAnimators: List<ObjectAnimator> = emptyList()

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
        fetchAiComment(result, sessionTitle, elapsedMillis)
    }

    private fun setupClicks() {
        binding.ivBack.setOnClickListener { finish() }

        // 点击“待提升”卡片（红色卡片），跳转错题回顾页
        // 如果 XML 没有给红色卡片加 ID，需要先添加，例如 android:id="@+id/cardIncorrect"
        // 找到 XML 里定义的红色“待提升”卡片
        val cardIncorrect = findViewById<androidx.cardview.widget.CardView>(R.id.cardIncorrect) // 或者是 binding.cardIncorrect

        // 也可以直接用 binding 设置（如果 ViewBinding 有对应字段）
        // binding.cardIncorrect.setOnClickListener { ... }

        // 由于布局使用了 binding，建议直接用 binding
        binding.cardIncorrect?.setOnClickListener {
            // 从 intent 中获取必要参数
            // 注意：ResultDTO 包含 userId、sessionId
            val result: MockExamResultDTO? = intent.getParcelableExtra("result")

            if (result != null) {
                // 只有错题数 > 0 才跳转
                val wrongCount = result.totalQuestions - result.correctCount
                if (wrongCount > 0) {
                    MockExamWrongReviewActivity.start(
                        this,
                        result.sessionId,
                        result.userId
                    )
                } else {
                    Toast.makeText(this, "太棒了，全对！没有错题🎉", Toast.LENGTH_SHORT).show()
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
        binding.tvTimeSpent.text = "答题耗时：$timeStr"

        // 2. 底部小卡片数据
        // 卡片1：正确数
        binding.tvCorrectCount.text = result.correctCount.toString()

        // 卡片2：待提升（总数 - 正确数）
        val incorrectOrUnanswered = result.totalQuestions - result.correctCount
        binding.tvIncorrectCount.text = incorrectOrUnanswered.toString()

        // 卡片3：正确率
        val rateInt = if (result.totalQuestions > 0) {
            ((result.correctCount.toDouble() / result.totalQuestions) * 100).toInt()
        } else {
            0
        }
        binding.tvCorrectRateSmall.text = "$rateInt%"

        // 3. 核心：仪表盘显示【得分】
        // 如果得分可能超过 100，需要调整 gaugeView 的 max 或归一化到 100
        val gaugeValue = if (result.score > 100) 100 else result.score.toInt()

        // 启动得分动画
        animateScore(gaugeValue, result.score)
    }

    private fun fetchAiComment(result: MockExamResultDTO, sessionTitle: String, elapsedMillis: Long) {
        startAiCommentLoading()

        val totalQuestions = result.totalQuestions
        val correctCount = result.correctCount
        val incorrectCount = (totalQuestions - correctCount).coerceAtLeast(0)
        val unansweredCount = 0
        val correctRate = if (totalQuestions > 0) {
            ((correctCount.toDouble() / totalQuestions) * 100).toInt()
        } else 0

        val categoryName = if (sessionTitle.isNotBlank()) "模考-${sessionTitle}" else "模考"

        val req = PracticeCommentRequest(
            categoryName = categoryName,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            correctRate = correctRate,
            timeSpentSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis),
            wrongUuids = emptyList(),
            topSlowQuestions = emptyList(),
            questionAttempts = emptyList(),
            attemptStartEpoch = 0L,
            attemptEndEpoch = 0L
        )

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.deepSeekService.practiceComment(req)
                }

                if (resp.isSuccessful) {
                    val body = resp.body()
                    val comment = if (body?.code == 200 && body.data != null) {
                        body.data.comment
                    } else {
                        "AI 点评生成失败：${body?.message ?: "未知错误"}"
                    }
                    showAiComment(comment)
                } else {
                    showAiComment("服务器错误：HTTP ${resp.code()}")
                }
            } catch (e: Exception) {
                showAiComment("网络异常：${e.message ?: "unknown"}")
            }
        }
    }

    private fun startAiCommentLoading() {
        binding.pbAiCommentLoading.visibility = View.VISIBLE
        binding.llAiSkeleton.visibility = View.VISIBLE
        binding.tvAiComment.visibility = View.GONE

        val lines = listOf(
            binding.skeletonLine1,
            binding.skeletonLine2,
            binding.skeletonLine3,
            binding.skeletonLine4
        )

        skeletonAnimators.forEach { it.cancel() }

        skeletonAnimators = lines.mapIndexed { index, v ->
            ObjectAnimator.ofFloat(v, "alpha", 0.35f, 1.0f, 0.35f).apply {
                duration = 900L
                startDelay = (index * 120).toLong()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun stopAiCommentLoading() {
        skeletonAnimators.forEach { it.cancel() }
        skeletonAnimators = emptyList()

        binding.pbAiCommentLoading.visibility = View.GONE
        binding.llAiSkeleton.visibility = View.GONE
    }

    private fun showAiComment(text: String) {
        stopAiCommentLoading()
        binding.tvAiComment.visibility = View.VISIBLE
        val pretty = formatAiComment(text)
        binding.tvAiComment.text = buildBoldSpannable(pretty)
    }

    private fun formatAiComment(text: String): String {
        val normalized = text.replace("\r\n", "\n").trim()
        val lines = normalized.split("\n")
        val cleaned = lines
            .filterNot { it.trim().startsWith("NEXT_ACTION") }
            .map { line ->
                when {
                    line.startsWith("- ") -> line.substring(2).trimStart()
                    line.startsWith("• ") -> line.substring(2).trimStart()
                    else -> line
                }
            }
        return cleaned.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun buildBoldSpannable(text: String): CharSequence {
        val sb = SpannableStringBuilder()
        var index = 0
        while (index < text.length) {
            val start = text.indexOf("**", index)
            if (start < 0) {
                sb.append(text.substring(index))
                break
            }
            val end = text.indexOf("**", start + 2)
            if (end < 0) {
                sb.append(text.substring(index))
                break
            }
            sb.append(text.substring(index, start))
            val boldStart = sb.length
            sb.append(text.substring(start + 2, end))
            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                boldStart,
                sb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            index = end + 2
        }
        return sb
    }

    private fun animateScore(gaugeTarget: Int, finalScore: Double) {
        // 1. 仪表盘指针动画（0 ~ 100 整数）
        binding.gaugeView.setValueInstant(0)
        binding.gaugeView.animateTo(gaugeTarget, 1200L)

        // 2. 大数字文本动画（0.0 ~ finalScore 浮点数）
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

    override fun onDestroy() {
        stopAiCommentLoading()
        super.onDestroy()
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

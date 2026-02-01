package com.example.directorduck_v10.feature.practice.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.directorduck_v10.core.network.dto.deepseek.PracticeCommentRequest
import com.example.directorduck_v10.core.network.dto.deepseek.QuestionAttempt
import com.example.directorduck_v10.core.network.dto.deepseek.SlowQuestion
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.databinding.ActivityResultBinding
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var skeletonAnimators: List<ObjectAnimator> = emptyList()

    // ✅ 作为成员变量，供点击事件使用
    private var wrongUuids: ArrayList<String> = arrayListOf()
    private var wrongUserAnswers: ArrayList<String> = arrayListOf()
    private var currentUser: User? = null
    private var nextAction: NextAction? = null

    data class NextAction(
        val categoryId: Int,
        val subcategoryId: Int?,
        val count: Int,
        val categoryName: String?,
        val subcategoryName: String?
    )

    companion object {
        const val EXTRA_CORRECT_COUNT = "correct_count"
        const val EXTRA_INCORRECT_COUNT = "incorrect_count"
        const val EXTRA_UNANSWERED_COUNT = "unanswered_count"
        const val EXTRA_CORRECT_RATE = "correct_rate"

        const val EXTRA_WRONG_UUIDS = "wrong_uuids"
        const val EXTRA_TIME_QIDS = "time_qids"
        const val EXTRA_TIME_MILLIS = "time_millis"
        const val EXTRA_ATTEMPT_START_EPOCH = "attempt_start_epoch"
        const val EXTRA_ATTEMPT_END_EPOCH = "attempt_end_epoch"
        const val EXTRA_QUESTION_ATTEMPTS = "question_attempts"

        const val EXTRA_WRONG_USER_ANSWERS = "wrong_user_answers"

        private const val TAG = "ResultActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra("user") as? User

        displayResults()
        setupClicks()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAiPractice.setOnClickListener { startNextActionPractice() }

        // ✅ 点击“错误”卡片跳转错题页
        binding.cardIncorrect.setOnClickListener {
            if (wrongUuids.isEmpty()) {
                Toast.makeText(this, "本次没有错题 🎉", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val itn = Intent(this, WrongReviewActivity::class.java).apply {
                putStringArrayListExtra(WrongReviewActivity.EXTRA_WRONG_UUIDS, wrongUuids)
                putStringArrayListExtra(WrongReviewActivity.EXTRA_WRONG_USER_ANSWERS, wrongUserAnswers)
            }
            startActivity(itn)
        }
    }

    private fun displayResults() {
        val totalQuestions = intent.getIntExtra(QuizActivity.EXTRA_TOTAL_QUESTIONS, 0)
        val timeSpentMillis = intent.getLongExtra(QuizActivity.EXTRA_TIME_SPENT, 0L)
        val categoryName = intent.getStringExtra(QuizActivity.EXTRA_CATEGORY_NAME) ?: "未知分类"

        val correctCount = intent.getIntExtra(EXTRA_CORRECT_COUNT, 0)
        val incorrectCount = intent.getIntExtra(EXTRA_INCORRECT_COUNT, 0)
        val unansweredCount = intent.getIntExtra(EXTRA_UNANSWERED_COUNT, 0)
        val correctRate = intent.getIntExtra(EXTRA_CORRECT_RATE, 0).coerceIn(0, 100)

        // ✅ 取出：错题 uuid 列表 + 错选答案列表（赋值给成员变量）
        wrongUuids = intent.getStringArrayListExtra(EXTRA_WRONG_UUIDS) ?: arrayListOf()
        wrongUserAnswers = intent.getStringArrayListExtra(EXTRA_WRONG_USER_ANSWERS) ?: arrayListOf()

        // ✅ 取出：每题耗时（qId数组 + millis数组）
        val timeQids = intent.getLongArrayExtra(EXTRA_TIME_QIDS) ?: LongArray(0)
        val timeMillisArr = intent.getLongArrayExtra(EXTRA_TIME_MILLIS) ?: LongArray(0)

        // ✅ 取出：练习开始/结束时间戳
        val attemptStartEpoch = intent.getLongExtra(EXTRA_ATTEMPT_START_EPOCH, 0L)
        val attemptEndEpoch = intent.getLongExtra(EXTRA_ATTEMPT_END_EPOCH, 0L)
        @Suppress("UNCHECKED_CAST")
        val attempts = intent.getSerializableExtra(EXTRA_QUESTION_ATTEMPTS) as? ArrayList<QuestionAttempt>
            ?: arrayListOf()

        Log.d(TAG, "wrongUuids size=${wrongUuids.size} => $wrongUuids")
        Log.d(TAG, "wrongUserAnswers size=${wrongUserAnswers.size} => $wrongUserAnswers")
        Log.d(TAG, "time arrays size qids=${timeQids.size}, millis=${timeMillisArr.size}")
        Log.d(TAG, "attemptStart=$attemptStartEpoch, attemptEnd=$attemptEndEpoch")

        // 格式化总用时
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeSpentMillis)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        val timeSpentFormatted = String.format("%02d:%02d", minutes, remainingSeconds)

        // 顶部信息
        binding.tvResultCategory.text = "分类：$categoryName"
        binding.tvResultTimeSpent.text = "用时：$timeSpentFormatted"
        binding.tvResultTotalQuestions.text = "本次共 $totalQuestions 题"

        // 三卡数字
        binding.tvResultCorrectCount.text = correctCount.toString()
        binding.tvResultIncorrectCount.text = incorrectCount.toString()
        binding.tvResultUnansweredCount.text = unansweredCount.toString()

        // 仪表盘 + 正确率动画
        binding.gaugeView.setValueInstant(0)
        binding.gaugeView.animateTo(correctRate, 900L)
        animateRateText(correctRate, 900L)

        // ✅ 拉取 AI 点评
        fetchAiComment(
            categoryName = categoryName,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            correctRate = correctRate,
            timeSpentMillis = timeSpentMillis,
            wrongUuids = wrongUuids,
            timeQids = timeQids,
            timeMillisArr = timeMillisArr,
            questionAttempts = attempts,
            attemptStartEpoch = attemptStartEpoch,
            attemptEndEpoch = attemptEndEpoch
        )
    }

    private fun fetchAiComment(
        categoryName: String,
        totalQuestions: Int,
        correctCount: Int,
        incorrectCount: Int,
        unansweredCount: Int,
        correctRate: Int,
        timeSpentMillis: Long,
        wrongUuids: List<String>,
        timeQids: LongArray,
        timeMillisArr: LongArray,
        questionAttempts: List<QuestionAttempt>,
        attemptStartEpoch: Long,
        attemptEndEpoch: Long
    ) {
        startAiCommentLoading()

        val topSlow = buildTopSlowQuestions(timeQids, timeMillisArr, topK = 5)
        val attemptsForAi = if (questionAttempts.isNotEmpty()) {
            val wrong = questionAttempts.filter { it.status == "incorrect" }
            val others = questionAttempts.filter { it.status != "incorrect" }
            (wrong + others).take(30)
        } else {
            emptyList()
        }

        val req = PracticeCommentRequest(
            categoryName = categoryName,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            correctRate = correctRate,
            timeSpentSeconds = TimeUnit.MILLISECONDS.toSeconds(timeSpentMillis),
            wrongUuids = wrongUuids.take(30),
            topSlowQuestions = topSlow,
            questionAttempts = attemptsForAi,
            attemptStartEpoch = attemptStartEpoch,
            attemptEndEpoch = attemptEndEpoch
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

    private fun buildTopSlowQuestions(
        qids: LongArray,
        millis: LongArray,
        topK: Int
    ): List<SlowQuestion> {
        val n = minOf(qids.size, millis.size)
        if (n <= 0) return emptyList()

        return (0 until n)
            .map { i -> SlowQuestion(qids[i], TimeUnit.MILLISECONDS.toSeconds(millis[i])) }
            .sortedByDescending { it.seconds }
            .take(topK)
    }

    private fun startAiCommentLoading() {
        binding.pbAiCommentLoading.visibility = View.VISIBLE
        binding.llAiSkeleton.visibility = View.VISIBLE
        binding.tvAiComment.visibility = View.GONE
        binding.btnAiPractice.visibility = View.GONE

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
        nextAction = extractNextAction(text)
        updateNextActionButton()
        val pretty = formatAiComment(text)
        binding.tvAiComment.text = buildBoldSpannable(pretty)
    }

    private fun updateNextActionButton() {
        val action = nextAction
        binding.btnAiPractice.visibility =
            if (action != null && action.categoryId > 0 && currentUser != null) View.VISIBLE else View.GONE
    }

    private fun startNextActionPractice() {
        val action = nextAction
        val user = currentUser
        if (action == null || user == null || action.categoryId <= 0) {
            Toast.makeText(this, "暂无可用的练习推荐", Toast.LENGTH_SHORT).show()
            return
        }

        val count = action.count.coerceIn(5, 20)

        QuizActivity.startQuizActivity(
            context = this,
            user = user,
            categoryId = action.categoryId,
            categoryName = action.categoryName,
            subcategoryId = action.subcategoryId,
            subcategoryName = action.subcategoryName,
            questionCount = count
        )
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

    private fun extractNextAction(text: String): NextAction? {
        val line = text.lines().firstOrNull { it.contains("NEXT_ACTION") } ?: return null
        val match = Regex("NEXT_ACTION\\s*:\\s*(\\{[^}]*\\})").find(line) ?: return null
        val json = match.groupValues[1]
        return try {
            val obj = JsonParser().parse(json).asJsonObject
            val categoryId = obj.get("categoryId")?.asInt ?: return null
            val subcategoryId = obj.get("subcategoryId")?.asInt ?: 0
            val count = obj.get("count")?.asInt ?: 10
            val categoryName = obj.get("categoryName")?.asString
            val subcategoryName = obj.get("subcategoryName")?.asString
            NextAction(
                categoryId = categoryId,
                subcategoryId = subcategoryId.takeIf { it > 0 },
                count = count,
                categoryName = categoryName,
                subcategoryName = subcategoryName
            )
        } catch (_: Exception) {
            null
        }
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

    private fun animateRateText(targetRate: Int, duration: Long) {
        ValueAnimator.ofInt(0, targetRate).apply {
            this.duration = duration
            addUpdateListener {
                val v = it.animatedValue as Int
                binding.tvRateBig.text = "$v%"
            }
            start()
        }
    }

    override fun onDestroy() {
        stopAiCommentLoading()
        super.onDestroy()
    }
}

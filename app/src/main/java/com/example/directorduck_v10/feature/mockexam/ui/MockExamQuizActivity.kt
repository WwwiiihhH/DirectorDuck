package com.example.directorduck_v10.feature.mockexam.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.isOk
import com.example.directorduck_v10.databinding.ActivityMockExamQuizBinding
import com.example.directorduck_v10.feature.mockexam.model.MockExamSubmitRequest
import com.example.directorduck_v10.feature.practice.adapter.AnswerSheetAdapter
import com.example.directorduck_v10.feature.practice.adapter.GridSpacingItemDecoration
import com.example.directorduck_v10.feature.practice.adapter.QuestionPagerAdapter
import com.example.directorduck_v10.feature.practice.model.Question
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MockExamQuizActivity : BaseActivity() {

    private lateinit var binding: ActivityMockExamQuizBinding

    private val sessionId: Long by lazy { intent.getLongExtra(EXTRA_SESSION_ID, -1L) }
    private val sessionTitle: String by lazy { intent.getStringExtra(EXTRA_SESSION_TITLE).orEmpty() }
    private val userId: Long by lazy { intent.getLongExtra(EXTRA_USER_ID, -1L) }
    private val username: String by lazy { intent.getStringExtra(EXTRA_USERNAME).orEmpty() }

    private val questions = mutableListOf<Question>()
    private lateinit var pagerAdapter: QuestionPagerAdapter

    /** ✅ 只记录用户选择：qId(orderIndex) -> A/B/C/D */
    private val userAnswers = mutableMapOf<Long, String>()
    private val questionAnswered = mutableMapOf<Long, Boolean>()

    // --- 倒计时相关 ---
    private var examEndTimeMillis: Long = 0L // 考试结束的时间戳
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private var timeUpDialog: AlertDialog? = null

    // ✅ 修改重点 1：提前交卷缓冲时间改为 5000ms (5秒)
    // 预留足够的时间，防止客户端因为网络延迟或时间误差，导致请求到达后端时考试已结束
    private val AUTO_SUBMIT_BUFFER_MS = 5000L

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val now = System.currentTimeMillis()
                val remainingMillis = examEndTimeMillis - now

                // 如果剩余时间大于缓冲时间（5秒），继续倒计时
                if (remainingMillis > AUTO_SUBMIT_BUFFER_MS) {
                    updateTimerText(remainingMillis)
                    handler.postDelayed(this, 1000)
                } else {
                    // 剩余时间不足5秒，视为时间耗尽，强制归零并触发自动交卷
                    binding.tvTime.text = "00:00:00"
                    stopTimer()
                    handleTimeUp()
                }
            }
        }
    }

    // --- 答题卡 ---
    private var answerSheetDialog: BottomSheetDialog? = null
    private var answerSheetAdapter: AnswerSheetAdapter? = null
    private var answerSheetDecorationAdded = false

    // 防止重复提交标记 & Loading
    private var isSubmitting = false
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockExamQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (sessionId <= 0 || userId <= 0 || username.isBlank()) {
            Toast.makeText(this, "参数错误，请从模考列表进入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 拦截系统返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.draftOverlay.visibility == android.view.View.VISIBLE) {
                    binding.draftOverlay.visibility = android.view.View.GONE
                } else {
                    confirmExit()
                }
            }
        })

        setupUI()
        loadPaperAndQuestions()
    }

    private fun setupUI() {
        binding.back.setOnClickListener { confirmExit() }

        binding.tvCategoryInfo.text = if (sessionTitle.isNotBlank()) "模考（$sessionTitle）" else "模考答题"
        binding.tvProgress.text = "0/0"
        binding.tvTime.text = "--:--"

        binding.ivPen.setOnClickListener { binding.draftOverlay.visibility = android.view.View.VISIBLE }
        binding.btnDraftClose.setOnClickListener { binding.draftOverlay.visibility = android.view.View.GONE }
        binding.btnDraftClear.setOnClickListener { binding.draftView.clear() }
        binding.btnDraftUndo.setOnClickListener { binding.draftView.undo() }

        binding.ivBookmark.setOnClickListener {
            Toast.makeText(this, "模考页暂不支持收藏", Toast.LENGTH_SHORT).show()
        }

        pagerAdapter = QuestionPagerAdapter(
            questions = questions,
            onAnswerSelected = { questionId, selectedOption ->
                userAnswers[questionId] = selectedOption
                questionAnswered[questionId] = true
                answerSheetAdapter?.refresh()
            },
            onSubmitClick = { _, _ -> },
            getSelectedOption = { qId -> userAnswers[qId] }
        )

        binding.viewPagerQuestions.adapter = pagerAdapter
        binding.viewPagerQuestions.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgressIndicator()
            }
        })

        binding.ivAnswersheet.setOnClickListener { showAnswerSheetDialog() }
    }

    private fun loadPaperAndQuestions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 获取场次详情以拿到 endTime
                var endTimeFromServer: Long = 0L
                val detailResp = ApiClient.mockExamService.getSessionDetail(sessionId)

                if (detailResp.isSuccessful && detailResp.body()?.isOk() == true) {
                    val dto = detailResp.body()!!.data
                    if (dto != null) {
                        endTimeFromServer = parseServerTime(dto.endTime)
                        Log.d(TAG, "获取到考试结束时间: ${dto.endTime} -> $endTimeFromServer")
                    }
                } else {
                    Log.e(TAG, "获取考试时间详情失败: ${detailResp.code()}")
                }

                // 2. 生成试卷
                val genResp = ApiClient.mockExamService.generatePaper(sessionId)
                if (!genResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamQuizActivity, "生成试卷失败: ${genResp.code()}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val genBody = genResp.body()
                if (genBody == null || !genBody.isOk()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamQuizActivity, genBody?.message ?: "生成试卷失败", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                // 3. 获取题目
                val qResp = ApiClient.mockExamService.getPaperQuestions(sessionId)
                if (!qResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamQuizActivity, "加载题目失败: ${qResp.code()}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val qBody = qResp.body()
                val list = qBody?.data
                if (qBody == null || !qBody.isOk() || list == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamQuizActivity, qBody?.message ?: "加载题目失败", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val mapped: List<Question> = list
                    .sortedBy { dto -> dto.orderIndex }
                    .map { dto ->
                        Question(
                            id = dto.orderIndex.toLong(),
                            uuid = dto.uuid,
                            questionText = dto.questionText,
                            questionImage = dto.questionImage?.takeIf { it.isNotBlank() },
                            optionA = dto.optionA,
                            optionB = dto.optionB,
                            optionC = dto.optionC,
                            optionD = dto.optionD
                        )
                    }

                // 4. 更新UI并启动倒计时
                withContext(Dispatchers.Main) {
                    questions.clear()
                    questions.addAll(mapped)
                    pagerAdapter.notifyDataSetChanged()
                    updateProgressIndicator()

                    if (questions.isEmpty()) {
                        Toast.makeText(this@MockExamQuizActivity, "本场试卷暂无题目", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // 如果当前时间还未到 (结束时间 - 5秒缓冲)，则启动倒计时
                        if (endTimeFromServer > System.currentTimeMillis() + AUTO_SUBMIT_BUFFER_MS) {
                            examEndTimeMillis = endTimeFromServer
                            startTimer()
                        } else {
                            // 否则直接判定为考试结束
                            binding.tvTime.text = "00:00:00"
                            AlertDialog.Builder(this@MockExamQuizActivity)
                                .setTitle("考试已结束")
                                .setMessage("当前时间已超过本场考试结束时间。")
                                .setCancelable(false)
                                .setPositiveButton("退出") { _, _ -> finish() }
                                .show()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamQuizActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun parseServerTime(timeStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timeStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "时间解析失败: $timeStr", e)
            0L
        }
    }

    private fun startTimer() {
        if (!isTimerRunning && examEndTimeMillis > System.currentTimeMillis()) {
            isTimerRunning = true
            handler.post(updateTimerRunnable)
        }
    }

    private fun stopTimer() {
        if (isTimerRunning) {
            isTimerRunning = false
            handler.removeCallbacks(updateTimerRunnable)
        }
    }

    private fun updateTimerText(remainingMillis: Long) {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val timeStr = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        binding.tvTime.text = timeStr

        if (remainingMillis < 5 * 60 * 1000) {
            binding.tvTime.setTextColor(Color.RED)
        } else {
            binding.tvTime.setTextColor(Color.BLACK)
        }
    }

    private fun handleTimeUp() {
        Log.e(TAG, "1. 进入 handleTimeUp 方法")

        if (isFinishing || isDestroyed) return
        if (isSubmitting) return

        // 停止计时器
        stopTimer()

        // 弹出不可取消的“正在交卷”对话框
        if (timeUpDialog == null) {
            timeUpDialog = AlertDialog.Builder(this)
                .setTitle("考试结束")
                .setMessage("考试时间已到，系统正在自动为您交卷，请稍候...")
                .setCancelable(false) // 禁止取消
                .create()
        }

        if (timeUpDialog?.isShowing == false) {
            timeUpDialog?.show()
        }

        // 立即触发交卷
        submitPaper(isAutoSubmit = true)
    }

    private fun submitPaper(isAutoSubmit: Boolean = false) {
        Log.e(TAG, "2. 进入 submitPaper, isAutoSubmit=$isAutoSubmit")

        if (isSubmitting) return
        if (questions.isEmpty()) return

        isSubmitting = true

        // 如果不是自动交卷（手动点击），且此时没有正在显示的强制弹窗，则显示 Loading
        if (!isAutoSubmit) {
            showLoadingDialog()
        }

        // 组装答案
        val answers = MutableList(questions.size) { "" }
        try {
            userAnswers.forEach { (qId, ans) ->
                val idx = (qId.toInt() - 1)
                if (idx in answers.indices) {
                    answers[idx] = ans
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据组装异常", e)
            isSubmitting = false
            return
        }

        val req = MockExamSubmitRequest(
            userId = userId,
            username = username,
            answers = answers
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "发起交卷请求...")
                val resp = ApiClient.mockExamService.submitPaper(sessionId, req)

                if (!resp.isSuccessful) {
                    val errorString = resp.errorBody()?.string() ?: "无错误体"
                    Log.e(TAG, "请求失败: $errorString")
                    withContext(Dispatchers.Main) {
                        handleSubmitFailure("提交失败(${resp.code()}): 请检查网络")
                    }
                    return@launch
                }

                val body = resp.body()

                withContext(Dispatchers.Main) {
                    // ✅ 关键：请求结束后，清理所有弹窗
                    dismissAllDialogs()

                    if (body != null && body.isOk() && body.data != null) {
                        Log.d(TAG, "✅ 交卷成功，跳转结果页")
                        stopTimer()
                        MockExamResultActivity.start(
                            context = this@MockExamQuizActivity,
                            result = body.data!!,
                            elapsedMillis = 0L,
                            sessionTitle = sessionTitle
                        )
                        finish()
                    } else {
                        val msg = body?.message ?: "未知错误"
                        Log.e(TAG, "❌ 业务报错: $msg")

                        // ✅ 修改重点 2：判断是否因为“已收卷”导致的失败
                        // 如果后端返回“已收卷”或“无法提交”，说明考试彻底结束，不应重试
                        if (msg.contains("已收卷") || msg.contains("结束") || msg.contains("无法提交")) {
                            handleLateSubmission()
                        } else {
                            // 其他错误（如网络波动），允许用户重试
                            handleSubmitFailure(msg)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "网络异常", e)
                withContext(Dispatchers.Main) {
                    handleSubmitFailure("网络异常: ${e.message}")
                }
            }
        }
    }

    // ✅ 新增：处理“提交太晚，被后端拒收”的情况 -> 不再重试，直接退出
    private fun handleLateSubmission() {
        Log.e(TAG, "判定为提交超时，不再重试，直接退出")
        isSubmitting = false
        stopTimer()
        dismissAllDialogs() // 确保之前的 Loading 消失

        AlertDialog.Builder(this)
            .setTitle("考试已结束")
            .setMessage("考试时间已结束，系统已停止收卷。您的试卷可能已由系统自动处理，请退出查看结果。")
            .setPositiveButton("退出") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // 处理普通失败 -> 允许重试
    private fun handleSubmitFailure(msg: String) {
        Log.e(TAG, "执行失败逻辑: $msg")
        dismissAllDialogs()
        isSubmitting = false

        AlertDialog.Builder(this)
            .setTitle("交卷失败")
            .setMessage(msg)
            .setPositiveButton("重试") { _, _ ->
                submitPaper(isAutoSubmit = true) // 再次尝试自动提交
            }
            .setNegativeButton("退出") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // ✅ 新增：统一关闭所有弹窗，防止遮挡
    private fun dismissAllDialogs() {
        try {
            if (loadingDialog?.isShowing == true) loadingDialog?.dismiss()
            if (timeUpDialog?.isShowing == true) timeUpDialog?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = AlertDialog.Builder(this)
                .setMessage("正在提交试卷，请稍候...")
                .setCancelable(false)
                .create()
        }
        loadingDialog?.show()
    }

    private fun showAnswerSheetDialog() {
        if (questions.isEmpty()) return

        if (answerSheetDialog == null) {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_answer_sheet, null, false)

            val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAnswerSheet)
            val btnSubmitPaper = view.findViewById<android.widget.Button>(R.id.btnSubmitPaper)

            btnSubmitPaper.setOnClickListener {
                dialog.dismiss()
                submitPaper()
            }

            rv.layoutManager = GridLayoutManager(this, 5)

            if (!answerSheetDecorationAdded) {
                rv.addItemDecoration(
                    GridSpacingItemDecoration(
                        spanCount = 5,
                        spacingH = dpToPx(16),
                        spacingV = dpToPx(24),
                        includeEdge = true
                    )
                )
                answerSheetDecorationAdded = true
            }

            answerSheetAdapter = AnswerSheetAdapter(
                total = questions.size,
                isAnswered = { position ->
                    val qId = questions[position].id
                    questionAnswered[qId] == true
                },
                onClick = { position ->
                    binding.viewPagerQuestions.setCurrentItem(position, false)
                    dialog.dismiss()
                }
            )

            rv.adapter = answerSheetAdapter
            dialog.setContentView(view)
            answerSheetDialog = dialog
        }

        answerSheetAdapter?.refresh()
        answerSheetDialog?.show()
    }

    private fun updateProgressIndicator() {
        val cur = if (questions.isEmpty()) 0 else binding.viewPagerQuestions.currentItem + 1
        binding.tvProgress.text = "$cur/${questions.size}"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun confirmExit() {
        if (isSubmitting) return
        AlertDialog.Builder(this)
            .setTitle("确认退出？")
            .setMessage("退出后可在本场未结束时继续进入；本机仅保存已选答案，未交卷不会计分。")
            .setNegativeButton("继续答题", null)
            .setPositiveButton("退出") { _, _ -> finish() }
            .show()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        if (questions.isNotEmpty()) startTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        answerSheetDialog?.dismiss()
        answerSheetDialog = null
        loadingDialog?.dismiss()
        timeUpDialog?.dismiss() // 确保销毁时清理引用
    }

    companion object {
        private const val EXTRA_SESSION_ID = "sessionId"
        private const val EXTRA_SESSION_TITLE = "sessionTitle"
        private const val EXTRA_USER_ID = "userId"
        private const val EXTRA_USERNAME = "username"
        private const val TAG = "MockExamQuizActivity"

        fun start(context: Context, sessionId: Long, sessionTitle: String, userId: Long, username: String) {
            val it = Intent(context, MockExamQuizActivity::class.java)
            it.putExtra(EXTRA_SESSION_ID, sessionId)
            it.putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            it.putExtra(EXTRA_USER_ID, userId)
            it.putExtra(EXTRA_USERNAME, username)
            context.startActivity(it)
        }
    }
}
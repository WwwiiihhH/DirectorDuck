package com.example.directorduck_v10

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.adapters.AnswerSheetAdapter
import com.example.directorduck_v10.adapters.GridSpacingItemDecoration
import com.example.directorduck_v10.adapters.QuestionPagerAdapter
import com.example.directorduck_v10.data.api.RandomQuestionRequest
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityQuizBinding
import com.example.directorduck_v10.fragments.practice.data.Question
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope

class QuizActivity : BaseActivity() {

    private lateinit var binding: ActivityQuizBinding
    private lateinit var currentUser: User
    private lateinit var questionPagerAdapter: QuestionPagerAdapter
    private val questions = mutableListOf<Question>()

    private var categoryId: Int = -1
    private var categoryName: String? = null
    private var subcategoryId: Int = -1
    private var subcategoryName: String? = null
    private var questionCount: Int = 10

    // --- 计时器相关变量 ---
    private var startTimeMillis: Long = 0L
    private var elapsedTimeMillis: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                elapsedTimeMillis = SystemClock.elapsedRealtime() - startTimeMillis
                updateTimerText()
                handler.postDelayed(this, 1000)
            }
        }
    }

    // --- 答题结果统计变量 ---
    private val userAnswers = mutableMapOf<Long, String>()          // qId -> userAnswer (A/B/C/D)
    private val correctAnswers = mutableMapOf<Long, String>()       // qId -> correctAnswer
    private val questionStatus = mutableMapOf<Long, String>()       // qId -> "correct"/"incorrect"/"unanswered"
    private val questionAnswered = mutableMapOf<Long, Boolean>()    // qId -> answered?

    // --- 答题卡 BottomSheet ---
    private var answerSheetDialog: BottomSheetDialog? = null
    private var answerSheetAdapter: AnswerSheetAdapter? = null
    private var answerSheetDecorationAdded = false

    // --- 每题耗时（累计在该题停留的时间） ---
    private val questionTimeSpentMillis = mutableMapOf<Long, Long>()
    private var currentPageIndex: Int = 0
    private var currentPageStartRealtime: Long = 0L

    // --- 本次练习时间戳（给后端存记录用） ---
    private var attemptStartEpochMillis: Long = 0L
    private var attemptEndEpochMillis: Long = 0L

    // --- 收藏相关 ---
    private val favoriteStateByUuid = mutableMapOf<String, Boolean>() // 缓存：uuid -> 是否收藏
    private var bookmarkQuerySeq = 0 // 防止快速滑动时旧请求覆盖新请求
    private var bookmarkOpInFlight = false // 防止连点

    companion object {
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_SUBCATEGORY_ID = "subcategory_id"
        const val EXTRA_SUBCATEGORY_NAME = "subcategory_name"
        const val EXTRA_QUESTION_COUNT = "question_count"

        private const val TAG = "QuizActivity"

        fun startQuizActivity(
            context: Context,
            user: User,
            categoryId: Int? = null,
            categoryName: String? = null,
            subcategoryId: Int? = null,
            subcategoryName: String? = null,
            questionCount: Int
        ) {
            val intent = Intent(context, QuizActivity::class.java).apply {
                putExtra("user", user)
                categoryId?.let { putExtra(EXTRA_CATEGORY_ID, it) }
                categoryName?.let { putExtra(EXTRA_CATEGORY_NAME, it) }
                subcategoryId?.let { putExtra(EXTRA_SUBCATEGORY_ID, it) }
                subcategoryName?.let { putExtra(EXTRA_SUBCATEGORY_NAME, it) }
                putExtra(EXTRA_QUESTION_COUNT, questionCount)
            }
            context.startActivity(intent)
        }

        const val EXTRA_TOTAL_QUESTIONS = "total_questions"
        const val EXTRA_TIME_SPENT = "time_spent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!initData()) return

        setupUI()
        loadQuestions()

        binding.back.setOnClickListener { finish() }
    }

    private fun initData(): Boolean {
        return try {
            currentUser = intent.getSerializableExtra("user") as User
            categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
            categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
            subcategoryId = intent.getIntExtra(EXTRA_SUBCATEGORY_ID, -1)
            subcategoryName = intent.getStringExtra(EXTRA_SUBCATEGORY_NAME)
            questionCount = intent.getIntExtra(EXTRA_QUESTION_COUNT, 10)

            Log.d(TAG, "获取到用户信息: ID=${currentUser.id}, Username=${currentUser.username}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "获取数据失败: ${e.message}", e)
            Toast.makeText(this, "数据获取失败", Toast.LENGTH_SHORT).show()
            finish()
            false
        }
    }

    private fun setupUI() {
        binding.ivPen.setOnClickListener { showDraftOverlay() }
        binding.btnDraftClose.setOnClickListener { hideDraftOverlay() }
        binding.btnDraftClear.setOnClickListener { binding.draftView.clear() }
        binding.btnDraftUndo.setOnClickListener { binding.draftView.undo() }

        binding.ivBookmark.setOnClickListener {
            toggleFavoriteForCurrentQuestion()
        }

        binding.tvCategoryInfo.text = when {
            subcategoryName != null -> "专项练习(${subcategoryName})"
            categoryName != null -> "专项练习(${categoryName})"
            else -> "答题练习"
        }

        initTimer()

        questionPagerAdapter = QuestionPagerAdapter(
            questions = questions,
            onAnswerSelected = { questionId, selectedOption ->
                userAnswers[questionId] = selectedOption
                questionAnswered[questionId] = true
                answerSheetAdapter?.refresh()
                fetchAndCheckAnswer(questionId, selectedOption)
            },
            onSubmitClick = { _, selectedOption ->
                Toast.makeText(this, "提交答案: $selectedOption", Toast.LENGTH_SHORT).show()
                moveToNextQuestion()
            },
            getSelectedOption = { qId ->
                userAnswers[qId]   // ✅ 关键：从 Activity 的 map 读取
            }
        )


        binding.viewPagerQuestions.adapter = questionPagerAdapter

        binding.viewPagerQuestions.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 记录上一题耗时
                recordCurrentQuestionTime()

                // 切换到新题，重置基准
                currentPageIndex = position
                currentPageStartRealtime = SystemClock.elapsedRealtime()

                updateProgressIndicator()

                // ✅ 每次翻页刷新书签状态
                refreshBookmarkForPosition(position)
            }
        })

        // 点击答题卡图标弹出底部抽屉
        binding.ivAnswersheet.setOnClickListener { showAnswerSheetDialog() }
    }

    private fun showDraftOverlay() {
        binding.draftOverlay.visibility = android.view.View.VISIBLE
    }

    private fun hideDraftOverlay() {
        binding.draftOverlay.visibility = android.view.View.GONE
    }

    private fun showAnswerSheetDialog() {
        if (questions.isEmpty()) return

        if (answerSheetDialog == null) {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_answer_sheet, null, false)

            val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAnswerSheet)
            val btnSubmitPaper = view.findViewById<android.widget.Button>(R.id.btnSubmitPaper)

            btnSubmitPaper.setOnClickListener {
                val unanswered = questions.count { q -> questionAnswered[q.id] != true }
                if (unanswered > 0) {
                    Toast.makeText(this, "还有 $unanswered 题未作答，已为你直接交卷", Toast.LENGTH_SHORT).show()
                }
                answerSheetDialog?.dismiss()
                calculateAndShowResult()
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
                    answerSheetDialog?.dismiss()
                }
            )

            rv.adapter = answerSheetAdapter
            dialog.setContentView(view)
            answerSheetDialog = dialog
        }

        answerSheetAdapter?.refresh()
        answerSheetDialog?.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun fetchAndCheckAnswer(questionId: Long, userAnswer: String) {
        val question = questions.find { it.id == questionId }
        if (question == null || question.uuid == null) {
            questionStatus[questionId] = "unanswered"
            Log.e(TAG, "找不到题目ID为 $questionId 的UUID")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.practiceService.getQuestionAnswerByUuid(question.uuid!!)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        val correctAnswer = apiResponse.data.correctAnswer
                        correctAnswers[questionId] = correctAnswer
                        val status = if (userAnswer == correctAnswer) "correct" else "incorrect"
                        questionStatus[questionId] = status
                    } else {
                        questionStatus[questionId] = "unanswered"
                    }
                } else {
                    questionStatus[questionId] = "unanswered"
                }
            } catch (e: Exception) {
                questionStatus[questionId] = "unanswered"
                Log.e(TAG, "获取正确答案网络错误: ${e.message}, 题目ID: $questionId", e)
            }
        }
    }

    private fun initTimer() {
        elapsedTimeMillis = 0L
        isTimerRunning = false
        updateTimerText()
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            startTimeMillis = SystemClock.elapsedRealtime() - elapsedTimeMillis
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

    private fun updateTimerText() {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        binding.tvTime.text = String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun loadQuestions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RandomQuestionRequest(
                    categoryId = if (subcategoryId == -1) categoryId else null,
                    subcategoryId = if (subcategoryId != -1) subcategoryId else null,
                    count = questionCount
                )

                val response = ApiClient.practiceService.getRandomQuestions(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        withContext(Dispatchers.Main) {
                            questions.clear()
                            questions.addAll(apiResponse.data)

                            questionPagerAdapter.notifyDataSetChanged()
                            updateProgressIndicator()

                            // ✅ 第一题进来就刷新收藏状态
                            refreshBookmarkForPosition(0)

                            if (questions.isEmpty()) {
                                Toast.makeText(this@QuizActivity, "没有找到相关题目", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                attemptStartEpochMillis = System.currentTimeMillis()
                                currentPageIndex = binding.viewPagerQuestions.currentItem
                                currentPageStartRealtime = SystemClock.elapsedRealtime()
                                startTimer()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@QuizActivity,
                                apiResponse?.message ?: "加载题目失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@QuizActivity,
                            "服务器响应错误: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuizActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateProgressIndicator() {
        val currentPosition = binding.viewPagerQuestions.currentItem + 1
        binding.tvProgress.text = "$currentPosition/${questions.size}"
    }

    private fun moveToNextQuestion() {
        val currentPosition = binding.viewPagerQuestions.currentItem
        if (currentPosition < questions.size - 1) {
            binding.viewPagerQuestions.currentItem = currentPosition + 1
        } else {
            calculateAndShowResult()
        }
    }

    private fun calculateAndShowResult() {
        recordCurrentQuestionTime()

        stopTimer()
        attemptEndEpochMillis = System.currentTimeMillis()

        val totalQuestions = questions.size
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0

        for (q in questions) {
            val qId = q.id
            val isAnswered = questionAnswered[qId] == true
            val status = questionStatus[qId] ?: "unanswered"

            if (isAnswered) {
                when (status) {
                    "correct" -> correctCount++
                    "incorrect" -> incorrectCount++
                    "unanswered" -> incorrectCount++
                    else -> incorrectCount++
                }
            } else {
                unansweredCount++
            }
        }

        val correctRate = if (totalQuestions > 0) {
            (correctCount.toDouble() / totalQuestions * 100).toInt()
        } else 0

        val wrongUuids = arrayListOf<String>()
        val wrongUserAnswers = arrayListOf<String>()

        for (q in questions) {
            val qId = q.id
            val status = questionStatus[qId] ?: "unanswered"
            if (questionAnswered[qId] == true && status == "incorrect") {
                val uuid = q.uuid
                val ua = userAnswers[qId]
                if (!uuid.isNullOrBlank() && !ua.isNullOrBlank()) {
                    wrongUuids.add(uuid)
                    wrongUserAnswers.add(ua)
                }
            }
        }

        val timeQids = LongArray(questions.size)
        val timeMillis = LongArray(questions.size)
        for (i in questions.indices) {
            val qId = questions[i].id
            timeQids[i] = qId
            timeMillis[i] = questionTimeSpentMillis[qId] ?: 0L
        }

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_TOTAL_QUESTIONS, totalQuestions)
            putExtra(EXTRA_TIME_SPENT, elapsedTimeMillis)
            putExtra(EXTRA_CATEGORY_NAME, categoryName ?: subcategoryName ?: "未知分类")

            putExtra(ResultActivity.EXTRA_CORRECT_COUNT, correctCount)
            putExtra(ResultActivity.EXTRA_INCORRECT_COUNT, incorrectCount)
            putExtra(ResultActivity.EXTRA_UNANSWERED_COUNT, unansweredCount)
            putExtra(ResultActivity.EXTRA_CORRECT_RATE, correctRate)

            putStringArrayListExtra(ResultActivity.EXTRA_WRONG_UUIDS, wrongUuids)
            putStringArrayListExtra(ResultActivity.EXTRA_WRONG_USER_ANSWERS, wrongUserAnswers)

            putExtra(ResultActivity.EXTRA_TIME_QIDS, timeQids)
            putExtra(ResultActivity.EXTRA_TIME_MILLIS, timeMillis)

            putExtra(ResultActivity.EXTRA_ATTEMPT_START_EPOCH, attemptStartEpochMillis)
            putExtra(ResultActivity.EXTRA_ATTEMPT_END_EPOCH, attemptEndEpochMillis)

            putExtra("user_id", currentUser.id)
            putExtra("category_id", categoryId)
            putExtra("subcategory_id", subcategoryId)
        }

        startActivity(intent)
        finish()
    }

    private fun recordCurrentQuestionTime() {
        if (questions.isEmpty()) return
        if (currentPageStartRealtime == 0L) return

        val q = questions.getOrNull(currentPageIndex) ?: return
        val now = SystemClock.elapsedRealtime()
        val delta = now - currentPageStartRealtime

        val prev = questionTimeSpentMillis[q.id] ?: 0L
        questionTimeSpentMillis[q.id] = prev + delta

        currentPageStartRealtime = now
    }

    private fun setBookmarkIcon(isFav: Boolean) {
        val res = if (isFav) R.drawable.bookmark_pressed else R.drawable.bookmark
        binding.ivBookmark.setBackgroundResource(res)
    }

    private fun refreshBookmarkForPosition(position: Int) {
        val uuid = questions.getOrNull(position)?.uuid
        if (uuid.isNullOrBlank()) {
            setBookmarkIcon(false)
            return
        }

        // 1) 有缓存就直接用
        favoriteStateByUuid[uuid]?.let { cached ->
            setBookmarkIcon(cached)
            return
        }

        // 2) 没缓存：先显示未收藏，再异步查
        setBookmarkIcon(false)

        val seq = ++bookmarkQuerySeq
        lifecycleScope.launch(Dispatchers.IO) {   // ✅ 注意这里是 lifecycleScope，不是 ifecycleScope
            try {
                val resp = ApiClient.favoriteService.exists(uuid, currentUser.id)
                val isFav = resp.isSuccessful && resp.body()?.code == 200 && resp.body()?.data == true

                withContext(Dispatchers.Main) {
                    if (seq != bookmarkQuerySeq) return@withContext

                    favoriteStateByUuid[uuid] = isFav
                    val curUuid = questions.getOrNull(binding.viewPagerQuestions.currentItem)?.uuid
                    if (curUuid == uuid) setBookmarkIcon(isFav)
                }
            } catch (_: Exception) {
                // 网络错就保持默认未收藏，不要崩
            }
        }
    }

    private fun toggleFavoriteForCurrentQuestion() {
        if (bookmarkOpInFlight) return

        val position = binding.viewPagerQuestions.currentItem
        val uuid = questions.getOrNull(position)?.uuid
        if (uuid.isNullOrBlank()) return

        val currentFav = favoriteStateByUuid[uuid] == true
        val targetFav = !currentFav

        bookmarkOpInFlight = true

        // 先乐观更新UI
        setBookmarkIcon(targetFav)

        lifecycleScope.launch(Dispatchers.IO) {   // ✅ 统一用 lifecycleScope
            try {
                val resp = if (targetFav) {
                    ApiClient.favoriteService.addFavorite(uuid, currentUser.id)
                } else {
                    ApiClient.favoriteService.removeFavorite(uuid, currentUser.id)
                }

                val ok = resp.isSuccessful && resp.body()?.code == 200

                withContext(Dispatchers.Main) {
                    if (ok) {
                        favoriteStateByUuid[uuid] = targetFav
                        val curUuid = questions.getOrNull(binding.viewPagerQuestions.currentItem)?.uuid
                        if (curUuid == uuid) setBookmarkIcon(targetFav)
                        Toast.makeText(
                            this@QuizActivity,
                            if (targetFav) "已收藏" else "已取消收藏",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // 回滚
                        favoriteStateByUuid[uuid] = currentFav
                        val curUuid = questions.getOrNull(binding.viewPagerQuestions.currentItem)?.uuid
                        if (curUuid == uuid) setBookmarkIcon(currentFav)
                        Toast.makeText(
                            this@QuizActivity,
                            resp.body()?.message ?: "操作失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // 回滚
                    favoriteStateByUuid[uuid] = currentFav
                    val curUuid = questions.getOrNull(binding.viewPagerQuestions.currentItem)?.uuid
                    if (curUuid == uuid) setBookmarkIcon(currentFav)
                    Toast.makeText(this@QuizActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                bookmarkOpInFlight = false
            }
        }
    }

    override fun onBackPressed() {
        if (binding.draftOverlay.visibility == android.view.View.VISIBLE) {
            hideDraftOverlay()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        if (questions.isNotEmpty()) {
            startTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        answerSheetDialog?.dismiss()
        answerSheetDialog = null
    }
}

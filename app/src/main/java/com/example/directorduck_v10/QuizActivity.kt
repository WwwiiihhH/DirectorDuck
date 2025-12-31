package com.example.directorduck_v10

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.data.api.RandomQuestionRequest
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityQuizBinding
import com.example.directorduck_v10.fragments.practice.adapters.QuestionPagerAdapter
import com.example.directorduck_v10.fragments.practice.data.Question
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取用户信息和参数
        if (!initData()) {
            return
        }

        // 设置UI
        setupUI()

        // 加载题目
        loadQuestions()

        binding.back.setOnClickListener{finish()}
    }

    private fun initData(): Boolean {
        try {
            currentUser = intent.getSerializableExtra("user") as User
            categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
            categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
            subcategoryId = intent.getIntExtra(EXTRA_SUBCATEGORY_ID, -1)
            subcategoryName = intent.getStringExtra(EXTRA_SUBCATEGORY_NAME)
            questionCount = intent.getIntExtra(EXTRA_QUESTION_COUNT, 10)

            Log.d(TAG, "获取到用户信息: ID=${currentUser.id}, Username=${currentUser.username}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "获取数据失败: ${e.message}")
            Toast.makeText(this, "数据获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
    }

    private fun setupUI() {
        // 设置顶部信息
        binding.tvCategoryInfo.text = when {
            subcategoryName != null -> "专项练习(${subcategoryName})"
            categoryName != null -> "专项练习(${categoryName})"
            else -> "答题练习"
        }


        // 设置ViewPager
        questionPagerAdapter = QuestionPagerAdapter(
            questions,
            onAnswerSelected = { questionId, selectedOption ->
                // 处理答案选择
                Log.d(TAG, "题目 $questionId 选择了选项: $selectedOption")
            },
            onSubmitClick = { questionId, selectedOption ->
                // 处理提交答案
                if (selectedOption.isEmpty()) {
                    Toast.makeText(this, "请选择一个答案", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "提交答案: $selectedOption", Toast.LENGTH_SHORT).show()
                    // 这里可以添加答案验证逻辑
                    moveToNextQuestion()
                }
            }
        )

        binding.viewPagerQuestions.adapter = questionPagerAdapter

        // 设置页面切换监听器
        binding.viewPagerQuestions.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgressIndicator()
            }
        })
    }

    private fun loadQuestions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 构造请求参数
                val request = RandomQuestionRequest(
                    categoryId = if (subcategoryId == -1) categoryId else null,
                    subcategoryId = if (subcategoryId != -1) subcategoryId else null,
                    count = questionCount
                )

                Log.d(TAG, "请求参数: categoryId=${request.categoryId}, subcategoryId=${request.subcategoryId}, count=${request.count}")

                // 调用API获取随机题目
                val response = ApiClient.practiceService.getRandomQuestions(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        withContext(Dispatchers.Main) {
                            questions.clear()
                            questions.addAll(apiResponse.data)
                            questionPagerAdapter.notifyDataSetChanged()
                            updateProgressIndicator()

                            if (questions.isEmpty()) {
                                Toast.makeText(this@QuizActivity, "没有找到相关题目", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                            Log.d(TAG, "成功加载 ${questions.size} 道题目")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            val errorMsg = apiResponse?.message ?: "加载题目失败"
                            Toast.makeText(this@QuizActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "API返回错误: $errorMsg")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QuizActivity, "服务器响应错误: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "服务器响应错误: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuizActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "网络错误: ${e.message}")
                }
            }
        }
    }

    private fun updateProgressIndicator() {
        val currentPosition = binding.viewPagerQuestions.currentItem + 1
        val totalQuestions = questions.size
        binding.tvProgress.text = "$currentPosition/$totalQuestions"
    }

    private fun moveToNextQuestion() {
        val currentPosition = binding.viewPagerQuestions.currentItem
        if (currentPosition < questions.size - 1) {
            binding.viewPagerQuestions.currentItem = currentPosition + 1
        } else {
            // 最后一题，可以显示结果或结束练习
            Toast.makeText(this, "练习完成！", Toast.LENGTH_SHORT).show()
            // 这里可以跳转到结果页面
        }
    }
}
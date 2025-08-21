package com.example.directorduck_v10

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.directorduck_v10.data.model.User

class QuizActivity : BaseActivity() {

    private lateinit var currentUser: User // 添加用户变量

    companion object {
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_SUBCATEGORY_ID = "subcategory_id"
        const val EXTRA_SUBCATEGORY_NAME = "subcategory_name"
        const val EXTRA_QUESTION_COUNT = "question_count"

        // 添加日志标签
        private const val TAG = "QuizActivity"

        fun startQuizActivity(
            context: Context,
            user: User, // 确保这里有user参数
            categoryId: Int? = null,
            categoryName: String? = null,
            subcategoryId: Int? = null,
            subcategoryName: String? = null,
            questionCount: Int
        ) {
            val intent = Intent(context, QuizActivity::class.java).apply {
                putExtra("user", user) // 传递用户信息
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
        setContentView(R.layout.activity_quiz)

        // 获取用户信息
        try {
            currentUser = intent.getSerializableExtra("user") as User
            Log.d(TAG, "获取到用户信息: ID=${currentUser.id}, Username=${currentUser.username}")
        } catch (e: Exception) {
            Log.e(TAG, "获取用户信息失败: ${e.message}")
            Toast.makeText(this, "用户信息获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 打印所有接收到的Intent数据
        logIntentData()

        // 获取传递的参数
        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
        val subcategoryId = intent.getIntExtra(EXTRA_SUBCATEGORY_ID, -1)
        val subcategoryName = intent.getStringExtra(EXTRA_SUBCATEGORY_NAME)
        val questionCount = intent.getIntExtra(EXTRA_QUESTION_COUNT, 10)

        // 详细的参数日志
        Log.d(TAG, "=== 接收到的参数详情 ===")
        Log.d(TAG, "用户ID: ${currentUser.id}")
        Log.d(TAG, "用户名: ${currentUser.username}")
        Log.d(TAG, "categoryId: $categoryId")
        Log.d(TAG, "categoryName: $categoryName")
        Log.d(TAG, "subcategoryId: $subcategoryId")
        Log.d(TAG, "subcategoryName: $subcategoryName")
        Log.d(TAG, "questionCount: $questionCount")
        Log.d(TAG, "====================")

        // 显示传递的参数（用于测试）
        val message = when {
            subcategoryId != -1 && subcategoryName != null -> {
                "小类刷题: $subcategoryName, 题目数量: $questionCount"
            }
            categoryId != -1 && categoryName != null -> {
                "大类刷题: $categoryName, 题目数量: $questionCount"
            }
            else -> {
                "参数错误"
            }
        }

        Log.d(TAG, "显示消息: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // 这里可以实现具体的答题功能
        setupQuiz(categoryId, categoryName, subcategoryId, subcategoryName, questionCount)
    }

    private fun logIntentData() {
        Log.d(TAG, "=== Intent数据详情 ===")
        Log.d(TAG, "Intent Action: ${intent.action}")
        Log.d(TAG, "Intent Data: ${intent.data}")
        Log.d(TAG, "Intent Extras:")

        // 遍历所有extras
        intent.extras?.keySet()?.forEach { key ->
            val value = intent.extras?.get(key)
            Log.d(TAG, "  $key: $value (type: ${value?.javaClass?.simpleName})")
        }
        Log.d(TAG, "==================")
    }

    private fun setupQuiz(
        categoryId: Int,
        categoryName: String?,
        subcategoryId: Int,
        subcategoryName: String?,
        questionCount: Int
    ) {
        // 这里实现具体的答题逻辑
        // 例如：加载题目、显示题目、处理答题等
        val title = when {
            subcategoryName != null -> "答题 - $subcategoryName"
            categoryName != null -> "答题 - $categoryName"
            else -> "答题练习"
        }

        Log.d(TAG, "设置标题: $title")
        supportActionBar?.title = title

        // 现在你可以使用currentUser变量了
        Log.d(TAG, "当前用户: ${currentUser.username}, ID: ${currentUser.id}")
    }

    // 提供获取当前用户的方法
    fun getCurrentUser(): User {
        return currentUser
    }
}
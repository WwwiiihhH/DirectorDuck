package com.example.directorduck_v10.adapters // 修改包路径

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // 假设你使用 Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.defItems.OptionItem
import com.example.directorduck_v10.fragments.practice.data.Question

class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvQuestionIndex: TextView = itemView.findViewById(R.id.tvQuestionIndex) // 题目序号
    private val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
    private val ivQuestionImage: ImageView = itemView.findViewById(R.id.ivQuestionImage) // 题目图片
    private val optionA: OptionItem = itemView.findViewById(R.id.optionA)
    private val optionB: OptionItem = itemView.findViewById(R.id.optionB)
    private val optionC: OptionItem = itemView.findViewById(R.id.optionC)
    private val optionD: OptionItem = itemView.findViewById(R.id.optionD)
    private val btnSubmit: Button = itemView.findViewById(R.id.btnSubmit)

    private var currentQuestionId: Long = -1
    private var currentSelectedOption: String = ""
    private var onAnswerSelectedCallback: ((Long, String) -> Unit)? = null
    private var onSubmitClickCallback: ((Long, String) -> Unit)? = null

    fun bind(
        question: Question,
        position: Int,
        totalQuestions: Int, // 接收总题目数
        onAnswerSelected: (Long, String) -> Unit,
        onSubmitClick: (Long, String) -> Unit
    ) {
        // 保存回调和当前题目ID
        currentQuestionId = question.id
        onAnswerSelectedCallback = onAnswerSelected
        onSubmitClickCallback = onSubmitClick

        // 重置选项状态
        resetOptionStates()
        currentSelectedOption = ""

        // --- 关键修改：设置题目序号 ---
        tvQuestionIndex.text = "${position + 1}/$totalQuestions"
        // --- 结束修改 ---

        // 绑定题目信息
        tvQuestionText.text = "${question.questionText}" // 移除了序号，因为序号在 tvQuestionIndex 中显示

        // --- 关键修改：处理题目图片 ---
        Log.d("QuestionViewHolder", "Position: $position, Question ID: ${question.id}")
        Log.d("QuestionViewHolder", "Raw questionImage value: '${question.questionImage}'")
        Log.d("QuestionViewHolder", "ApiClient.getQuizImageBaseUrl(): ${ApiClient.getQuizImageBaseUrl()}")

        val questionImageUrl = question.questionImage
        if (!questionImageUrl.isNullOrEmpty()) {
            Log.d("QuestionViewHolder", "Question image URL is not null or empty.")

            // 1. 拼接图片URL：基础URL + 文件名
            val fullImageUrl = "${ApiClient.getQuizImageBaseUrl()}$questionImageUrl"
            Log.d("QuestionViewHolder", "Full image URL to load: $fullImageUrl")

            // 2. 使用图片加载库加载图片
            // Glide 加载时添加日志回调 (适用于 Glide 4.x)
            Glide.with(ivQuestionImage.context)
                .load(fullImageUrl)
                .error(R.drawable.logonew2) // 可选：设置加载失败时的占位图
                .placeholder(R.drawable.logonew2) // 可选：设置加载过程中的占位图
                .listener(object : RequestListener<android.graphics.drawable.Drawable> { // 使用 Glide 4.x 的 RequestListener
                    override fun onLoadFailed(
                        glideException: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("Glide", "Failed to load image from URL: $fullImageUrl", glideException)
                        return false // Return false to allow Glide to handle the error (e.g., show error drawable)
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("Glide", "Image loaded successfully from URL: $fullImageUrl")
                        Log.d("Glide", "Resource type: ${resource?.javaClass?.simpleName}")
                        return false // Return false to allow Glide to handle the resource normally
                    }
                })
                .into(ivQuestionImage)

            // 3. 显示 ImageView
            ivQuestionImage.visibility = View.VISIBLE
            Log.d("QuestionViewHolder", "ImageView visibility set to VISIBLE for position $position")
        } else {
            // 如果没有图片，隐藏 ImageView
            Log.d("QuestionViewHolder", "Question image URL is null or empty. Hiding ImageView.")
            ivQuestionImage.visibility = View.GONE
        }
        // --- 结束修改 ---


        // 绑定选项文本 (移除前缀 A., B., C., D.)
        optionA.setText(question.optionA)
        optionB.setText(question.optionB)
        optionC.setText(question.optionC)
        optionD.setText(question.optionD)


        optionA.setLabel("A")
        optionB.setLabel("B")
        optionC.setLabel("C")
        optionD.setLabel("D")

        // 设置选项点击监听器
        optionA.setOnClickListener { onOptionSelected("A") }
        optionB.setOnClickListener { onOptionSelected("B") }
        optionC.setOnClickListener { onOptionSelected("C") }
        optionD.setOnClickListener { onOptionSelected("D") }

        // 设置提交按钮监听器
        btnSubmit.setOnClickListener {
            if (currentSelectedOption.isNotEmpty()) {
                onSubmitClickCallback?.invoke(currentQuestionId, currentSelectedOption)
            } else {
                Toast.makeText(itemView.context, "请选择一个答案", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onOptionSelected(option: String) {
        currentSelectedOption = option
        // 更新UI状态 (高亮选中项)
        updateOptionUI(option)
        // 调用外部回调
        onAnswerSelectedCallback?.invoke(currentQuestionId, option)
    }

    private fun updateOptionUI(selectedOption: String) {
        optionA.isSelected = (selectedOption == "A")
        optionB.isSelected = (selectedOption == "B")
        optionC.isSelected = (selectedOption == "C")
        optionD.isSelected = (selectedOption == "D")
    }

    private fun resetOptionStates() {
        optionA.isSelected = false
        optionB.isSelected = false
        optionC.isSelected = false
        optionD.isSelected = false
    }
}
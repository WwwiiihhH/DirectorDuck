package com.example.directorduck_v10.activities.quiz

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.directorduck_v10.R
import com.example.directorduck_v10.defItems.OptionItem
import com.example.directorduck_v10.fragments.practice.data.Question

class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvQuestionIndex: TextView = itemView.findViewById(R.id.tvQuestionIndex)
    private val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
    private val ivQuestionImage: ImageView = itemView.findViewById(R.id.ivQuestionImage)

    private val optionA: OptionItem = itemView.findViewById(R.id.optionA)
    private val optionB: OptionItem = itemView.findViewById(R.id.optionB)
    private val optionC: OptionItem = itemView.findViewById(R.id.optionC)
    private val optionD: OptionItem = itemView.findViewById(R.id.optionD)

    private val btnSubmit: Button = itemView.findViewById(R.id.btnSubmit)

    private var selectedOption: String? = null

    fun bind(
        question: Question,
        position: Int,
        totalCount: Int,
        onAnswerSelected: (questionId: Long, selectedOption: String) -> Unit,
        onSubmitClick: (questionId: Long, selectedOption: String) -> Unit
    ) {
        // 重置选择状态
        clearSelection()
        selectedOption = null

        // 设置题目序号
        tvQuestionIndex.text = "${position + 1}/$totalCount"

        // 设置题目内容
        tvQuestionText.text = question.questionText

        // 设置图片（如果有）
        if (!question.questionImage.isNullOrEmpty()) {
            ivQuestionImage.visibility = View.VISIBLE
            Glide.with(itemView.context)
                .load(question.questionImage)
                .into(ivQuestionImage)
        } else {
            ivQuestionImage.visibility = View.GONE
        }

        // 设置选项文本和标签
        optionA.tvLabel.text = "A"
        optionA.setText(question.optionA)

        optionB.tvLabel.text = "B"
        optionB.setText(question.optionB)

        optionC.tvLabel.text = "C"
        optionC.setText(question.optionC)

        optionD.tvLabel.text = "D"
        optionD.setText(question.optionD)

        // 设置选项选择监听器
        optionA.onOptionClicked = {
            selectedOption = "A"
            selectOption(optionA, optionB, optionC, optionD)
            onAnswerSelected(question.id, "A")
        }

        optionB.onOptionClicked = {
            selectedOption = "B"
            selectOption(optionB, optionA, optionC, optionD)
            onAnswerSelected(question.id, "B")
        }

        optionC.onOptionClicked = {
            selectedOption = "C"
            selectOption(optionC, optionA, optionB, optionD)
            onAnswerSelected(question.id, "C")
        }

        optionD.onOptionClicked = {
            selectedOption = "D"
            selectOption(optionD, optionA, optionB, optionC)
            onAnswerSelected(question.id, "D")
        }

        // 设置提交按钮点击监听器
        btnSubmit.setOnClickListener {
            if (selectedOption.isNullOrEmpty()) {
                onSubmitClick(question.id, "")
            } else {
                onSubmitClick(question.id, selectedOption!!)
            }
        }
    }

    private fun selectOption(
        selected: OptionItem,
        unselected1: OptionItem,
        unselected2: OptionItem,
        unselected3: OptionItem
    ) {
        selected.setSelected(true)
        unselected1.setSelected(false)
        unselected2.setSelected(false)
        unselected3.setSelected(false)
    }

    fun clearSelection() {
        optionA.setSelected(false)
        optionB.setSelected(false)
        optionC.setSelected(false)
        optionD.setSelected(false)
        selectedOption = null
    }
}
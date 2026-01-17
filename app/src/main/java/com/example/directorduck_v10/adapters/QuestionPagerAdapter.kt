package com.example.directorduck_v10.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.adapters.QuestionViewHolder // 修改导入路径
import com.example.directorduck_v10.fragments.practice.data.Question


class QuestionPagerAdapter(
    private val questions: List<Question>,
    private val onAnswerSelected: (questionId: Long, selectedOption: String) -> Unit,
    private val onSubmitClick: (questionId: Long, selectedOption: String) -> Unit,
    private val getSelectedOption: (questionId: Long) -> String?   // ✅ 新增
) : RecyclerView.Adapter<QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        val saved = getSelectedOption(question.id) // ✅ 取出已选项
        holder.bind(
            question = question,
            position = position,
            totalQuestions = questions.size,
            savedSelectedOption = saved,            // ✅ 传进去恢复 UI
            onAnswerSelected = onAnswerSelected,
            onSubmitClick = onSubmitClick
        )
    }

    override fun getItemCount(): Int = questions.size

    fun getQuestion(position: Int): Question? {
        return if (position >= 0 && position < questions.size) {
            questions[position]
        } else {
            null
        }
    }
}
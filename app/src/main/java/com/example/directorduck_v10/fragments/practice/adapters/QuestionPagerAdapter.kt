package com.example.directorduck_v10.fragments.practice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.activities.quiz.QuestionViewHolder
import com.example.directorduck_v10.fragments.practice.data.Question


class QuestionPagerAdapter(
    private val questions: List<Question>,
    private val onAnswerSelected: (questionId: Long, selectedOption: String) -> Unit,
    private val onSubmitClick: (questionId: Long, selectedOption: String) -> Unit
) : RecyclerView.Adapter<QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        holder.bind(question, position, questions.size, onAnswerSelected, onSubmitClick)
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
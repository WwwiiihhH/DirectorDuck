package com.example.directorduck_v10.adapters

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.model.FavoriteQuestionDetailDTO
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.defItems.OptionItem

class FavoriteQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvQuestionIndex: TextView = itemView.findViewById(R.id.tvQuestionIndex)
    private val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
    private val ivQuestionImage: ImageView = itemView.findViewById(R.id.ivQuestionImage)

    private val optionA: OptionItem = itemView.findViewById(R.id.optionA)
    private val optionB: OptionItem = itemView.findViewById(R.id.optionB)
    private val optionC: OptionItem = itemView.findViewById(R.id.optionC)
    private val optionD: OptionItem = itemView.findViewById(R.id.optionD)

    private val scrollRoot: NestedScrollView = itemView.findViewById(R.id.scrollRoot)
    private val llAnalysis: LinearLayout = itemView.findViewById(R.id.llAnalysis)
    private val tvAnalysis: TextView = itemView.findViewById(R.id.tvAnalysis)

    fun bind(
        question: FavoriteQuestionDetailDTO,
        position: Int,
        total: Int,
        savedSelected: String?,
        onAnswered: (selected: String) -> Unit
    ) {
        tvQuestionIndex.text = "${position + 1}/$total"
        tvQuestionText.text = question.questionText ?: ""

        // 图片
        val img = question.questionImage
        if (!img.isNullOrBlank()) {
            ivQuestionImage.visibility = View.VISIBLE
            val url = "${ApiClient.getQuizImageBaseUrl()}$img"
            Glide.with(ivQuestionImage.context)
                .load(url)
                .placeholder(R.drawable.logonew2)
                .error(R.drawable.logonew2)
                .into(ivQuestionImage)
        } else {
            ivQuestionImage.visibility = View.GONE
        }

        // 选项文本/标签
        optionA.setLabel("A"); optionA.setText(question.optionA ?: "")
        optionB.setLabel("B"); optionB.setText(question.optionB ?: "")
        optionC.setLabel("C"); optionC.setText(question.optionC ?: "")
        optionD.setLabel("D"); optionD.setText(question.optionD ?: "")

        // 每次 bind 先恢复为“未作答”样式
        resetAllOptions()
        hideAnalysis()

        val correct = (question.correctAnswer ?: "").trim()

        // ✅ 如果这题之前已经作答过（翻页回来要恢复）
        if (!savedSelected.isNullOrBlank() && correct.isNotBlank()) {
            applyJudgeUI(selected = savedSelected, correct = correct, analysis = question.analysis)
            disableAllOptions()
            return
        }

        // ✅ 未作答：绑定点击判题
        optionA.setOnClickListener { handleAnswer("A", correct, question, onAnswered) }
        optionB.setOnClickListener { handleAnswer("B", correct, question, onAnswered) }
        optionC.setOnClickListener { handleAnswer("C", correct, question, onAnswered) }
        optionD.setOnClickListener { handleAnswer("D", correct, question, onAnswered) }
        enableAllOptions()
    }

    private fun handleAnswer(
        selected: String,
        correct: String,
        question: FavoriteQuestionDetailDTO,
        onAnswered: (String) -> Unit
    ) {
        if (correct.isBlank()) return

        // 防止重复点：一旦显示解析就认为已作答
        if (llAnalysis.visibility == View.VISIBLE) return

        onAnswered(selected)

        applyJudgeUI(selected = selected, correct = correct, analysis = question.analysis)
        disableAllOptions()

        // 让解析尽量出现在视野里
        scrollRoot.post { scrollRoot.fullScroll(View.FOCUS_DOWN) }
    }

    private fun applyJudgeUI(selected: String, correct: String, analysis: String?) {
        // 先全部 normal
        resetAllOptions()

        if (selected == correct) {
            // 选对：该项绿
            getOption(selected).setReviewState(OptionItem.STATE_CORRECT)
        } else {
            // 选错：选的红 + 正确绿
            getOption(selected).setReviewState(OptionItem.STATE_WRONG)
            getOption(correct).setReviewState(OptionItem.STATE_CORRECT)
        }

        // 显示解析
        llAnalysis.visibility = View.VISIBLE
        tvAnalysis.text = "正确答案：$correct\n\n解析：${analysis ?: "暂无解析"}"
    }

    private fun getOption(letter: String): OptionItem {
        return when (letter) {
            "A" -> optionA
            "B" -> optionB
            "C" -> optionC
            "D" -> optionD
            else -> optionA
        }
    }

    private fun resetAllOptions() {
        optionA.setReviewState(OptionItem.STATE_NORMAL)
        optionB.setReviewState(OptionItem.STATE_NORMAL)
        optionC.setReviewState(OptionItem.STATE_NORMAL)
        optionD.setReviewState(OptionItem.STATE_NORMAL)
    }

    private fun hideAnalysis() {
        llAnalysis.visibility = View.GONE
        tvAnalysis.text = ""
    }

    private fun disableAllOptions() {
        optionA.isClickable = false
        optionB.isClickable = false
        optionC.isClickable = false
        optionD.isClickable = false
    }

    private fun enableAllOptions() {
        optionA.isClickable = true
        optionB.isClickable = true
        optionC.isClickable = true
        optionD.isClickable = true
    }
}
package com.example.directorduck_v10.feature.mockexam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.widget.OptionItem
import com.example.directorduck_v10.databinding.ItemWrongReviewPageBinding
import com.example.directorduck_v10.feature.mockexam.model.MockExamWrongQuestionDTO

class MockExamWrongReviewAdapter(
    private val items: List<MockExamWrongQuestionDTO>
) : RecyclerView.Adapter<MockExamWrongReviewAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // ✅ 复用之前的 Item 布局
        val binding = ItemWrongReviewPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    class VH(private val b: ItemWrongReviewPageBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: MockExamWrongQuestionDTO, position: Int, total: Int) {
            b.tvQuestionIndex.text = "${position + 1}/$total"

            // 模考是一次性拉取，没有 loading 状态
            b.llLoading.visibility = View.GONE
            b.tvError.visibility = View.GONE
            b.contentContainer.visibility = View.VISIBLE

            // 1. 题干
            b.tvQuestionText.text = item.questionText?.trim().orEmpty()

            // 2. 图片处理
            val rawImg = item.questionImage?.trim().orEmpty()
            val imgName = rawImg.split(",").firstOrNull()?.trim().orEmpty()
            if (imgName.isNotBlank()) {
                val fullUrl = ApiClient.getQuizImageBaseUrl() + imgName
                b.ivQuestionImage.visibility = View.VISIBLE
                Glide.with(b.ivQuestionImage.context)
                    .load(fullUrl)
                    .into(b.ivQuestionImage)
            } else {
                b.ivQuestionImage.visibility = View.GONE
            }

            // 3. 选项内容
            b.optionA.tvLabel.text = "A"
            b.optionB.tvLabel.text = "B"
            b.optionC.tvLabel.text = "C"
            b.optionD.tvLabel.text = "D"

            b.optionA.setText(item.optionA.orEmpty())
            b.optionB.setText(item.optionB.orEmpty())
            b.optionC.setText(item.optionC.orEmpty())
            b.optionD.setText(item.optionD.orEmpty())

            // 禁止点击
            b.optionA.disableToggle()
            b.optionB.disableToggle()
            b.optionC.disableToggle()
            b.optionD.disableToggle()

            // 4. 颜色逻辑 (标红/标绿)
            val correct = (item.correctAnswer ?: "").trim()
            val user = (item.userAnswer ?: "").trim()

            applyState(b.optionA, "A", user, correct)
            applyState(b.optionB, "B", user, correct)
            applyState(b.optionC, "C", user, correct)
            applyState(b.optionD, "D", user, correct)

            // 5. 底部解析区域
            b.tvUserAnswer.text = "你的选择：$user"
            b.tvCorrectAnswer.text = "正确答案：$correct"
            b.tvAnalysis.text = item.analysis?.trim().orEmpty()
        }

        private fun applyState(option: OptionItem, key: String, user: String, correct: String) {
            val ctx = option.context
            when {
                key == correct -> {
                    // 正确答案：绿色
                    option.setReviewState(OptionItem.STATE_CORRECT)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
                }
                key == user -> {
                    // 用户选错的：红色
                    option.setReviewState(OptionItem.STATE_WRONG)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
                }
                else -> {
                    // 其他：默认
                    option.setReviewState(OptionItem.STATE_NORMAL)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                }
            }
        }
    }
}
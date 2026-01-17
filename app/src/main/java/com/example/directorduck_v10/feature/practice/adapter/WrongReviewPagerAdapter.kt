package com.example.directorduck_v10.feature.practice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.directorduck_v10.feature.practice.model.WrongReviewItem
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ItemWrongReviewPageBinding
import com.example.directorduck_v10.core.widget.OptionItem

class WrongReviewPagerAdapter(
    private val items: MutableList<WrongReviewItem>
) : RecyclerView.Adapter<WrongReviewPagerAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWrongReviewPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    fun updateItem(index: Int, newItem: WrongReviewItem) {
        if (index < 0 || index >= items.size) return
        items[index] = newItem
        notifyItemChanged(index)
    }

    class VH(private val b: ItemWrongReviewPageBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: WrongReviewItem, position: Int, total: Int) {
            b.tvQuestionIndex.text = "${position + 1}/$total"

            // loading / error
            b.llLoading.visibility = if (item.loading) View.VISIBLE else View.GONE
            b.tvError.visibility = if (!item.loading && item.error != null) View.VISIBLE else View.GONE
            b.tvError.text = item.error ?: ""

            val detail = item.detail
            if (detail == null) {
                b.contentContainer.visibility = View.GONE
                return
            }
            b.contentContainer.visibility = View.VISIBLE

            b.tvQuestionText.text = detail.questionText.orEmpty()

            // 图片：后端可能是 "a.png,b.png"
            val rawImg = detail.questionImage?.trim().orEmpty()
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

            // 选项文本 + 标号（你的 OptionItem 布局里 label 默认有，但这里再保险写一下）
            b.optionA.tvLabel.text = "A"
            b.optionB.tvLabel.text = "B"
            b.optionC.tvLabel.text = "C"
            b.optionD.tvLabel.text = "D"

            b.optionA.setText(detail.optionA.orEmpty())
            b.optionB.setText(detail.optionB.orEmpty())
            b.optionC.setText(detail.optionC.orEmpty())
            b.optionD.setText(detail.optionD.orEmpty())

            // 禁止点击（只展示）
            b.optionA.disableToggle()
            b.optionB.disableToggle()
            b.optionC.disableToggle()
            b.optionD.disableToggle()

            // 标红/标绿
            val correct = (detail.correctAnswer ?: "").trim()
            val user = item.userAnswer.trim()

            applyState(b.optionA, "A", user, correct)
            applyState(b.optionB, "B", user, correct)
            applyState(b.optionC, "C", user, correct)
            applyState(b.optionD, "D", user, correct)

            b.tvUserAnswer.text = "你的选择：$user"
            b.tvCorrectAnswer.text = "正确答案：$correct"
            b.tvAnalysis.text = detail.analysis?.trim().orEmpty()
        }

        private fun applyState(option: OptionItem, key: String, user: String, correct: String) {
            val ctx = option.context
            when {
                key == correct -> {
                    option.setReviewState(OptionItem.STATE_CORRECT)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
                }
                key == user -> {
                    option.setReviewState(OptionItem.STATE_WRONG)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
                }
                else -> {
                    option.setReviewState(OptionItem.STATE_NORMAL)
                    option.tvText.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                }
            }
        }
    }
}

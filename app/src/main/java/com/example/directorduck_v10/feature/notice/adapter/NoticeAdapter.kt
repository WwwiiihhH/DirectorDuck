package com.example.directorduck_v10.feature.notice.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.feature.notice.model.Notice
import com.example.directorduck_v10.databinding.ItemNoticeBinding

class NoticeAdapter(
    private val notices: MutableList<Notice>,
    private val onItemClick: ((Notice) -> Unit)? = null
) : RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {

    inner class NoticeViewHolder(val binding: ItemNoticeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val binding = ItemNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        val notice = notices[position]
        with(holder.binding) {
            tvTitle.text = notice.title
            tvCategory.text = notice.category
            tvTime.text = formatTime(notice.publishTime)

            // 高级感处理：将数字变成蓝色
            tvRecruitCount.text = getHighlightedText("招录 ${notice.recruitCount} 人", notice.recruitCount.toString())
            tvPositionCount.text = getHighlightedText("岗位 ${notice.positionCount} 个", notice.positionCount.toString())

            root.setOnClickListener {
                onItemClick?.invoke(notice)
            }
        }
    }

    // 辅助函数：高亮文本中的数字部分
    private fun getHighlightedText(fullText: String, highlightPart: String): SpannableString {
        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(highlightPart)
        if (startIndex >= 0) {
            // 设置为鸭局长蓝 #3E54AC
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#3E54AC")),
                startIndex,
                startIndex + highlightPart.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // 可选：加粗数字
            // spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + highlightPart.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun formatTime(timeString: String): String {
        return try {
            if (timeString.length >= 10) {
                timeString.substring(0, 10).replace("-", ".")
            } else {
                timeString
            }
        } catch (e: Exception) {
            timeString
        }
    }

    override fun getItemCount(): Int = notices.size

    fun updateNotices(newNotices: List<Notice>) {
        notices.clear()
        notices.addAll(newNotices)
        notifyDataSetChanged()
    }
}
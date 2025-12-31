package com.example.directorduck_v10.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.data.model.Notice
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
            tvRecruitInfo.text = "招聘 ${notice.recruitCount} 人  ${notice.positionCount} 个岗位"
            tvCategory.text = notice.category
            tvTime.text = formatTime(notice.publishTime)

            root.setOnClickListener {
                onItemClick?.invoke(notice)
            }
        }
    }

    private fun formatTime(timeString: String): String {
        return try {
            // 将 "2025-11-03 00:00:00" 格式转换为 "2025.11.03"
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
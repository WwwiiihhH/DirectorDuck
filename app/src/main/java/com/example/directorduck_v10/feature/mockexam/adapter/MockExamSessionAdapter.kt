package com.example.directorduck_v10.feature.mockexam.adapter

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.directorduck_v10.R
import com.example.directorduck_v10.databinding.ItemMockExamSessionBinding
import com.example.directorduck_v10.feature.mockexam.model.MockExamSessionDTO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MockExamSessionAdapter(
    private val onJoinClick: (MockExamSessionDTO) -> Unit,
    private val onEnterClick: (MockExamSessionDTO) -> Unit,
    // ✅ 新增回调：点击查看报告
    private val onResultClick: (MockExamSessionDTO) -> Unit,
    private val onDescClick: ((MockExamSessionDTO) -> Unit)? = null
) : RecyclerView.Adapter<MockExamSessionAdapter.VH>() {

    private val data = mutableListOf<MockExamSessionDTO>()

    fun submitList(list: List<MockExamSessionDTO>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    // 更新人数
    fun updateCount(sessionId: Long, count: Long) {
        val idx = data.indexOfFirst { it.id == sessionId }
        if (idx >= 0) {
            data[idx].joinCount = count
            notifyItemChanged(idx)
        }
    }

    // 更新报名状态 (只改 joined，不改 completed，因为报名的一瞬间肯定没考完)
    fun updateJoined(sessionId: Long, joined: Boolean) {
        val idx = data.indexOfFirst { it.id == sessionId }
        if (idx >= 0) {
            data[idx].joined = joined
            notifyItemChanged(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMockExamSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        // ✅ 传递 onResultClick
        return VH(binding, onJoinClick, onEnterClick, onResultClick, onDescClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    override fun getItemCount(): Int = data.size

    class VH(
        private val binding: ItemMockExamSessionBinding,
        private val onJoinClick: (MockExamSessionDTO) -> Unit,
        private val onEnterClick: (MockExamSessionDTO) -> Unit,
        private val onResultClick: (MockExamSessionDTO) -> Unit, // 接收
        private val onDescClick: ((MockExamSessionDTO) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MockExamSessionDTO) {
            binding.tvSessionTitle.text = item.title
            val timeLine = buildChineseTimeLine(item.startTime, item.endTime)
            binding.tvExamTimeLine.text = "行测：$timeLine"

            // ✅ 根据 joined 和 isCompleted 渲染不同按钮样式
            renderJoinButton(item)

            binding.layoutDesc.setOnClickListener { onDescClick?.invoke(item) }

            // ✅ 核心点击逻辑
            binding.btnJoinContainer.setOnClickListener {
                when {
                    item.isCompleted -> onResultClick(item) // 已考完 -> 看结果
                    item.joined -> onEnterClick(item)       // 已报名未考 -> 进考场
                    else -> onJoinClick(item)               // 未报名 -> 报名
                }
            }

            setJoinCountText(item.joinCount)
        }

        private fun renderJoinButton(item: MockExamSessionDTO) {
            val ctx = binding.root.context

            when {
                // 1. 已完成：显示“查看模考报告” (绿色)
                item.isCompleted -> {
                    // 使用胶囊背景
                    binding.btnJoinContainer.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_enter_exam_pill)

                    binding.ivJoinIcon.visibility = View.VISIBLE
                    binding.ivJoinIcon.setImageResource(R.drawable.logonew2) // 或换成报表图标

                    binding.tvJoinText.text = "查看模考报告"
                    // 设置为绿色 (#10B981) 以示区别
                    binding.tvJoinText.setTextColor(Color.parseColor("#10B981"))
                }

                // 2. 已报名：显示“进入考试” (橙色)
                item.joined -> {
                    binding.btnJoinContainer.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_enter_exam_pill)

                    binding.ivJoinIcon.visibility = View.VISIBLE
                    binding.ivJoinIcon.setImageResource(R.drawable.logonew2)

                    binding.tvJoinText.text = "进入考试"
                    binding.tvJoinText.setTextColor(Color.parseColor("#FF7A1A"))
                }

                // 3. 未报名：显示“立即报名” (橙色渐变实心)
                else -> {
                    binding.btnJoinContainer.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_orange_gradient_btn)

                    binding.ivJoinIcon.visibility = View.GONE

                    binding.tvJoinText.text = "立即报名"
                    binding.tvJoinText.setTextColor(Color.WHITE)
                }
            }
        }

        // ... setJoinCountText, buildChineseTimeLine, parseIsoToDate 保持不变 ...
        private fun setJoinCountText(count: Long) {
            val prefix = "已有 "
            val num = count.toString()
            val suffix = " 人报名"
            val full = prefix + num + suffix
            val ss = SpannableString(full)
            val gray = Color.parseColor("#9CA3AF")
            ss.setSpan(ForegroundColorSpan(gray), 0, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val orange = Color.parseColor("#FF7A1A")
            val start = prefix.length
            val end = start + num.length
            ss.setSpan(ForegroundColorSpan(orange), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.tvJoinCount.text = ss
        }

        private fun buildChineseTimeLine(startIso: String, endIso: String): String {
            val start = parseIsoToDate(startIso) ?: return startIso.replace('T', ' ')
            val end = parseIsoToDate(endIso)
            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
            val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)
            val weekFmt = SimpleDateFormat("E", Locale.CHINA)
            val date = dateFmt.format(start)
            val week = weekFmt.format(start)
            val st = timeFmt.format(start)
            val et = if (end != null) timeFmt.format(end) else ""
            return if (et.isNotEmpty()) "$date（$week）$st-$et" else "$date（$week）$st"
        }

        private fun parseIsoToDate(iso: String): Date? {
            return runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                fmt.parse(iso)
            }.getOrNull()
        }
    }
}
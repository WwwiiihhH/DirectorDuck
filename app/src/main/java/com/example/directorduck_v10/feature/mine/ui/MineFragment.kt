package com.example.directorduck_v10.feature.mine.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.directorduck_v10.core.state.SharedUserViewModel
import com.example.directorduck_v10.databinding.FragmentMineBinding
import com.example.directorduck_v10.feature.mine.viewmodel.MineViewModel
import com.example.directorduck_v10.feature.report.ui.ReportActivity
import java.text.SimpleDateFormat
import java.util.Locale

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()
    private val viewModel: MineViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 观察用户信息
        sharedUserViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvUsername.text = user?.username ?: "未登录"
            if (user != null) {
                viewModel.loadLatestExam(user.id)
            }
        }

        // 2. 观察模考数据
        viewModel.targetSession.observe(viewLifecycleOwner) { session ->
            if (session != null) {
                binding.cv3.visibility = View.VISIBLE
                binding.tvExamTitle.text = session.title
                binding.tvExamTime.text = formatDisplayTime(session.startTime)
            } else {
                // 如果没有符合条件（未报名）的考试
                binding.tvExamTitle.text = "暂无待预约模考"
                binding.tvExamTime.text = "您已预约所有近期考试或暂无计划"
                // 隐藏预约按钮，或者改为置灰
                binding.btnReserve.visibility = View.GONE
            }
        }

        // 3. 观察报名状态
        viewModel.isRegistered.observe(viewLifecycleOwner) { isJoined ->
            val btn = binding.btnReserve
            if (isJoined) {
                btn.text = "已报名"
                btn.setBackgroundColor(Color.parseColor("#B0BEC5"))
                btn.isEnabled = false
            } else {
                btn.text = "立即预约"
                btn.setBackgroundColor(Color.parseColor("#3E54AC"))
                btn.isEnabled = true
            }
            // 只要有数据，按钮就应该显示（除非上面targetSession为空隐藏了）
            if (viewModel.targetSession.value != null) {
                btn.visibility = View.VISIBLE
            }
        }

        // 4. Toast
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() = binding.run {
        // ... 其他点击事件 ...

        // 3) 模考卡片点击
        cv3.setOnClickListener {
            // 这里可以跳转到模考列表页，或者根据状态跳转
            // startActivity(Intent(context, MockExamListActivity::class.java))
        }

        // ✅ 关键修复：预约按钮点击事件
        btnReserve.setOnClickListener {
            val user = sharedUserViewModel.user.value
            if (user != null) {
                // 调用 ViewModel 的报名方法
                viewModel.reserveExam(user.id, user.username)
            } else {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            }
        }

        // ... 菜单点击事件 ...
        itemReport.setOnClickListener {
            startActivity(Intent(requireContext(), ReportActivity::class.java))
        }
    }

    private fun formatDisplayTime(rawTime: String?): String {
        if (rawTime.isNullOrEmpty()) return "时间待定"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM月dd日 HH:mm 开考", Locale.getDefault())
            val date = inputFormat.parse(rawTime)
            outputFormat.format(date ?: return rawTime)
        } catch (e: Exception) {
            rawTime
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
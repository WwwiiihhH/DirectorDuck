package com.example.directorduck_v10.feature.mockexam.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.isOk
import com.example.directorduck_v10.databinding.ActivityMockExamListBinding
import com.example.directorduck_v10.feature.mockexam.adapter.MockExamSessionAdapter
import com.example.directorduck_v10.feature.mockexam.model.MockExamJoinRequest
import com.example.directorduck_v10.feature.mockexam.model.MockExamSessionDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

class MockExamListActivity : BaseActivity() {

    private lateinit var binding: ActivityMockExamListBinding

    private val userId: Long by lazy { intent.getLongExtra(EXTRA_USER_ID, -1L) }
    private val username: String by lazy { intent.getStringExtra(EXTRA_USERNAME).orEmpty() }

    private var countdownDialog: AlertDialog? = null
    private var countdownTimer: CountDownTimer? = null

    // ✅ 修改点 1：适配器增加了 onResultClick 回调
    private val adapter: MockExamSessionAdapter by lazy {
        MockExamSessionAdapter(
            onJoinClick = { session -> joinSession(session.id) },
            onEnterClick = { session -> handleEnter(session) },
            onResultClick = { session -> handleViewResult(session) }, // 新增：查看报告
            onDescClick = { session ->
                Toast.makeText(this, "查看考试说明：${session.title}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockExamListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerSessions.layoutManager = LinearLayoutManager(this)
        binding.recyclerSessions.adapter = adapter
        binding.ivBack.setOnClickListener { finish() }

        if (userId <= 0 || username.isBlank()) {
            Toast.makeText(this, "请先登录后再进入模考", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSessions()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        countdownDialog?.dismiss()
        countdownTimer = null
        countdownDialog = null
    }

    /**
     * ✅ 新展示规则：
     * - 已报名：只要未结束(now < endTime)就展示（含进行中）
     * - 未报名：仅在未过报名截止且未开考时展示
     * - 报名截止且未报名：不展示
     * - 已结束：不展示
     */
    private fun loadSessions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.mockExamService.listSessions()
                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, "服务器错误: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = resp.body()
                // 注意：这里使用 isOk() 或者是你封装的 isSuccess()，确保 ApiResponse 类里有这个方法
                if (body == null || !body.isOk()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, body?.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val allSessions = body.data ?: emptyList()

                // 先粗过滤：已结束的肯定不展示（无论是否报名）
                val notEnded = allSessions.filter { s ->
                    val endMs = parseIsoLocalDateTime(s.endTime) ?: return@filter false
                    now < endMs
                }

                // ✅ 修改点 2：改为填充 Joined 和 Completed 状态
                val withStatus = fillJoinedAndCompletedState(notEnded)

                val displayList = withStatus.filter { s ->
                    val startMs = parseIsoLocalDateTime(s.startTime) ?: return@filter false
                    val endMs = parseIsoLocalDateTime(s.endTime) ?: return@filter false
                    val deadlineMs = s.registerDeadline?.let { parseIsoLocalDateTime(it) }

                    // 再兜底一次：未结束
                    if (now >= endMs) return@filter false

                    if (s.joined) {
                        // ✅ 已报名：未结束就展示（允许考试中途进入 或 查看结果）
                        true
                    } else {
                        // ✅ 未报名：必须报名未截止 + 且未开考
                        val deadlineOk = deadlineMs == null || now <= deadlineMs
                        val notStarted = now < startMs
                        deadlineOk && notStarted
                    }
                }.sortedBy { parseIsoLocalDateTime(it.startTime) ?: Long.MAX_VALUE }

                withContext(Dispatchers.Main) {
                    adapter.submitList(displayList)
                }

                // 加载报名人数
                loadCountsFor(displayList)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamListActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * ✅ 修改点 3：填充 joined 状态，如果已 joined，再查 checkCompletion
     */
    private suspend fun fillJoinedAndCompletedState(list: List<MockExamSessionDTO>): List<MockExamSessionDTO> {
        val result = ArrayList<MockExamSessionDTO>(list.size)
        for (s in list) {
            // 1. 查是否已报名
            val joined = runCatching {
                val r = ApiClient.mockExamService.exists(s.id, userId)
                r.isSuccessful && r.body()?.isOk() == true && (r.body()?.data == true)
            }.getOrDefault(false)

            s.joined = joined
            s.isCompleted = false // 默认初始化

            // 2. 如果已报名，进一步查是否已交卷
            if (joined) {
                val completed = runCatching {
                    val r = ApiClient.mockExamService.checkCompletion(s.id, userId)
                    r.isSuccessful && r.body()?.isOk() == true && (r.body()?.data == true)
                }.getOrDefault(false)

                s.isCompleted = completed
            }

            result.add(s)
        }
        return result
    }

    /**
     * 点击“立即报名”
     */
    private fun joinSession(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.mockExamService.join(
                    sessionId = sessionId,
                    req = MockExamJoinRequest(userId = userId, username = username)
                )

                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, "服务器错误: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = resp.body()
                if (body == null || !body.isOk()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, body?.message ?: "报名失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    adapter.updateJoined(sessionId, true)
                    Toast.makeText(this@MockExamListActivity, "报名成功", Toast.LENGTH_SHORT).show()
                }

                refreshCount(sessionId)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamListActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCountsFor(list: List<MockExamSessionDTO>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (s in list) {
                val count = fetchCountSafe(s.id)
                withContext(Dispatchers.Main) { adapter.updateCount(s.id, count) }
            }
        }
    }

    private fun refreshCount(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val count = fetchCountSafe(sessionId)
            withContext(Dispatchers.Main) { adapter.updateCount(sessionId, count) }
        }
    }

    private suspend fun fetchCountSafe(sessionId: Long): Long {
        return runCatching {
            val r = ApiClient.mockExamService.countParticipants(sessionId)
            if (r.isSuccessful && r.body()?.isOk() == true) r.body()?.data ?: 0L else 0L
        }.getOrDefault(0L)
    }

    /**
     * ✅ 修改点 4：新增 - 处理“查看报告”点击
     */
    private fun handleViewResult(session: MockExamSessionDTO) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 调用接口获取成绩详情
                val resp = ApiClient.mockExamService.getResult(session.id, userId)

                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, "获取成绩失败: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = resp.body()
                val resultData = body?.data

                withContext(Dispatchers.Main) {
                    if (body != null && body.isOk() && resultData != null) {
                        // 2. 跳转到结果页
                        // 注意：这里 elapsedMillis 传 0 即可，因为列表页不知道具体答题耗时
                        // 结果页会显示后端返回的提交时间
                        MockExamResultActivity.start(
                            context = this@MockExamListActivity,
                            result = resultData,
                            elapsedMillis = 0L,
                            sessionTitle = session.title
                        )
                    } else {
                        Toast.makeText(this@MockExamListActivity, body?.message ?: "未查询到成绩", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamListActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 点击“进入考试”
     */
    private fun handleEnter(session: MockExamSessionDTO) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val statusResp = ApiClient.mockExamService.status(session.id, userId)
                if (!statusResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, "服务器错误: ${statusResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = statusResp.body()
                val status = body?.data
                if (body == null || !body.isOk() || status == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamListActivity, body?.message ?: "获取状态失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                when (status.status) {
                    0 -> { // 未开始
                        val remainSec = status.remainSecondsToStart ?: run {
                            val startMs = parseIsoLocalDateTime(status.startTime) ?: 0L
                            val serverNowMs = parseIsoLocalDateTime(status.serverNow) ?: System.currentTimeMillis()
                            max(0L, (startMs - serverNowMs) / 1000L)
                        }
                        withContext(Dispatchers.Main) {
                            showCountdownDialog(
                                sessionTitle = session.title,
                                startTimeIso = status.startTime,
                                remainSeconds = remainSec,
                                onFinish = { handleEnter(session) }
                            )
                        }
                    }

                    1 -> { // 进行中
                        val enterResp = ApiClient.mockExamService.enter(session.id, userId)
                        withContext(Dispatchers.Main) {
                            if (enterResp.isSuccessful) {
                                val enterBody = enterResp.body()
                                val data = enterBody?.data
                                if (enterBody != null && enterBody.isOk() && data != null) {
                                    if (data.allowed) {
                                        Toast.makeText(this@MockExamListActivity, "允许进入：正在进入模考", Toast.LENGTH_SHORT).show()
                                        MockExamQuizActivity.start(
                                            context = this@MockExamListActivity,
                                            sessionId = session.id,
                                            sessionTitle = session.title,
                                            userId = userId,
                                            username = username
                                        )
                                    } else {
                                        Toast.makeText(this@MockExamListActivity, data.message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@MockExamListActivity, enterBody?.message ?: "进入失败", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@MockExamListActivity, "服务器错误: ${enterResp.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MockExamListActivity, "模考已结束，无法进入", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamListActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCountdownDialog(
        sessionTitle: String,
        startTimeIso: String,
        remainSeconds: Long,
        onFinish: () -> Unit
    ) {
        countdownTimer?.cancel()
        countdownDialog?.dismiss()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_mock_exam_countdown, null, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubTitle = view.findViewById<TextView>(R.id.tvSubTitle)
        val tvRemain = view.findViewById<TextView>(R.id.tvRemain)
        val tvStartTime = view.findViewById<TextView>(R.id.tvStartTime)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        tvTitle.text = "尚未开考"
        tvSubTitle.text = sessionTitle
        tvStartTime.text = "开考时间：${startTimeIso.replace('T', ' ').take(16)}"

        val totalMs = max(0L, remainSeconds) * 1000L
        progress.max = 100

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .setPositiveButton("我知道了") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnDismissListener {
            countdownTimer?.cancel()
            countdownTimer = null
            countdownDialog = null
        }

        countdownDialog = dialog
        dialog.show()

        fun render(ms: Long) {
            val sec = max(0L, ms / 1000L)
            tvRemain.text = formatHms(sec)

            if (totalMs > 0) {
                val percent = ((totalMs - ms).toFloat() / totalMs.toFloat() * 100f).toInt()
                progress.progress = min(100, max(0, percent))
            } else {
                progress.progress = 100
            }
        }

        render(totalMs)

        countdownTimer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) = render(millisUntilFinished)
            override fun onFinish() {
                render(0L)
                if (countdownDialog?.isShowing == true) countdownDialog?.dismiss()
                onFinish()
            }
        }.start()
    }

    private fun formatHms(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    private fun parseIsoLocalDateTime(s: String): Long? {
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            fmt.timeZone = TimeZone.getDefault()
            fmt.parse(s)?.time
        }.getOrNull()
    }

    companion object {
        private const val EXTRA_USER_ID = "userId"
        private const val EXTRA_USERNAME = "username"

        fun start(context: android.content.Context, userId: Long, username: String) {
            val it = Intent(context, MockExamListActivity::class.java)
            it.putExtra(EXTRA_USER_ID, userId)
            it.putExtra(EXTRA_USERNAME, username)
            context.startActivity(it)
        }
    }
}
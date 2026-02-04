package com.example.directorduck_v10.feature.mockexam.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.isOk
import com.example.directorduck_v10.databinding.ActivityMockExamHistoryBinding
import com.example.directorduck_v10.feature.mockexam.adapter.MockExamSessionAdapter
import com.example.directorduck_v10.feature.mockexam.model.MockExamResultDTO
import com.example.directorduck_v10.feature.mockexam.model.MockExamSessionDTO
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

class MockExamHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityMockExamHistoryBinding

    private val userId: Long by lazy { intent.getLongExtra(EXTRA_USER_ID, -1L) }
    private val username: String by lazy { intent.getStringExtra(EXTRA_USERNAME).orEmpty() }

    private val resultCache = mutableMapOf<Long, MockExamResultDTO>()

    private val adapter: MockExamSessionAdapter by lazy {
        MockExamSessionAdapter(
            onJoinClick = { },
            onEnterClick = { },
            onResultClick = { session -> handleViewResult(session) },
            onDescClick = { session ->
                Toast.makeText(this, "查看考试说明：${session.title}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockExamHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        setupLineChartBase()

        if (userId <= 0 || username.isBlank()) {
            Toast.makeText(this, "请先登录后再查看模考历史", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadHistory()
    }

    private fun loadHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.mockExamService.listSessions()
                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamHistoryActivity, "服务器错误: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = resp.body()
                if (body == null || !body.isOk()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamHistoryActivity, body?.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val sessions = body.data ?: emptyList()
                val completed = mutableListOf<Pair<MockExamSessionDTO, MockExamResultDTO?>>()

                for (s in sessions) {
                    val isCompleted = runCatching {
                        val r = ApiClient.mockExamService.checkCompletion(s.id, userId)
                        r.isSuccessful && r.body()?.isOk() == true && (r.body()?.data == true)
                    }.getOrDefault(false)

                    if (isCompleted) {
                        s.isCompleted = true
                        val result = fetchResultSafe(s.id)
                        if (result != null) {
                            resultCache[s.id] = result
                        }
                        completed.add(s to result)
                    }
                }

                val sorted = completed.sortedBy { pair ->
                    val resultTime = parseIsoLocalDateTime(pair.second?.submittedAt)
                    resultTime ?: parseIsoLocalDateTime(pair.first.startTime) ?: Long.MAX_VALUE
                }

                val sortedSessions = sorted.map { it.first }
                val results = sorted.mapNotNull { it.second }

                withContext(Dispatchers.Main) {
                    adapter.submitList(sortedSessions)
                    renderScoreChart(results)
                }

                loadCountsFor(sortedSessions)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamHistoryActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun fetchResultSafe(sessionId: Long): MockExamResultDTO? {
        return runCatching {
            val resp = ApiClient.mockExamService.getResult(sessionId, userId)
            if (resp.isSuccessful && resp.body()?.isOk() == true) resp.body()?.data else null
        }.getOrNull()
    }

    private fun handleViewResult(session: MockExamSessionDTO) {
        val cached = resultCache[session.id]
        if (cached != null) {
            MockExamResultActivity.start(
                context = this@MockExamHistoryActivity,
                result = cached,
                elapsedMillis = 0L,
                sessionTitle = session.title
            )
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.mockExamService.getResult(session.id, userId)
                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MockExamHistoryActivity, "获取成绩失败: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = resp.body()
                val resultData = body?.data
                withContext(Dispatchers.Main) {
                    if (body != null && body.isOk() && resultData != null) {
                        resultCache[session.id] = resultData
                        MockExamResultActivity.start(
                            context = this@MockExamHistoryActivity,
                            result = resultData,
                            elapsedMillis = 0L,
                            sessionTitle = session.title
                        )
                    } else {
                        Toast.makeText(this@MockExamHistoryActivity, body?.message ?: "未查询到成绩", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MockExamHistoryActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private suspend fun fetchCountSafe(sessionId: Long): Long {
        return runCatching {
            val r = ApiClient.mockExamService.countParticipants(sessionId)
            if (r.isSuccessful && r.body()?.isOk() == true) r.body()?.data ?: 0L else 0L
        }.getOrDefault(0L)
    }

    private fun setupLineChartBase() = binding.lineScoreTrend.run {
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(false)
        setPinchZoom(false)
        description.isEnabled = false
        legend.isEnabled = false
        setNoDataText("暂无模考成绩")

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            textColor = Color.parseColor("#9CA3AF")
            textSize = 10f
            granularity = 1f
        }

        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F3F4F6")
            setDrawAxisLine(false)
            textColor = Color.parseColor("#9CA3AF")
            textSize = 10f
        }

        axisRight.isEnabled = false
    }

    private fun renderScoreChart(results: List<MockExamResultDTO>) = binding.lineScoreTrend.run {
        if (results.isEmpty()) {
            clear()
            invalidate()
            return@run
        }

        val scores = results.map { it.score.toFloat() }
        val entries = scores.mapIndexed { idx, v -> Entry(idx.toFloat(), v) }

        val dataSet = LineDataSet(entries, "").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            color = Color.parseColor("#3B82F6")
            lineWidth = 3f
            setDrawCircles(false)
            setDrawCircleHole(false)
            setDrawHighlightIndicators(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#803B82F6"), Color.TRANSPARENT)
            )
        }

        val labels = buildXAxisLabels(results)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.setLabelCount(min(5, labels.size), true)

        val minScore = scores.minOrNull() ?: 0f
        val maxScore = scores.maxOrNull() ?: 100f
        axisLeft.axisMinimum = max(0f, minScore - 5f)
        axisLeft.axisMaximum = min(100f, maxScore + 5f)

        data = LineData(dataSet)
        animateXY(600, 600)
        invalidate()
    }

    private fun buildXAxisLabels(results: List<MockExamResultDTO>): List<String> {
        val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
        return results.mapIndexed { index, r ->
            val ms = parseIsoLocalDateTime(r.submittedAt)
            if (ms != null) fmt.format(Date(ms)) else (index + 1).toString()
        }
    }

    private fun parseIsoLocalDateTime(s: String?): Long? {
        if (s.isNullOrBlank()) return null
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
            val it = Intent(context, MockExamHistoryActivity::class.java)
            it.putExtra(EXTRA_USER_ID, userId)
            it.putExtra(EXTRA_USERNAME, username)
            context.startActivity(it)
        }
    }
}

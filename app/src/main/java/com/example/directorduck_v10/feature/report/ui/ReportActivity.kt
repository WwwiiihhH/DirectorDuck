package com.example.directorduck_v10.feature.report.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.databinding.ActivityReportBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ReportActivity : BaseActivity() {

    private lateinit var binding: ActivityReportBinding

    // 定义高级感配色
    private val colorPrimary = Color.parseColor("#3B82F6") // 现代蓝
    private val colorPrimaryLight = Color.parseColor("#EFF6FF") // 极浅蓝
    private val colorTextMain = Color.parseColor("#1F2937") // 深灰黑
    private val colorTextSub = Color.parseColor("#9CA3AF") // 浅灰
    private val colorGrid = Color.parseColor("#F3F4F6") // 极淡的网格线

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }
        renderModernReport()
    }

    private fun renderModernReport() {
        // 1. Dashboard 数据填充
        val total = 320
        val correct = 238
        val accuracy = (correct * 100f / total).toInt()

        binding.tvTotalCount.text = total.toString()
        binding.tvAccuracy.text = "$accuracy%"
        binding.tvStreak.text = "6"

        // 2. 现代折线图：渐变填充 + 平滑曲线
        setupModernLineChart(
            listOf(52f, 58f, 60f, 63f, 64f, 66f, 65f, 67f, 70f, 72f, 74f, 76f)
        )

        // 3. 极简饼图：只突出“正确”部分
        setupMinimalPieChart(correct, total - correct)

        // 4. 条形图：清晰展示模块短板
        setupHorizontalBarChart(
            listOf(
                "常识" to 55f, // 倒序排列，因为 HorizontalBarChart 从下往上画
                "数量" to 62f,
                "资料" to 69f,
                "判断" to 71f,
                "言语" to 78f
            )
        )
    }

    private fun setupModernLineChart(values: List<Float>) = binding.lineTrend.run {
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(false)
        setPinchZoom(false)
        description.isEnabled = false
        legend.isEnabled = false // 只有一个趋势，不需要图例

        // X轴样式
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false) // 去掉竖向网格
            setDrawAxisLine(false)
            textColor = colorTextSub
            textSize = 10f
            granularity = 1f
            // 简化的 X 轴标签，每隔几天显示一个，或者是首尾
            valueFormatter = object : IndexAxisValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    return if (idx % 3 == 0) "${idx + 1}日" else ""
                }
            }
        }

        // Y轴样式
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = colorGrid
            setDrawAxisLine(false) // 去掉Y轴竖线
            textColor = colorTextSub
            textSize = 10f
            axisMinimum = 40f // 设置一个合理的最小值，让曲线波动看起来更明显
            axisMaximum = 90f
        }
        axisRight.isEnabled = false

        val entries = values.mapIndexed { idx, v -> Entry(idx.toFloat(), v) }

        val dataSet = LineDataSet(entries, "").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER // 贝塞尔曲线，平滑
            cubicIntensity = 0.2f
            color = colorPrimary
            lineWidth = 3f

            setDrawCircles(false) // 平时隐藏小圆点
            setDrawCircleHole(false)
            setDrawHighlightIndicators(false)

            setDrawValues(false) // 不显示具体的数值，保持干净

            // 关键：设置渐变填充
            setDrawFilled(true)
            // 在 XML drawable 中定义 fade_blue.xml，或者用代码生成 GradientDrawable
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#803B82F6"), Color.TRANSPARENT)
            )
            fillDrawable = gradientDrawable
        }

        data = LineData(dataSet)
        animateXY(800, 800)
        invalidate()
    }

    private fun setupMinimalPieChart(correct: Int, wrong: Int) = binding.pieCorrectWrong.run {
        description.isEnabled = false
        legend.isEnabled = false
        setUsePercentValues(true)

        isDrawHoleEnabled = true
        holeRadius = 75f // 环更细，显得更精致
        transparentCircleRadius = 0f
        setHoleColor(Color.WHITE)

        // 中间显示文字
        centerText = "正确率\n${(correct * 100f / (correct + wrong)).toInt()}%"
        setCenterTextSize(12f)
        setCenterTextColor(colorTextSub)

        val entries = listOf(
            PieEntry(correct.toFloat(), ""), // 不显示 label
            PieEntry(wrong.toFloat(), "")
        )

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 0f
            selectionShift = 5f
            // 只有两个颜色：主色 和 浅灰底色
            colors = listOf(colorPrimary, Color.parseColor("#F3F4F6"))
            setDrawValues(false) // 环上不显示字，字在中间
        }

        data = PieData(dataSet)
        animateY(800)
        invalidate()
    }

    private fun setupHorizontalBarChart(items: List<Pair<String, Float>>) = binding.barByCategory.run {
        description.isEnabled = false
        legend.isEnabled = false
        setTouchEnabled(false) // 禁止交互，作为纯展示
        setDrawGridBackground(false)

        val labels = items.map { it.first }
        val entries = items.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second)
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = colorPrimary // 单一色调，比彩虹色高级
            valueTextColor = colorTextMain
            valueTextSize = 10f
            setDrawValues(true) // 条形图末尾显示数值
        }

        data = BarData(dataSet).apply {
            barWidth = 0.4f // 细一点的条形
        }

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            textColor = colorTextMain
            textSize = 11f
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(labels)
        }

        axisLeft.isEnabled = false // 隐藏上方的轴数值
        axisRight.isEnabled = false

        // 可以在这里设置 AxisMinimum = 0f

        animateY(800)
        invalidate()
    }
}
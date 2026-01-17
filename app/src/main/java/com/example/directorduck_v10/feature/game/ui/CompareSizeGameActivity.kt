package com.example.directorduck_v10.feature.game.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.directorduck_v10.R
import com.example.directorduck_v10.databinding.ActivityCompareSizeGameBinding
import com.example.directorduck_v10.feature.game.adapter.CompareWrongAdapter
import com.example.directorduck_v10.feature.game.model.CompareQuestion
import com.example.directorduck_v10.feature.game.model.Fraction
import com.example.directorduck_v10.feature.game.model.WrongEntry
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

class CompareSizeGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompareSizeGameBinding

    private val handler = Handler(Looper.getMainLooper())
    private var startRealtime = 0L
    private var elapsed = 0L
    private var timerRunning = false

    private var totalCount = 20
    private val questions = mutableListOf<CompareQuestion>()
    private var index = 0

    private var correctCount = 0
    private var wrongCount = 0
    private val wrongList = mutableListOf<WrongEntry>()

    private var locked = false // 提交后到切题前锁住

    // ✅ 默认 3~6 位难度（分子分母的位数）
    private var minDigits = 3
    private var maxDigits = 6

    companion object {
        const val EXTRA_TOTAL_COUNT = "extra_total_count"
        const val EXTRA_MIN_DIGITS = "extra_min_digits"
        const val EXTRA_MAX_DIGITS = "extra_max_digits"
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!timerRunning) return
            elapsed = SystemClock.elapsedRealtime() - startRealtime
            binding.tvTimer.text = formatTime(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompareSizeGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalCount = intent.getIntExtra(EXTRA_TOTAL_COUNT, 20).coerceIn(5, 200)
        minDigits = intent.getIntExtra(EXTRA_MIN_DIGITS, 3).coerceIn(1, 9)
        maxDigits = intent.getIntExtra(EXTRA_MAX_DIGITS, 6).coerceIn(minDigits, 9)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { binding.handwritePad.clear() }
        binding.btnUndo.setOnClickListener { binding.handwritePad.undo() }

        // ✅ 抬笔即识别
        binding.handwritePad.onStrokeUp = strokeUp@{ bmp ->
            if (locked) return@strokeUp

            val symbol = recognizeSymbol(bmp)
            if (symbol != null) {
                submit(symbol)
            } else {
                // 失败：提示并让用户重写
                Toast.makeText(this, "识别失败，请写得更大、更尖一点", Toast.LENGTH_SHORT).show()
                binding.handwritePad.clear()
            }
        }


        startNewGame()
    }

    private fun startNewGame() {
        correctCount = 0
        wrongCount = 0
        wrongList.clear()
        index = 0
        locked = false

        questions.clear()
        questions.addAll(generateQuestions(totalCount, minDigits, maxDigits))

        // timer
        elapsed = 0L
        startRealtime = SystemClock.elapsedRealtime()
        timerRunning = true
        handler.removeCallbacks(tick)
        handler.post(tick)

        render()
    }

    private fun render() {
        val cur = questions[index]
        val next = questions.getOrNull(index + 1)

        // 顶部：当前题
        binding.tvProgress.text = "${index + 1}/$totalCount"
        binding.fvLeftCur.setFraction(cur.left)
        binding.fvRightCur.setFraction(cur.right)

        binding.tvCircleCur.text = "?"
        binding.tvCircleCur.setBackgroundResource(R.drawable.bg_compare_circle)

        // 中部：下一题预览
        if (next != null) {
            binding.tvNextLabel.text = "下一题"
            binding.fvLeftNext.setFraction(next.left)
            binding.fvRightNext.setFraction(next.right)
            binding.tvCircleNext.text = "?"
        } else {
            binding.tvNextLabel.text = "已是最后一题"
            binding.fvLeftNext.setFraction(Fraction(0, 1))
            binding.fvRightNext.setFraction(Fraction(0, 1))
            binding.tvCircleNext.text = "?"
        }

        // 清空写板
        binding.handwritePad.clear()
    }

    private fun askUserPickSymbol() {
        val items = arrayOf(">", "<", "重写这一笔")
        AlertDialog.Builder(this)
            .setTitle("识别失败")
            .setMessage("没识别出来，你可以手动选一下（后续可继续调优识别算法）")
            .setItems(items) { d, which ->
                when (which) {
                    0 -> submit(">")
                    1 -> submit("<")
                    else -> binding.handwritePad.clear()
                }
                d.dismiss()
            }
            .show()
    }

    private fun submit(symbol: String) {
        if (locked) return
        locked = true

        val q = questions[index]
        val correct = q.correctSymbol

        binding.tvCircleCur.text = symbol

        val isRight = symbol == correct
        if (isRight) {
            correctCount++
            flashCorrect()
        } else {
            wrongCount++
            wrongList.add(
                WrongEntry(
                    left = q.left,
                    right = q.right,
                    user = symbol,
                    correct = correct
                )
            )
            flashWrong()
        }

        // 0.5s 自动下一题/结束
        binding.root.postDelayed({
            locked = false
            if (index >= totalCount - 1) {
                finishGame()
            } else {
                index++
                render()
            }
        }, 500)
    }

    private fun flashCorrect() {
        binding.tvCircleCur.setBackgroundResource(R.drawable.bg_compare_circle_correct)
        binding.root.postDelayed({
            binding.tvCircleCur.setBackgroundResource(R.drawable.bg_compare_circle)
        }, 280)
    }

    private fun flashWrong() {
        binding.tvCircleCur.setBackgroundResource(R.drawable.bg_compare_circle_wrong)
        binding.root.postDelayed({
            binding.tvCircleCur.setBackgroundResource(R.drawable.bg_compare_circle)
        }, 280)
    }

    private fun finishGame() {
        timerRunning = false
        handler.removeCallbacks(tick)

        val rate = if (totalCount == 0) 0 else (correctCount * 100 / totalCount)

        val dialogView = layoutInflater.inflate(R.layout.dialog_compare_result, null, false)

        val tvTotal = dialogView.findViewById<android.widget.TextView>(R.id.tvTotal)
        val tvCorrect = dialogView.findViewById<android.widget.TextView>(R.id.tvCorrect)
        val tvWrong = dialogView.findViewById<android.widget.TextView>(R.id.tvWrong)
        val tvAccuracy = dialogView.findViewById<android.widget.TextView>(R.id.tvAccuracy)
        val progress = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
            R.id.progressAccuracy
        )
        val tvTime = dialogView.findViewById<android.widget.TextView>(R.id.tvTime)

        val tvNoWrong = dialogView.findViewById<android.widget.TextView>(R.id.tvNoWrong)
        val rvWrong = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvWrong)

        val btnAgain = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAgain)
        val btnExit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExit)

        // 填充数据
        tvTotal.text = totalCount.toString()
        tvCorrect.text = correctCount.toString()
        tvWrong.text = wrongCount.toString()
        tvAccuracy.text = "$rate%"
        progress.progress = rate
        tvTime.text = "用时：${formatTime(elapsed)}"

        // 错题列表
        if (wrongList.isEmpty()) {
            tvNoWrong.visibility = android.view.View.VISIBLE
            rvWrong.visibility = android.view.View.GONE
        } else {
            tvNoWrong.visibility = android.view.View.GONE
            rvWrong.visibility = android.view.View.VISIBLE
            rvWrong.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvWrong.adapter = CompareWrongAdapter(wrongList)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnAgain.setOnClickListener {
            dialog.dismiss()
            startNewGame()
        }
        btnExit.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }


    /**
     * ✅ 识别 > 或 <
     * 思路：将 bitmap 缩放+二值化，找到笔迹包围盒；
     * 然后比较“靠左边缘/靠右边缘”的笔迹像素占比：
     * - '>' 的尖点在右侧，右边缘更“聚”
     * - '<' 的尖点在左侧，左边缘更“聚”
     *
     * 返回：">" / "<" / null(不确定)
     */
    private fun recognizeSymbol(src: Bitmap): String? {
        if (src.width <= 0 || src.height <= 0) return null

        // ---------- 1) 找笔迹包围盒（在原图上找，更准） ----------
        val w0 = src.width
        val h0 = src.height
        val pixels0 = IntArray(w0 * h0)
        src.getPixels(pixels0, 0, w0, 0, 0, w0, h0)

        fun isInk(color: Int): Boolean {
            val a = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            if (a < 10) return false
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            return lum < 245 // ✅ 放宽（抗锯齿灰色也算笔迹）
        }

        var minX = w0
        var minY = h0
        var maxX = -1
        var maxY = -1
        var inkCount = 0

        // 采样步长：提高速度（2 像素采样一次）
        val step = 2
        var y = 0
        while (y < h0) {
            val row = y * w0
            var x = 0
            while (x < w0) {
                if (isInk(pixels0[row + x])) {
                    inkCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }

        // ✅ 太少：大概率没写/太轻
        if (inkCount < 40 || maxX < 0) return null

        // ---------- 2) 裁剪 + padding ----------
        val pad = 24
        minX = (minX - pad).coerceAtLeast(0)
        minY = (minY - pad).coerceAtLeast(0)
        maxX = (maxX + pad).coerceAtMost(w0 - 1)
        maxY = (maxY + pad).coerceAtMost(h0 - 1)

        val cropW = (maxX - minX + 1).coerceAtLeast(1)
        val cropH = (maxY - minY + 1).coerceAtLeast(1)

        // 太扁/太窄的乱画直接失败
        if (cropW < 40 || cropH < 40) return null

        val cropped = Bitmap.createBitmap(src, minX, minY, cropW, cropH)

        // ---------- 3) 归一化缩放 ----------
        val size = 160
        val bmp = Bitmap.createScaledBitmap(cropped, size, size, true)

        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // ---------- 4) 统计“左尖/右尖” ----------
        // 看左右两侧边缘 20% 区域的墨迹占比
        val leftXEnd = (w * 0.20f).toInt()
        val rightXStart = (w * 0.80f).toInt()

        var boxInk = 0
        var leftInk = 0
        var rightInk = 0

        for (yy in 0 until h) {
            val row = yy * w
            for (xx in 0 until w) {
                if (isInk(pixels[row + xx])) {
                    boxInk++
                    if (xx <= leftXEnd) leftInk++
                    if (xx >= rightXStart) rightInk++
                }
            }
        }

        if (boxInk < 120) return null

        val leftRatio = leftInk.toFloat() / boxInk
        val rightRatio = rightInk.toFloat() / boxInk

        // ✅ 放宽门槛：不再要求 diff 很大
        val diff = kotlin.math.abs(rightRatio - leftRatio)

        // 置信度太低就返回 null（比如画了竖线、乱涂）
        if (diff < 0.015f) return null

        // 右边更聚 => '>'；左边更聚 => '<'
        return if (rightRatio > leftRatio) ">" else "<"
    }


    /**
     * ✅ 生成“很难一眼比较”的分数（默认 3..6 位分子分母）
     */
    private fun generateQuestions(count: Int, minDigits: Int, maxDigits: Int): List<CompareQuestion> {
        val list = ArrayList<CompareQuestion>(count)
        while (list.size < count) {
            val digits = Random.nextInt(minDigits, maxDigits + 1)

            val den1 = randomNDigits(digits).toLong()
            val num1 = randomNearHalf(den1)

            val den2 = randomNDigits(digits).toLong()
            if (den2 == den1) continue

            val base = (num1 * den2) / den1
            val delta = listOf(-2L, -1L, 1L, 2L).random()
            val num2 = (base + delta).coerceIn(1L, den2 - 1L)

            val f1 = Fraction(num1, den1)
            val f2 = Fraction(num2, den2)

            if (CompareQuestion.compareFractions(f1, f2) == 0) continue
            list.add(CompareQuestion(f1, f2))
        }
        return list
    }

    private fun randomNearHalf(den: Long): Long {
        val win = max(1L, den / 20L) // 5%
        val offset = Random.nextLong(-win, win + 1)
        return (den / 2L + offset).coerceIn(1L, den - 1L)
    }

    private fun randomNDigits(digits: Int): Int {
        val min = pow10(digits - 1)
        val max = pow10(digits) - 1
        return Random.nextInt(min, max + 1)
    }

    private fun pow10(exp: Int): Int {
        var v = 1
        repeat(exp) { v *= 10 }
        return v
    }

    private fun formatTime(ms: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remaining = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remaining)
    }

    override fun onDestroy() {
        timerRunning = false
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}

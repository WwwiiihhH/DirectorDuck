package com.example.directorduck_v10

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.data.model.WrongReviewDetail
import com.example.directorduck_v10.data.model.WrongReviewItem
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityWrongReviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrongReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWrongReviewBinding

    companion object {
        const val EXTRA_WRONG_UUIDS = "wrong_uuids"
        const val EXTRA_WRONG_USER_ANSWERS = "wrong_user_answers"
    }

    private var wrongUuids: ArrayList<String> = arrayListOf()
    private var wrongUserAnswers: ArrayList<String> = arrayListOf()

    private lateinit var adapter: WrongReviewPagerAdapter
    private val items: MutableList<WrongReviewItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWrongReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wrongUuids = intent.getStringArrayListExtra(EXTRA_WRONG_UUIDS) ?: arrayListOf()
        wrongUserAnswers = intent.getStringArrayListExtra(EXTRA_WRONG_USER_ANSWERS) ?: arrayListOf()

        if (wrongUuids.isEmpty()) {
            Toast.makeText(this, "没有错题可回顾", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 兜底：长度不一致则补齐
        if (wrongUserAnswers.size != wrongUuids.size) {
            val fixed = ArrayList<String>()
            for (i in wrongUuids.indices) fixed.add(wrongUserAnswers.getOrNull(i) ?: "")
            wrongUserAnswers = fixed
        }

        setupPager()
        initLoadingItems()
        loadAllWrongQuestions()
    }

    private fun setupPager() {
        binding.btnBack.setOnClickListener { finish() }

        adapter = WrongReviewPagerAdapter(items)
        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        binding.tvIndicator.text = "1/${wrongUuids.size}"

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvIndicator.text = "${position + 1}/${wrongUuids.size}"
            }
        })
    }

    private fun initLoadingItems() {
        items.clear()
        wrongUuids.forEachIndexed { index, uuid ->
            items.add(
                WrongReviewItem(
                    uuid = uuid,
                    userAnswer = wrongUserAnswers.getOrNull(index) ?: "",
                    loading = true
                )
            )
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadAllWrongQuestions() {
        // 串行逐题拉取（稳）
        wrongUuids.forEachIndexed { index, uuid ->
            lifecycleScope.launch {
                val userAns = wrongUserAnswers.getOrNull(index) ?: ""

                try {
                    val detailResp = withContext(Dispatchers.IO) {
                        ApiClient.practiceService.getQuestionDetailByUuid(uuid)
                    }
                    val answerResp = withContext(Dispatchers.IO) {
                        ApiClient.practiceService.getQuestionAnswerByUuid(uuid)
                    }

                    if (!detailResp.isSuccessful || detailResp.body()?.code != 200 || detailResp.body()?.data == null) {
                        adapter.updateItem(
                            index,
                            WrongReviewItem(
                                uuid = uuid,
                                userAnswer = userAns,
                                loading = false,
                                error = "题目详情加载失败：HTTP ${detailResp.code()}"
                            )
                        )
                        return@launch
                    }

                    if (!answerResp.isSuccessful || answerResp.body()?.code != 200 || answerResp.body()?.data == null) {
                        adapter.updateItem(
                            index,
                            WrongReviewItem(
                                uuid = uuid,
                                userAnswer = userAns,
                                loading = false,
                                error = "答案解析加载失败：HTTP ${answerResp.code()}"
                            )
                        )
                        return@launch
                    }

                    val d = detailResp.body()!!.data!!
                    val a = answerResp.body()!!.data!!

                    val merged = WrongReviewDetail(
                        questionText = d.questionText,
                        questionImage = d.questionImage,
                        optionA = d.optionA,
                        optionB = d.optionB,
                        optionC = d.optionC,
                        optionD = d.optionD,
                        correctAnswer = a.correctAnswer, // ✅ 以 answer 接口为准
                        analysis = a.analysis
                    )

                    adapter.updateItem(
                        index,
                        WrongReviewItem(
                            uuid = uuid,
                            userAnswer = userAns,
                            loading = false,
                            detail = merged
                        )
                    )

                } catch (e: Exception) {
                    adapter.updateItem(
                        index,
                        WrongReviewItem(
                            uuid = uuid,
                            userAnswer = userAns,
                            loading = false,
                            error = "加载异常：${e.message ?: "unknown"}"
                        )
                    )
                }
            }
        }
    }
}

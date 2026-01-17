package com.example.directorduck_v10.feature.ai.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.feature.ai.adapter.ChatAdapter
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.dto.deepseek.Message
import com.example.directorduck_v10.core.network.dto.deepseek.ProxyChatRequest
import com.example.directorduck_v10.feature.ai.model.ChatMessage
import com.example.directorduck_v10.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiActivity : BaseActivity() {

    private lateinit var binding: com.example.directorduck_v10.databinding.ActivityAiBinding
    private lateinit var chatAdapter: ChatAdapter

    // ✅ 固定默认模型（只用一个）
    private val selectedModel: String = "deepseek-chat"
    // 如果你想默认用深度思考，就改成：
    // private val selectedModel: String = "deepseek-reasoner"

    // ✅ 保存历史（让它记住上下文）
    private val conversationHistory = mutableListOf<Message>()

    private var thinkingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.example.directorduck_v10.databinding.ActivityAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecycler()
        initSendIconWatcher()

        binding.ivBack.setOnClickListener { finish() }
        binding.ivSend.setOnClickListener { sendMessage() }
    }

    private fun initRecycler() {
        chatAdapter = ChatAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = chatAdapter
    }

    // ✅ 输入框有内容：send_pressed；无内容：send_normal
    private fun initSendIconWatcher() {
        fun refreshIcon() {
            val hasText = !binding.etAiinput.text.isNullOrBlank()
            binding.ivSend.setImageResource(if (hasText) R.drawable.send_pressed else R.drawable.send_normal)
        }
        refreshIcon()

        binding.etAiinput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshIcon()
            override fun afterTextChanged(s: Editable?) = refreshIcon()
        })
    }

    private fun sendMessage() {
        val text = binding.etAiinput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, "请输入问题", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ history 不包含本次 question（因为后端还有 question 字段）
        val historySnapshot = conversationHistory.toList()

        chatAdapter.addMessage(ChatMessage(isUser = true, text = text))
        scrollToBottom()

        // ✅ 本次用户消息写入历史（供下一轮使用）
        conversationHistory.add(Message(role = "user", content = text))

        binding.etAiinput.setText("")

        // AI 占位 + 动态点点点
        chatAdapter.addMessage(ChatMessage(isUser = false, text = "鸭局长正在思考中"))
        val thinkingIndex = chatAdapter.itemCount - 1
        startThinkingDots(thinkingIndex)
        scrollToBottom()

        lifecycleScope.launch {
            val req = ProxyChatRequest(
                model = selectedModel,
                question = text,
                history = historySnapshot.takeLast(20)
            )

            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.deepSeekService.chat(req)
                }

                stopThinkingDots()

                if (resp.isSuccessful) {
                    val body = resp.body()
                    val answer = if (body?.code == 200 && body.data != null) {
                        body.data.answer
                    } else {
                        "出错了：${body?.message ?: "未知错误"}"
                    }

                    chatAdapter.updateLastAiMessage(answer)
                    conversationHistory.add(Message(role = "assistant", content = answer))
                } else {
                    chatAdapter.updateLastAiMessage("服务器错误：HTTP ${resp.code()}")
                }
            } catch (e: Exception) {
                stopThinkingDots()
                chatAdapter.updateLastAiMessage("网络异常：${e.message ?: "unknown"}")
            }

            scrollToBottom()
        }
    }

    private fun startThinkingDots(aiMsgIndex: Int) {
        stopThinkingDots()
        thinkingJob = lifecycleScope.launch {
            var dots = 0
            while (true) {
                dots = (dots + 1) % 4
                val suffix = ".".repeat(dots)
                chatAdapter.updateMessageAt(aiMsgIndex, "鸭局长正在思考中$suffix")
                delay(350)
            }
        }
    }

    private fun stopThinkingDots() {
        thinkingJob?.cancel()
        thinkingJob = null
    }

    private fun scrollToBottom() {
        binding.recyclerView.post {
            val count = chatAdapter.itemCount
            if (count > 0) binding.recyclerView.scrollToPosition(count - 1)
        }
    }
}

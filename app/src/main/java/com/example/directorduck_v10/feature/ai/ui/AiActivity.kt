package com.example.directorduck_v10.feature.ai.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.dto.deepseek.Message
import com.example.directorduck_v10.core.network.dto.deepseek.ProxyChatRequest
import com.example.directorduck_v10.databinding.ActivityAiBinding
import com.example.directorduck_v10.feature.ai.adapter.ChatAdapter
import com.example.directorduck_v10.feature.ai.adapter.ChatSessionAdapter
import com.example.directorduck_v10.feature.ai.data.ChatHistoryStore
import com.example.directorduck_v10.feature.ai.model.ChatMessage
import com.example.directorduck_v10.feature.ai.model.ChatSession
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class AiActivity : BaseActivity() {

    private lateinit var binding: ActivityAiBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sessionAdapter: ChatSessionAdapter
    private lateinit var historyStore: ChatHistoryStore

    private val sessions = mutableListOf<ChatSession>()
    private var currentSession: ChatSession? = null

    private val gson = Gson()
    private val conversationHistory = mutableListOf<Message>()
    private val selectedModel: String = "deepseek-chat"

    private var thinkingJob: Job? = null
    private var typewriterJob: Job? = null
    private var streamingCall: Call? = null
    private var currentAiMsgIndex: Int = -1
    private var isStreaming = false
    private var autoScrollEnabled = true

    private val streamClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var userId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getLongExtra(EXTRA_USER_ID, -1L).takeIf { it > 0 }
        historyStore = ChatHistoryStore(this, userId)

        initRecycler()
        initHistoryDrawer()
        initSendIconWatcher()

        binding.ivHistory.setOnClickListener { toggleDrawer() }
        binding.ivBack.setOnClickListener { finish() }
        binding.ivSend.setOnClickListener { sendMessage() }

        loadSessions()
    }

    private fun initRecycler() {
        chatAdapter = ChatAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = chatAdapter
        // 防止频繁更新导致的闪烁/抖动
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                newState: Int
            ) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    autoScrollEnabled = false
                } else if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                    if (isNearBottom()) {
                        autoScrollEnabled = true
                    }
                }
            }
        })
    }

    private fun initHistoryDrawer() {
        sessionAdapter = ChatSessionAdapter(mutableListOf()) { session ->
            switchToSession(session)
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = sessionAdapter

        binding.tvNewChat.setOnClickListener {
            val session = createNewSession()
            switchToSession(session)
        }
    }

    private fun loadSessions() {
        sessions.clear()
        sessions.addAll(historyStore.load())

        if (sessions.isEmpty()) {
            val session = createNewSession()
            switchToSession(session)
        } else {
            sessions.sortByDescending { it.updatedAt }
            val session = sessions.first()
            switchToSession(session)
        }
        refreshSessionList()
    }

    private fun createNewSession(): ChatSession {
        val now = System.currentTimeMillis()
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            messages = mutableListOf(),
            updatedAt = now,
            createdAt = now
        )
        sessions.add(0, session)
        persistSessions()
        refreshSessionList()
        return session
    }

    private fun switchToSession(session: ChatSession) {
        currentSession = session
        chatAdapter.setMessages(session.messages)
        rebuildHistoryFromMessages(session.messages)
        sessionAdapter.setSelected(session.id)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        scrollToBottom()
    }

    private fun rebuildHistoryFromMessages(messages: List<ChatMessage>) {
        conversationHistory.clear()
        messages.forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            conversationHistory.add(Message(role = role, content = msg.text))
        }
    }

    private fun refreshSessionList() {
        sessions.sortByDescending { it.updatedAt }
        sessionAdapter.submit(sessions)
        sessionAdapter.setSelected(currentSession?.id)
    }

    private fun persistSessions() {
        historyStore.save(sessions)
    }

    private fun updateSessionTitleIfNeeded(text: String) {
        val session = currentSession ?: return
        if (session.title == "新对话") {
            session.title = if (text.length > 12) text.take(12) + "..." else text
        }
    }

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
        if (isStreaming) {
            Toast.makeText(this, "正在生成回复，请稍等", Toast.LENGTH_SHORT).show()
            return
        }

        val text = binding.etAiinput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, "请输入问题", Toast.LENGTH_SHORT).show()
            return
        }

        val session = currentSession ?: createNewSession().also { switchToSession(it) }
        autoScrollEnabled = true

        val historySnapshot = conversationHistory.toList()

        chatAdapter.addMessage(ChatMessage(isUser = true, text = text))
        updateSessionTitleIfNeeded(text)
        conversationHistory.add(Message(role = "user", content = text))

        binding.etAiinput.setText("")

        chatAdapter.addMessage(ChatMessage(isUser = false, text = "鸭局长正在思考中"))
        currentAiMsgIndex = chatAdapter.itemCount - 1
        startThinkingDots(currentAiMsgIndex)
        scrollToBottom()

        session.updatedAt = System.currentTimeMillis()
        persistSessions()
        refreshSessionList()

        startStream(historySnapshot, text)
    }

    private fun startStream(historySnapshot: List<Message>, question: String) {
        setSendingState(true)

        val req = ProxyChatRequest(
            model = selectedModel,
            question = question,
            history = historySnapshot.takeLast(20)
        )

        val url = ApiClient.getHostUrl() + "/api/deepseek/chat/stream"
        val bodyJson = gson.toJson(req)
        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        streamingCall?.cancel()
        streamingCall = streamClient.newCall(request)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = streamingCall?.execute()
                if (resp == null || !resp.isSuccessful) {
                    val msg = "服务端错误：HTTP ${resp?.code ?: -1}"
                    withContext(Dispatchers.Main) { handleStreamFailure(msg, historySnapshot, question) }
                    return@launch
                }

                val source = resp.body?.source()
                if (source == null) {
                    withContext(Dispatchers.Main) { handleStreamFailure("空响应", historySnapshot, question) }
                    return@launch
                }

                var fullText = ""
                var firstChunk = true

                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    if (!line.startsWith("data:")) continue

                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break

                    val delta = extractDelta(data)
                    if (delta.isNotEmpty()) {
                        if (firstChunk) {
                            firstChunk = false
                            withContext(Dispatchers.Main) { stopThinkingDots() }
                        }
                        fullText += delta
                        val finalText = normalizeAiText(fullText)
                        withContext(Dispatchers.Main) {
                            updateStreamingMessage(finalText)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    val cleaned = normalizeAiText(fullText).ifBlank { "暂无回复" }
                    finalizeAssistantMessage(cleaned)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleStreamFailure("网络异常: ${e.message ?: "unknown"}", historySnapshot, question)
                }
            }
        }
    }

    private fun handleStreamFailure(msg: String, historySnapshot: List<Message>, question: String) {
        stopThinkingDots()
        Toast.makeText(this, "流式失败，已切换普通模式", Toast.LENGTH_SHORT).show()
        fallbackToNonStream(historySnapshot, question)
    }

    private fun fallbackToNonStream(historySnapshot: List<Message>, question: String) {
        lifecycleScope.launch {
            try {
                val req = ProxyChatRequest(
                    model = selectedModel,
                    question = question,
                    history = historySnapshot.takeLast(20)
                )

                val resp = withContext(Dispatchers.IO) {
                    ApiClient.deepSeekService.chat(req)
                }

                val answer = if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.code == 200 && body.data != null) {
                        body.data.answer
                    } else {
                        "出错了：${body?.message ?: "未知错误"}"
                    }
                } else {
                    "服务端错误：HTTP ${resp.code()}"
                }

                stopThinkingDots()
                typewriterJob?.cancel()
                typewriterJob = lifecycleScope.launch {
                    typeOutText(normalizeAiText(answer))
                }
            } catch (e: Exception) {
                stopThinkingDots()
                updateStreamingMessage("网络异常: ${e.message ?: "unknown"}")
                finalizeAssistantMessage("网络异常: ${e.message ?: "unknown"}")
            }
        }
    }

    private suspend fun typeOutText(text: String) {
        var current = ""
        for (ch in text) {
            current += ch
            updateStreamingMessage(current)
            delay(15)
        }
        finalizeAssistantMessage(text)
    }

    private fun extractDelta(data: String): String {
        return try {
            val root = JsonParser().parse(data).asJsonObject
            if (root.has("error")) {
                return root.get("error").asString
            }
            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) return ""

            val choice = choices[0].asJsonObject
            val delta = choice.getAsJsonObject("delta")
            val message = choice.getAsJsonObject("message")

            when {
                delta != null && delta.has("content") -> delta.get("content").asString
                message != null && message.has("content") -> message.get("content").asString
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun updateStreamingMessage(text: String) {
        if (currentAiMsgIndex < 0) return
        chatAdapter.updateMessageAt(currentAiMsgIndex, text)
        if (autoScrollEnabled) {
            scrollToBottom()
        }
    }

    private fun finalizeAssistantMessage(text: String) {
        stopThinkingDots()
        if (text.isBlank()) return

        val cleaned = normalizeAiText(text)
        conversationHistory.add(Message(role = "assistant", content = cleaned))
        currentSession?.updatedAt = System.currentTimeMillis()
        persistSessions()
        refreshSessionList()
        currentAiMsgIndex = -1
        setSendingState(false)
    }

    private fun normalizeAiText(text: String): String {
        val normalized = text.replace("\r\n", "\n")
        val lines = normalized.split("\n").map { line ->
            var l = line.replace(Regex("^\\s*#{1,6}\\s*"), "")
            l = l.replace(Regex("^\\s*[-*+]\\s+"), "• ")
            l = l.replace(Regex("^\\s*\\d+\\.\\s+"), "• ")
            l
        }
        return lines.joinToString("\n")
            .replace("```", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun startThinkingDots(aiMsgIndex: Int) {
        stopThinkingDots()
        thinkingJob = lifecycleScope.launch {
            var dots = 0
            while (true) {
                dots = (dots + 1) % 4
                val suffix = ".".repeat(dots)
                updateStreamingMessage("鸭局长正在思考中$suffix")
                delay(350)
            }
        }
    }

    private fun stopThinkingDots() {
        thinkingJob?.cancel()
        thinkingJob = null
    }

    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setSendingState(sending: Boolean) {
        isStreaming = sending
        binding.ivSend.isEnabled = !sending
        binding.etAiinput.isEnabled = !sending
        binding.ivSend.alpha = if (sending) 0.6f else 1f
    }

    private fun scrollToBottom() {
        binding.recyclerView.post {
            val count = chatAdapter.itemCount
            if (count > 0) binding.recyclerView.scrollToPosition(count - 1)
        }
    }

    private fun isNearBottom(): Boolean {
        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return true
        val last = lm.findLastVisibleItemPosition()
        return last >= chatAdapter.itemCount - 2
    }

    override fun onDestroy() {
        stopThinkingDots()
        typewriterJob?.cancel()
        streamingCall?.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_USER_ID = "userId"
    }
}

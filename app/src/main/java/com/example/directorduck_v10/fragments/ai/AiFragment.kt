package com.example.directorduck_v10.fragments.ai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.databinding.FragmentAiBinding
import com.example.directorduck_v10.fragments.ai.adapters.ChatAdapter
import com.example.directorduck_v10.fragments.ai.model.ChatMessage
import com.example.directorduck_v10.fragments.ai.network.KimiApiService

class AiFragment : Fragment() {

    private var _binding: FragmentAiBinding? = null
    private val binding get() = _binding!!

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var thinkingRunnable: Runnable? = null
    private var dotCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 RecyclerView
        chatAdapter = ChatAdapter(messageList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = chatAdapter

        val editText = binding.etAiinput
        val sendButton = binding.ivSend

        // 输入框变化监听
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    sendButton.setImageResource(R.drawable.send_pressed)
                } else {
                    sendButton.setImageResource(R.drawable.send_normal)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 发送按钮点击
        sendButton.setOnClickListener {
            val question = editText.text.toString().trim()
            if (question.isNotEmpty()) {
                // 播放动画
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.send_button_click)
                sendButton.startAnimation(anim)

                // 清空输入
                editText.setText("")
                sendButton.setImageResource(R.drawable.send_normal)

                // 添加用户消息
                addMessage("user", question)

                // 添加占位消息并开始动画
                addMessage("assistant", "鸭局长皱着鸭眉思考中\uD83E\uDD14")
                startThinkingAnimation()

                // 调用 API 获取回复
                // 只取最近 20 条历史（你可以调整）
                val recentHistory = messageList.takeLast(20)

                KimiApiService.askKimi(recentHistory, question) { reply ->

                requireActivity().runOnUiThread {
                        stopThinkingAnimation()
                        // 删除占位消息
                        messageList.removeLast()
                        chatAdapter.notifyItemRemoved(messageList.size)

                        if (reply != null) {
                            addMessage("assistant", reply)
                        } else {
                            addMessage("assistant", "哎呀，鸭局长正在开会，请稍后再试试~")
                        }
                    }
                }
            }
        }
    }

    private fun addMessage(role: String, content: String) {
        messageList.add(ChatMessage(role, content))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun startThinkingAnimation() {
        dotCount = 0
        thinkingRunnable = object : Runnable {
            override fun run() {
                if (messageList.isNotEmpty() && messageList.last().role == "assistant") {
                    val base = "鸭局长皱着鸭眉思考中\uD83E\uDD14"
                    val dots = ".".repeat(dotCount % 4) // "", ".", "..", "..."
                    messageList[messageList.size - 1] = ChatMessage("assistant", base + dots)
                    chatAdapter.notifyItemChanged(messageList.size - 1)
                    dotCount++
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(thinkingRunnable!!)
    }

    private fun stopThinkingAnimation() {
        thinkingRunnable?.let { handler.removeCallbacks(it) }
        thinkingRunnable = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopThinkingAnimation()
        _binding = null
    }
}

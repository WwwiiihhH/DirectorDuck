package com.example.directorduck_v10

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.databinding.ActivityAiBinding
import com.example.directorduck_v10.fragments.ai.adapters.ChatAdapter
import com.example.directorduck_v10.fragments.ai.model.ChatMessage
import com.example.directorduck_v10.fragments.ai.network.KimiApiService

class AiActivity : BaseActivity() {

    private lateinit var binding: ActivityAiBinding

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var thinkingRunnable: Runnable? = null
    private var dotCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置 RecyclerView
        chatAdapter = ChatAdapter(messageList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = chatAdapter

        // 输入监听
        binding.etAiinput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ivSend.setImageResource(
                    if (!s.isNullOrEmpty()) R.drawable.send_pressed
                    else R.drawable.send_normal
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 发送按钮点击事件
        binding.ivSend.setOnClickListener {
            val question = binding.etAiinput.text.toString().trim()
            if (question.isNotEmpty()) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.send_button_click)
                binding.ivSend.startAnimation(anim)

                binding.etAiinput.setText("")
                binding.ivSend.setImageResource(R.drawable.send_normal)

                addMessage("user", question)
                addMessage("assistant", "鸭局长皱着鸭眉思考中\uD83E\uDD14")
                startThinkingAnimation()

                val recentHistory = messageList.takeLast(20)
                KimiApiService.askKimi(recentHistory, question) { reply ->
                    runOnUiThread {
                        stopThinkingAnimation()
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

        // 返回按钮逻辑
        binding.ivBack.setOnClickListener {
            finish() // 结束当前 Activity，返回上一个页面
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
                    val dots = ".".repeat(dotCount % 4)
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

    override fun onDestroy() {
        super.onDestroy()
        stopThinkingAnimation()
    }
}

package com.example.directorduck_v10

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.enableEdgeToEdge
import com.example.directorduck_v10.databinding.ActivitySplashBinding

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var hasSkipped = false // 防止重复跳转

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 倒计时器
        val timer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.btnSkip.text = "跳过 ${seconds}s"
            }

            override fun onFinish() {
                if (!hasSkipped) {
                    gotoMain()
                }
            }
        }
        timer.start()

        // 点击跳过
        binding.btnSkip.setOnClickListener {
            if (!hasSkipped) {
                hasSkipped = true
                timer.cancel()
                gotoMain()
            }
        }
    }

    private fun gotoMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

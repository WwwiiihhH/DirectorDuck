package com.example.directorduck_v10.feature.course.ui



import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.databinding.ActivityCoursePlayerBinding

class CoursePlayerActivity : BaseActivity() {

    private lateinit var binding: ActivityCoursePlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 初始化 ViewBinding
        binding = ActivityCoursePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递过来的视频 URL
        val videoUrl = intent.getStringExtra("videoUrl")

        if (videoUrl.isNullOrEmpty()) {
            finish() // 如果没有视频地址就直接退出
            return
        }

        // 设置视频控制器
        val mediaController = MediaController(this).apply {
            setAnchorView(binding.videoView)
        }

        binding.videoView.apply {
            setMediaController(mediaController)
            setVideoURI(Uri.parse(videoUrl))
            setOnPreparedListener { start() }

            // 播放结束自动退出可选
//            setOnCompletionListener { finish() }
        }
        // 调整视频宽高使居中显示
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            val videoWidth = mediaPlayer.videoWidth
            val videoHeight = mediaPlayer.videoHeight

            val videoProportion = videoWidth.toFloat() / videoHeight
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val screenProportion = screenWidth.toFloat() / screenHeight

            val lp = binding.videoView.layoutParams as ConstraintLayout.LayoutParams
            if (videoProportion > screenProportion) {
                lp.width = screenWidth
                lp.height = (screenWidth / videoProportion).toInt()
            } else {
                lp.width = (screenHeight * videoProportion).toInt()
                lp.height = screenHeight
            }

            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            binding.videoView.layoutParams = lp

            binding.videoView.start()
        }


    }
}
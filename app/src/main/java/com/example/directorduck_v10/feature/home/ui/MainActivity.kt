package com.example.directorduck_v10.feature.home.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.core.state.SharedUserViewModel
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.databinding.ActivityMainBinding
import com.example.directorduck_v10.feature.ai.ui.AiActivity
import com.example.directorduck_v10.feature.community.ui.InformationFragment
import com.example.directorduck_v10.feature.course.ui.CourseFragment
import com.example.directorduck_v10.feature.mine.ui.MineFragment
import com.example.directorduck_v10.feature.practice.ui.PracticeFragment
import java.util.Calendar
import java.util.Date

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val practiceFragment = PracticeFragment()
    private val courseFragment = CourseFragment()
    private val mineFragment = MineFragment()
    private val informationFragment = InformationFragment()

    private val sharedUserViewModel: SharedUserViewModel by viewModels()
    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 获取用户信息并注入 ViewModel
        if (intent.hasExtra("user")) {
            currentUser = intent.getSerializableExtra("user") as User
            sharedUserViewModel.user.value = currentUser
        }

        // 2. 默认显示练习页面（初始样式：非 Mine 页面）
        switchFragment(practiceFragment)
        binding.tvTime.text = getCountdownText()
        updateBottomNavUI(0)
        updateTopLayoutStyle(isMinePage = false) // <--- 初始化顶部样式

        // 3. 设置点击事件

        // [练习]
        binding.bottomNav1.setOnClickListener {
            binding.tvTime.text = getCountdownText()
            switchFragment(practiceFragment)
            updateBottomNavUI(0)
            updateTopLayoutStyle(isMinePage = false) // <--- 恢复白色顶部
        }

        // [课程]
        binding.bottomNav2.setOnClickListener {
            binding.tvTime.text = "鸭局长喊你上课！"
            switchFragment(courseFragment)
            updateBottomNavUI(1)
            updateTopLayoutStyle(isMinePage = false) // <--- 恢复白色顶部
        }

        // [AI 问答]
        binding.bottomNav3.setOnClickListener {
            playAiIconAnimation()
            val intent = Intent(this, AiActivity::class.java)
            if (this::currentUser.isInitialized) {
                intent.putExtra(AiActivity.EXTRA_USER_ID, currentUser.id)
            }
            startActivity(intent)
            // 跳转 Activity 不需要处理当前页面的顶部样式
        }

        // [资讯]
        binding.bottomNav4.setOnClickListener {
            binding.tvTime.text = "公考朋友圈~"
            switchFragment(informationFragment)
            updateBottomNavUI(3)
            updateTopLayoutStyle(isMinePage = false) // <--- 恢复白色顶部
        }

        // [我的]
        binding.bottomNav5.setOnClickListener {
            binding.tvTime.text = "查看你的小档案~"
            switchFragment(mineFragment)
            updateBottomNavUI(4)
            updateTopLayoutStyle(isMinePage = true) // <--- 开启深蓝色顶部模式
        }
    }

    /**
     * 根据是否是“我的”页面，动态修改顶部布局的颜色和样式
     */
    private fun updateTopLayoutStyle(isMinePage: Boolean) {
        if (isMinePage) {
            // === 沉浸式深色模式 ===
            // 1. 背景变深蓝 (#3E54AC)
            binding.TopLayout.setBackgroundColor(Color.parseColor("#3E54AC"))

            // 2. 文字变白（深色背景需要浅色文字）
            binding.tvTime.setTextColor(Color.WHITE)

            // 3. 隐藏淡黄色分割线（避免视觉割裂）
            binding.topDivider.visibility = View.GONE

            // 4. （可选）改变状态栏图标颜色为浅色（白色）
            // 注意：这取决于你的 BaseActivity 配置，如无效可注释掉下面这行
//            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        } else {
            // === 恢复默认浅色模式 ===
            // 1. 背景恢复白色
            binding.TopLayout.setBackgroundColor(Color.WHITE)

            // 2. 文字恢复深色（黑色或深灰）
            binding.tvTime.setTextColor(Color.parseColor("#1A1C1E"))

            // 3. 显示分割线
            binding.topDivider.visibility = View.VISIBLE

            // 4. （可选）恢复状态栏图标为深色（黑色）
//            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // 更新底部图标和文字颜色
    private fun updateBottomNavUI(selectedIndex: Int) {
        val imageViews = listOf(
            binding.ivHome,
            binding.ivCourse,
            binding.ivAi,   // AI 图标不变色
            binding.ivInfo,
            binding.ivMine
        )

        val textViews = listOf(
            binding.tvHome,
            binding.tvCourse,
            null,           // AI 问答没有文字描述
            binding.tvInfo,
            binding.tvMine,
        )

        val normalIcons = listOf(
            R.drawable.practice_normal,
            R.drawable.course_normal2,
            R.drawable.logonew,         // AI icon 不变
            R.drawable.info_normal,
            R.drawable.mine_normal
        )

        val pressedIcons = listOf(
            R.drawable.practice_pressed,
            R.drawable.course_pressed2,
            R.drawable.logonew,         // AI icon 不变
            R.drawable.info_pressed,
            R.drawable.mine_pressed
        )

        for (i in imageViews.indices) {
            if (i == 2) {
                // 跳过 AI 问答按钮，不改变样式
                continue
            }

            if (i == selectedIndex) {
                imageViews[i].setImageResource(pressedIcons[i])
                textViews[i]?.setTextColor(Color.parseColor("#446DFF")) // 蓝色
            } else {
                imageViews[i].setImageResource(normalIcons[i])
                textViews[i]?.setTextColor(Color.parseColor("#A7A6B5")) // 灰色
            }
        }
    }

    private fun getCountdownText(): String {
        val targetDate = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 30) // 月份从0开始
        }.time

        val currentDate = Date()
        val diffMillis = targetDate.time - currentDate.time
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return "距离2027年国考仅剩 $diffDays 天！"
    }

    private fun playAiIconAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.ivAi, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivAi, "scaleY", 1f, 1.2f, 1f)
        val bounce = ObjectAnimator.ofFloat(binding.ivAi, "translationY", 0f, -20f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, bounce)
        animatorSet.duration = 600
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()
    }
}

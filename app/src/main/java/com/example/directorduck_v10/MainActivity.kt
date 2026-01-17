package com.example.directorduck_v10

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.databinding.ActivityMainBinding
import com.example.directorduck_v10.fragments.Information.InformationFragment
import com.example.directorduck_v10.fragments.ai.AiFragment
import com.example.directorduck_v10.fragments.course.CourseFragment
import com.example.directorduck_v10.fragments.mine.MineFragment
import com.example.directorduck_v10.fragments.practice.PracticeFragment
import com.example.directorduck_v10.viewmodel.SharedUserViewModel

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val practiceFragment = PracticeFragment()
    private val courseFragment = CourseFragment()
    private val aiFragment = AiFragment()
    private val mineFragment = MineFragment()
    private val informationFragment = InformationFragment()

    private val sharedUserViewModel: SharedUserViewModel by viewModels()
    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从 LoginActivity 获取 User 对象
        currentUser = intent.getSerializableExtra("user") as User

        // 注入用户到 ViewModel
        sharedUserViewModel.user.value = currentUser




        // 默认显示练习页面
        switchFragment(practiceFragment)
        binding.tvTime.text = getCountdownText()
        updateBottomNavUI(0)

        // 设置点击事件
        binding.bottomNav1.setOnClickListener {
            binding.tvTime.text = getCountdownText()
            switchFragment(practiceFragment)
            updateBottomNavUI(0)
        }
        binding.bottomNav2.setOnClickListener {
            binding.tvTime.text="鸭局长喊你上课！"
            switchFragment(courseFragment)
            updateBottomNavUI(1)
        }
        binding.bottomNav3.setOnClickListener {
//            binding.tvTime.text="鸭局长上线答疑啦！"
//            switchFragment(aiFragment)
//            updateBottomNavUI(2)
            playAiIconAnimation()
            val intent = Intent(this, AiActivity::class.java)
            startActivity(intent)
        }
        binding.bottomNav4.setOnClickListener {
            binding.tvTime.text="考公资讯~"
            switchFragment(informationFragment)
            updateBottomNavUI(3)
        }
        binding.bottomNav5.setOnClickListener {
            binding.tvTime.text="查看你的小档案~"
            switchFragment(mineFragment)
            updateBottomNavUI(4)
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
                // 跳过 AI 问答按钮，不变样式
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
        val targetDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 30) // 月份是从0开始的
        }.time

        val currentDate = java.util.Date()
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

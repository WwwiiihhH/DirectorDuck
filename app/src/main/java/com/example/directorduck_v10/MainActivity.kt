package com.example.directorduck_v10

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import com.example.directorduck_v10.databinding.ActivityMainBinding
import com.example.directorduck_v10.fragments.AiFragment
import com.example.directorduck_v10.fragments.CourseFragment
import com.example.directorduck_v10.fragments.MineFragment
import com.example.directorduck_v10.fragments.PracticeFragment

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val practiceFragment = PracticeFragment()
    private val courseFragment = CourseFragment()
    private val aiFragment = AiFragment()
    private val mineFragment = MineFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 默认显示练习页面
        switchFragment(practiceFragment)
        updateBottomNavUI(0)

        // 设置点击事件
        binding.bottomNav1.setOnClickListener {
            switchFragment(practiceFragment)
            updateBottomNavUI(0)
        }
        binding.bottomNav2.setOnClickListener {
            switchFragment(courseFragment)
            updateBottomNavUI(1)
        }
        binding.bottomNav3.setOnClickListener {
            switchFragment(aiFragment)
            updateBottomNavUI(2)
        }
        binding.bottomNav4.setOnClickListener {
            switchFragment(mineFragment)
            updateBottomNavUI(3)
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
            binding.ivHome, binding.ivCourse, binding.ivAi, binding.ivMine
        )
        val textViews = listOf(
            binding.tvHome, binding.tvCourse, binding.tvAi, binding.tvMine
        )

        val normalIcons = listOf(
            R.drawable.practice_normal,

            R.drawable.practice_normal,
            R.drawable.practice_normal,

//            R.drawable.course_normal,
//            R.drawable.ai_normal,
            R.drawable.mine_normal
        )
        val pressedIcons = listOf(
            R.drawable.practice_pressed,

            R.drawable.practice_pressed,
            R.drawable.practice_pressed,


//            R.drawable.course_pressed,
//            R.drawable.ai_pressed,
            R.drawable.mine_pressed
        )

        for (i in imageViews.indices) {
            if (i == selectedIndex) {
                imageViews[i].setImageResource(pressedIcons[i])
                textViews[i].setTextColor(Color.parseColor("#446DFF")) // 蓝色
            } else {
                imageViews[i].setImageResource(normalIcons[i])
                textViews[i].setTextColor(Color.parseColor("#A7A6B5")) // 灰色
            }
        }




    }
}

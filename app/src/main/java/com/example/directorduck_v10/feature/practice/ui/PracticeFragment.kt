package com.example.directorduck_v10.feature.practice.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.R
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.state.SharedUserViewModel
import com.example.directorduck_v10.databinding.FragmentPracticeBinding
import com.example.directorduck_v10.feature.favorite.ui.FavoriteCollectionActivity
import com.example.directorduck_v10.feature.game.ui.CompareSizeGameActivity
import com.example.directorduck_v10.feature.mockexam.ui.MockExamListActivity
import com.example.directorduck_v10.feature.notice.ui.NoticeActivity
import com.example.directorduck_v10.feature.practice.adapter.BannerAdapter
import com.example.directorduck_v10.feature.practice.adapter.HorizontalImageAdapter
import com.example.directorduck_v10.feature.practice.adapter.PracticeAdapter
import com.example.directorduck_v10.feature.practice.model.Category
import com.example.directorduck_v10.feature.practice.model.ImageItem
import com.example.directorduck_v10.feature.practice.model.PracticeItem
import com.example.directorduck_v10.feature.practice.model.Subcategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeFragment : Fragment() {

    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!

    private lateinit var customQuizDialog: Dialog
    private var selectedQuestionCount = 10 // 默认题目数量

    // SharedPreferences的键名
    private val PREFS_NAME = "PracticePrefs"
    private val KEY_QUESTION_COUNT = "question_count"

    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()

    // ================== Banner 自动轮播 + 手动滑动暂停恢复 ==================
    private val bannerHandler = Handler(Looper.getMainLooper())
    private val bannerInterval = 4500L               // 正常自动轮播间隔
    private val resumeAfterUserDelay = 2000L         // 用户手动滑动后暂停多久再继续

    private var isUserDragging = false

    private val bannerRunnable = object : Runnable {
        override fun run() {
            val vp = _binding?.viewPager ?: return
            val count = bannerImages.size
            if (count > 1) {
                val next = (vp.currentItem + 1) % count
                vp.setCurrentItem(next, true)
                bannerHandler.postDelayed(this, bannerInterval)
            }
        }
    }

    private fun startBannerAutoScroll(delay: Long = bannerInterval) {
        bannerHandler.removeCallbacks(bannerRunnable)
        bannerHandler.postDelayed(bannerRunnable, delay)
    }

    private fun stopBannerAutoScroll() {
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    private val bannerPageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)

            when (state) {
                ViewPager2.SCROLL_STATE_DRAGGING -> {
                    // 用户开始手动拖动：立刻暂停
                    isUserDragging = true
                    stopBannerAutoScroll()
                }

                ViewPager2.SCROLL_STATE_IDLE -> {
                    // 用户松手并停稳：延迟一段时间再恢复（避免抢操作）
                    if (isUserDragging) {
                        isUserDragging = false
                        if (isResumed) {
                            startBannerAutoScroll(resumeAfterUserDelay)
                        }
                    }
                }

                ViewPager2.SCROLL_STATE_SETTLING -> {
                    // 惯性滑动中：不做事（保持暂停状态）
                }
            }
        }
    }
    // ======================================================================

    private val bannerImages = listOf(
        R.drawable.banner1,
        R.drawable.banner2,
        R.drawable.banner3,
        R.drawable.banner4,
        R.drawable.banner5
    )

    private val horizontalItems = listOf(
        ImageItem(R.drawable.icon_100, "考试资讯"),
        ImageItem(R.drawable.app_iocn_int, "火眼金睛"),
        ImageItem(R.drawable.app_iocn_manager, "收藏集"),
        ImageItem(R.drawable.app_iocn_myhomework, "模考")
    )

    private lateinit var practiceAdapter: PracticeAdapter
    private val categories = mutableListOf<Category>()
    private val categorySubcategories = mutableMapOf<Int, List<Subcategory>>()
    private val expandedCategories = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeBinding.inflate(inflater, container, false)

        setupBanner()
        setupHorizontalList()
        setupCategoriesList()
        loadCategories()

        loadSavedQuestionCount()
        initCustomQuizDialog()

        binding.btnCustomQuiz.setOnClickListener {
            customQuizDialog.show()
        }

        Log.d("dadawd", "onCreateView: ${getSavedQuestionCount()}")
        return binding.root
    }

    private fun setupBanner() {
        binding.viewPager.adapter = BannerAdapter(bannerImages)
        binding.viewPager.unregisterOnPageChangeCallback(bannerPageCallback) // 防止重复注册
        binding.viewPager.registerOnPageChangeCallback(bannerPageCallback)
    }

    private fun setupHorizontalList() {
        binding.recyclerHorizontal.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerHorizontal.adapter = HorizontalImageAdapter(horizontalItems) { item ->
            val position = horizontalItems.indexOf(item)
            when (position) {
                0 -> startActivity(Intent(requireContext(), NoticeActivity::class.java))
                1 -> startActivity(Intent(requireContext(), CompareSizeGameActivity::class.java))
                2 -> {
                    val user = sharedUserViewModel.user.value
                    if (user != null) {
                        FavoriteCollectionActivity.start(requireContext(), user)
                    } else {
                        Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> {
                    val user = sharedUserViewModel.user.value
                    if (user != null) {
                        val it = Intent(requireContext(), MockExamListActivity::class.java)
                        it.putExtra("userId", user.id)
                        it.putExtra("username", user.username)
                        startActivity(it)
                    } else {
                        Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> Toast.makeText(requireContext(), "点击了：${item.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategoriesList() {
        practiceAdapter = PracticeAdapter(
            onCategoryClick = { categoryId ->
                val category = categories.find { it.id == categoryId } ?: return@PracticeAdapter
                val questionCount = getSavedQuestionCount()

                val user = sharedUserViewModel.user.value
                if (user != null) {
                    QuizActivity.startQuizActivity(
                        context = requireContext(),
                        user = user,
                        categoryId = categoryId,
                        categoryName = category.categoryName,
                        questionCount = questionCount
                    )
                } else {
                    Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                }
            },
            onCategoryToggle = { categoryId -> toggleCategory(categoryId) },
            onSubcategoryClick = { subcategoryId ->
                val subcategory = categorySubcategories.values.flatten().find { it.id == subcategoryId }
                    ?: return@PracticeAdapter

                val questionCount = getSavedQuestionCount()
                val categoryName = categories.find { it.id == subcategory.categoryId }?.categoryName

                val user = sharedUserViewModel.user.value
                if (user != null) {
                    QuizActivity.startQuizActivity(
                        context = requireContext(),
                        user = user,
                        categoryId = subcategory.categoryId,
                        categoryName = categoryName,
                        subcategoryId = subcategoryId,
                        subcategoryName = subcategory.subcategoryName,
                        questionCount = questionCount
                    )
                } else {
                    Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = practiceAdapter
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.practiceService.getCategories()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        withContext(Dispatchers.Main) {
                            categories.clear()
                            categories.addAll(apiResponse.data)
                            updateCategoryList()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "加载分类失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "服务器响应错误: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleCategory(categoryId: Int) {
        if (expandedCategories.contains(categoryId)) {
            expandedCategories.remove(categoryId)
        } else {
            expandedCategories.add(categoryId)
            if (!categorySubcategories.containsKey(categoryId)) {
                loadSubcategories(categoryId)
            }
        }
        updateCategoryList()
    }

    private fun loadSubcategories(categoryId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.practiceService.getSubcategories(categoryId)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        withContext(Dispatchers.Main) {
                            categorySubcategories[categoryId] = apiResponse.data
                            updateCategoryList()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "加载子分类失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "服务器响应错误: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCategoryList() {
        val items = mutableListOf<PracticeItem>()

        categories.forEach { category ->
            val isExpanded = expandedCategories.contains(category.id)
            val subcategories = if (isExpanded) categorySubcategories[category.id] ?: emptyList() else emptyList()

            items.add(PracticeItem.CategoryItem(category, isExpanded, subcategories))

            if (isExpanded) {
                categorySubcategories[category.id]?.forEach { sub ->
                    items.add(PracticeItem.SubcategoryItem(sub))
                }
            }
        }

        practiceAdapter.updateItems(items)
    }

    private fun loadSavedQuestionCount() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedQuestionCount = prefs.getInt(KEY_QUESTION_COUNT, 10)
    }

    private fun saveQuestionCount(count: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt(KEY_QUESTION_COUNT, count)
            apply()
        }
        selectedQuestionCount = count
    }

    private fun initCustomQuizDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_custom_quiz, null)

        customQuizDialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        customQuizDialog.setContentView(dialogView)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        numberPicker.minValue = 5
        numberPicker.maxValue = 20
        numberPicker.value = getSavedQuestionCount()
        numberPicker.wrapSelectorWheel = false
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        try {
            removeDivider(numberPicker)
        } catch (_: Exception) {}

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val selectedCount = numberPicker.value
            saveQuestionCount(selectedCount)
            Toast.makeText(requireContext(), "你选择了 ${selectedCount} 道题", Toast.LENGTH_SHORT).show()
            customQuizDialog.dismiss()
        }
    }

    private fun removeDivider(numberPicker: NumberPicker) {
        val pickerFields = NumberPicker::class.java.declaredFields
        for (field in pickerFields) {
            if (field.name == "mSelectionDivider") {
                field.isAccessible = true
                field.set(numberPicker, null)
            }
        }
    }

    fun getSavedQuestionCount(): Int = selectedQuestionCount

    override fun onResume() {
        super.onResume()
        startBannerAutoScroll(bannerInterval)
    }

    override fun onPause() {
        super.onPause()
        stopBannerAutoScroll()
    }

    override fun onDestroyView() {
        _binding?.viewPager?.unregisterOnPageChangeCallback(bannerPageCallback)
        stopBannerAutoScroll()
        _binding = null
        if (::customQuizDialog.isInitialized && customQuizDialog.isShowing) {
            customQuizDialog.dismiss()
        }
        super.onDestroyView()
    }

    private fun Int.dpToPx(): Int {
        return (this * requireContext().resources.displayMetrics.density).toInt()
    }
}

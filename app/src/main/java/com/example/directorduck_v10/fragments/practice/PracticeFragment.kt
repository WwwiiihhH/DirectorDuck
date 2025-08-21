package com.example.directorduck_v10.fragments.practice

import android.app.Dialog
import android.content.Context
import android.os.Bundle
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
import com.example.directorduck_v10.QuizActivity
import com.example.directorduck_v10.R
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.FragmentPracticeBinding
import com.example.directorduck_v10.fragments.practice.adapters.BannerAdapter
import com.example.directorduck_v10.fragments.practice.adapters.HorizontalImageAdapter
import com.example.directorduck_v10.fragments.practice.adapters.PracticeAdapter
import com.example.directorduck_v10.fragments.practice.data.Category
import com.example.directorduck_v10.fragments.practice.data.ImageItem
import com.example.directorduck_v10.fragments.practice.data.PracticeItem
import com.example.directorduck_v10.fragments.practice.data.Subcategory
import com.example.directorduck_v10.viewmodel.SharedUserViewModel
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


    private val bannerImages = listOf(
        R.drawable.logonew,
        R.drawable.logo,
        R.drawable.logonew
    )

    private val horizontalItems = listOf(
        ImageItem(R.drawable.icon_100, "图像1"),
        ImageItem(R.drawable.app_iocn_manager, "图像2"),
        ImageItem(R.drawable.app_iocn_int, "图像3"),
        ImageItem(R.drawable.app_iocn_myhomework, "图像4")
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

        // 从SharedPreferences加载保存的题目数量
        loadSavedQuestionCount()

        // 初始化自定义刷题对话框
        initCustomQuizDialog()

        // 设置自定义刷题按钮点击事件
        binding.btnCustomQuiz.setOnClickListener {
            customQuizDialog.show()
        }

        Log.d("dadawd", "onCreateView: ${getSavedQuestionCount()}")

        return binding.root
    }

    private fun setupBanner() {
        binding.viewPager.adapter = BannerAdapter(bannerImages)
    }

    private fun setupHorizontalList() {
        binding.recyclerHorizontal.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerHorizontal.adapter = HorizontalImageAdapter(horizontalItems) { item ->
            Toast.makeText(requireContext(), "点击了：${item.title}", Toast.LENGTH_SHORT).show()
            // 这里可以添加跳转逻辑
        }
    }

    private fun setupCategoriesList() {
        practiceAdapter = PracticeAdapter(
            onCategoryClick = { categoryId ->
                // 点击大类名称，跳转到答题页面
                val category = categories.find { it.id == categoryId }
                if (category != null) {
                    // 获取保存的题目数量
                    val questionCount = getSavedQuestionCount()

                    // 获取用户信息
                    val user = sharedUserViewModel.user.value
                    if (user != null) {
                        // 跳转到答题页面
                        QuizActivity.startQuizActivity(
                            context = requireContext(),
                            user = user, // 传递用户信息
                            categoryId = categoryId,
                            categoryName = category.categoryName,
                            questionCount = questionCount
                        )
                    } else {
                        Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCategoryToggle = { categoryId ->
                // 点击展开图标控制展开/收起
                toggleCategory(categoryId)
            },
            onSubcategoryClick = { subcategoryId ->
                // 点击小类，跳转到答题页面
                val subcategory = categorySubcategories.values.flatten().find { it.id == subcategoryId }
                if (subcategory != null) {
                    // 获取保存的题目数量
                    val questionCount = getSavedQuestionCount()

                    // 找到对应的大类名称
                    val categoryName = categories.find { it.id == subcategory.categoryId }?.categoryName

                    // 获取用户信息
                    val user = sharedUserViewModel.user.value
                    if (user != null) {
                        // 跳转到答题页面
                        QuizActivity.startQuizActivity(
                            context = requireContext(),
                            user = user, // 传递用户信息
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
            // 收起
            expandedCategories.remove(categoryId)
        } else {
            // 展开
            expandedCategories.add(categoryId)
            // 加载子类别（如果还没有加载过）
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
            val subcategories = if (isExpanded) {
                categorySubcategories[category.id] ?: emptyList()
            } else {
                emptyList()
            }

            items.add(PracticeItem.CategoryItem(category, isExpanded, subcategories))

            if (isExpanded) {
                categorySubcategories[category.id]?.forEach { subcategory ->
                    items.add(PracticeItem.SubcategoryItem(subcategory))
                }
            }
        }

        practiceAdapter.updateItems(items)
    }


    // 从SharedPreferences加载保存的题目数量
    private fun loadSavedQuestionCount() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedQuestionCount = prefs.getInt(KEY_QUESTION_COUNT, 10) // 默认值为10
    }

    // 保存题目数量到SharedPreferences
    private fun saveQuestionCount(count: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt(KEY_QUESTION_COUNT, count)
            apply() // 异步保存
        }
        selectedQuestionCount = count
    }



    private fun initCustomQuizDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_custom_quiz, null)

        customQuizDialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        customQuizDialog.setContentView(dialogView)

        // 设置NumberPicker
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)

        // 设置NumberPicker的范围
        numberPicker.minValue = 5
        numberPicker.maxValue = 20

        // 设置默认值
        numberPicker.value = getSavedQuestionCount()

        // 设置显示格式
        numberPicker.wrapSelectorWheel = false

        // 设置NumberPicker的显示样式
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        // 尝试移除分割线（忽略错误）
        try {
            removeDivider(numberPicker)
        } catch (e: Exception) {
            // 忽略反射错误
        }

        // 确定按钮点击事件
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val selectedCount = numberPicker.value

            // 保存用户选择的题目数量
            saveQuestionCount(selectedCount)

            Toast.makeText(requireContext(), "你选择了 ${selectedCount} 道题", Toast.LENGTH_SHORT).show()
            customQuizDialog.dismiss()



            // 这里可以添加实际的业务逻辑
            // getRandomQuestions(selectedCount)
        }
    }


    private fun removeDivider(numberPicker: NumberPicker) {
        try {
            val pickerFields = NumberPicker::class.java.declaredFields
            for (field in pickerFields) {
                if (field.name == "mSelectionDivider") {
                    field.isAccessible = true
                    field.set(numberPicker, null) // 移除分割线
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 如果你需要在其他地方使用保存的题目数量，可以添加这个方法
    fun getSavedQuestionCount(): Int {
        return selectedQuestionCount
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (customQuizDialog.isShowing) {
            customQuizDialog.dismiss()
        }
    }

    // dp转px工具方法
    private fun Int.dpToPx(): Int {
        return (this * requireContext().resources.displayMetrics.density).toInt()
    }
}

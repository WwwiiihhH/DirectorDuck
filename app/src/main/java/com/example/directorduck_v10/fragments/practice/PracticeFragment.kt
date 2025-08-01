package com.example.directorduck_v10.fragments.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorduck_v10.R
import com.example.directorduck_v10.databinding.FragmentPracticeBinding
import com.example.directorduck_v10.fragments.practice.adapters.BannerAdapter
import com.example.directorduck_v10.fragments.practice.adapters.HorizontalImageAdapter
import com.example.directorduck_v10.fragments.practice.data.ImageItem

class PracticeFragment : Fragment() {

    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeBinding.inflate(inflater, container, false)
        setupBanner()
        setupHorizontalList()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

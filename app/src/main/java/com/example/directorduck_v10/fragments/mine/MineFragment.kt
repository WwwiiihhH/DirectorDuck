package com.example.directorduck_v10.fragments.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.directorduck_v10.databinding.FragmentMineBinding
import com.example.directorduck_v10.viewmodel.SharedUserViewModel

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)

        // 观察用户信息
        sharedUserViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvUsername.text=user.username
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

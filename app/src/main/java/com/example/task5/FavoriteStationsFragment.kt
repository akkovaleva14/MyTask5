package com.example.task5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.task5.databinding.FragmentFavoriteStationsBinding

class FavoriteStationsFragment : Fragment() {
    private var _binding: FragmentFavoriteStationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.icBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Здесь добавьте логику для отображения избранных радиостанций
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

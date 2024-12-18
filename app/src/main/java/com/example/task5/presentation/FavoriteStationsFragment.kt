package com.example.task5.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.databinding.FragmentFavoriteStationsBinding

class FavoriteStationsFragment : Fragment() {
    private var _binding: FragmentFavoriteStationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoriteStationsAdapter: FavoriteStationsAdapter
    private val viewModel: FavoriteStationsViewModel by viewModels {
        FavoriteStationsViewModelFactory(AppDatabase.getDatabase(requireContext()))
    }

    // Получение экземпляра базы данных
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StationsListFragment())
                .addToBackStack(null)
                .commit()
        }

        // Загрузка любимых станций через ViewModel
        viewModel.loadFavoriteStations { favoriteStations ->
            setupRecyclerView(favoriteStations)
            updateEmptyStateVisibility(favoriteStations.isEmpty())
        }
    }

    private fun setupRecyclerView(favoriteStations: List<FavoriteStation>) {
        favoriteStationsAdapter = FavoriteStationsAdapter(
            favoriteStations,
            onPlayPauseClick = { station ->
                // Обработка нажатия кнопки воспроизведения/паузы для отдельной станции
            },
            onLikeClick = { station ->
                // Обработка нажатия кнопки "нравится" для отдельной станции
            },
            database // Теперь передаем базу данных
        )

        binding.recyclerViewFavoriteStations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteStationsAdapter
        }

        updateEmptyStateVisibility(favoriteStations.isEmpty())
    }

    private fun updateEmptyStateVisibility(isEmpty: Boolean) {
        binding.emptyStateImage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewFavoriteStations.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
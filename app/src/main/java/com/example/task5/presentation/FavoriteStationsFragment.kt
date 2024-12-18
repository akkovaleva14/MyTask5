package com.example.task5.presentation

import android.os.Bundle
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
                // Handle play/pause action for individual station
            },
            onLikeClick = { station ->
                // Удаление радиостанции из избранного через ViewModel
                viewModel.deleteStation(station.url) { updatedStations ->
                    favoriteStationsAdapter.updateStations(updatedStations)
                    // Обновление likedStations в адаптере
                    favoriteStationsAdapter.updateLikedStations(updatedStations)
                    updateEmptyStateVisibility(updatedStations.isEmpty())
                }
            }
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

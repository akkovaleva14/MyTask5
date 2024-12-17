package com.example.task5.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.databinding.FragmentFavoriteStationsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteStationsFragment : Fragment() {
    private var _binding: FragmentFavoriteStationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoriteStationsAdapter: FavoriteStationsAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.backButton.setOnClickListener {
//            findNavController().navigate(R.id.action_favoriteStationsFragment_to_stationsListFragment)
//        }
        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StationsListFragment())
                .addToBackStack(null)
                .commit()
        }

        // Initialize the database
        database = AppDatabase.getDatabase(requireContext())

        // Load favorite stations
        CoroutineScope(Dispatchers.IO).launch {
            val favoriteStations = database.favoriteStationDao().getAllFavoriteStations()
            withContext(Dispatchers.Main) {
                setupRecyclerView(favoriteStations)
            }
        }

        binding.playPauseButtonMain.setOnClickListener {
            // Handle play/pause action
        }
    }

    private fun setupRecyclerView(favoriteStations: List<FavoriteStation>) {
        favoriteStationsAdapter = FavoriteStationsAdapter(
            favoriteStations,
            onPlayPauseClick = { station ->
                // Handle play/pause action for individual station
            },
            onLikeClick = { station ->
                // Handle like/unlike action for individual station
            }
        )

        binding.recyclerViewFavoriteStations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteStationsAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

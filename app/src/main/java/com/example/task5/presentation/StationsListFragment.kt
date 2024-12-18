package com.example.task5.presentation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.task5.AudioService
import com.example.task5.AudioService.Companion.NOTIFICATION_ID
import com.example.task5.PlaybackReceiver
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.databinding.FragmentStationsListBinding

class StationsListFragment : Fragment(), PlaybackStateListener {
    private var _binding: FragmentStationsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var stationsAdapter: StationsAdapter
    private lateinit var database: AppDatabase
    private val viewModel: StationsListViewModel by viewModels {
        StationsListViewModelFactory(
            requireContext(),
            database
        )
    }

    private lateinit var playbackReceiver: PlaybackReceiver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationsListBinding.inflate(inflater, container, false)
        database = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve stations from ViewModel
        val stations = viewModel.getStations()

        // Initialize the adapter with the stations
        stationsAdapter = StationsAdapter(stations.toList(), requireActivity(), database)
        binding.recyclerView.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Load favorite stations
        viewModel.loadFavoriteStations()
        viewModel.favoriteStations.observe(viewLifecycleOwner, Observer { favoriteStations ->
            stationsAdapter.likedStations.clear() // Очистите старые значения
            favoriteStations.forEach { station ->
                stationsAdapter.likedStations.add(station.url)
            }
            stationsAdapter.notifyDataSetChanged() // Обновите адаптер
        })

        // Observing ViewModel LiveData
        viewModel.isPlaying.observe(viewLifecycleOwner, Observer { isPlaying ->
            updatePlayPauseButton(isPlaying, viewModel.isLoading.value ?: false)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            updatePlayPauseButton(viewModel.isPlaying.value ?: false, isLoading)
        })

        viewModel.currentStation.observe(viewLifecycleOwner, Observer { station ->
            stationsAdapter.updateStationState(
                station ?: "",
                viewModel.isPlaying.value ?: false,
                viewModel.isLoading.value ?: false
            )
        })

        binding.playPauseButtonMain.setOnClickListener { handlePlayPauseButtonClick() }

        playbackReceiver = PlaybackReceiver()
        requireActivity().registerReceiver(playbackReceiver, IntentFilter("UPDATE_PLAYBACK_STATE"))

        binding.goToFavButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FavoriteStationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun handlePlayPauseButtonClick() {
        // Toggle playback state
        val isCurrentlyPlaying = viewModel.isPlaying.value ?: false
        if (isCurrentlyPlaying) {
            // Pause the audio
            requireActivity().startService(
                Intent(
                    requireActivity(),
                    AudioService::class.java
                ).apply {
                    action = "PAUSE"
                })
        } else {
            // Play the audio
            requireActivity().startService(
                Intent(
                    requireActivity(),
                    AudioService::class.java
                ).apply {
                    action = "PLAY"
                    putExtra(
                        "stationUrl",
                        viewModel.currentStation.value
                    ) // Pass the current station URL
                })
        }
    }

    override fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        viewModel.savePlaybackState(isPlaying, isLoading, station)
        // Additional UI updates if needed
    }

    private fun updatePlayPauseButton(isPlaying: Boolean, isLoading: Boolean) {
        binding.playPauseButtonMain.apply {
            setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(playbackReceiver)
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().startService(Intent(requireActivity(), AudioService::class.java).apply {
            action = "STOP"
        })

        val notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

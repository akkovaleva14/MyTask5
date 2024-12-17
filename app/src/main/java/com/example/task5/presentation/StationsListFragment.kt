package com.example.task5.presentation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.task5.AudioService
import com.example.task5.AudioService.Companion.NOTIFICATION_ID
import com.example.task5.PlaybackReceiver
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.databinding.FragmentStationsListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StationsListFragment : Fragment(), PlaybackStateListener {
    private var _binding: FragmentStationsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var stationAdapter: StationAdapter
    private lateinit var database: AppDatabase

    // Map URLs to station names
    private val stations = mapOf(
        "https://radio.plaza.one/mp3" to "Plaza Radio",
        "https://hermitage.hostingradio.ru/hermitage128.mp3" to "Hermitage Radio",
        "https://radiorecord.hostingradio.ru/sd9096.aacp" to "Record Radio 9096",
        "https://radiorecord.hostingradio.ru/christmas96.aacp" to "Christmas Radio 96",
        "https://radiorecord.hostingradio.ru/beach96.aacp" to "Beach Radio 96",
        "https://radiorecord.hostingradio.ru/hypno96.aacp" to "Hypno Radio 96",
        "https://radiorecord.hostingradio.ru/198096.aacp" to "Record Radio 198096"
    )

    private var lastPlayedStation: String? = null
    private lateinit var playbackReceiver: PlaybackReceiver
    private lateinit var sharedPreferences: SharedPreferences

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

        sharedPreferences = requireActivity().getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("isPlaying", false)
            putBoolean("isLoading", false)
            apply()
        }

        // Initialize the adapter with station names
        stationAdapter = StationAdapter(stations.toList(), requireActivity(), database)
        binding.recyclerView.apply {
            adapter = stationAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Load favorite stations from local storage
        CoroutineScope(Dispatchers.IO).launch {
            val favoriteStations = database.favoriteStationDao().getAllFavoriteStations()
            favoriteStations.forEach { station ->
                stationAdapter.likedStations.add(station.id)
            }
            withContext(Dispatchers.Main) {
                stationAdapter.notifyDataSetChanged()
            }
        }

        binding.playPauseButtonMain.setOnClickListener { handlePlayPauseButtonClick() }

        playbackReceiver = PlaybackReceiver()
        requireActivity().registerReceiver(playbackReceiver, IntentFilter("UPDATE_PLAYBACK_STATE"))

        binding.goToFavButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FavoriteStationsFragment())
                .addToBackStack(null)
                .commit()
        }

        updateUIFromPreferences()
    }

    private fun handlePlayPauseButtonClick() {
        if (sharedPreferences.getBoolean("isLoading", false)) return // Block click if loading
        if (stationAdapter.isAnyStationPlaying()) {
            pauseAnyStation(lastPlayedStation)
        } else {
            playStation(lastPlayedStation ?: stations.keys.first())
        }
    }

    override fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        stationAdapter.resetCurrentPlayingStation()

        station?.let {
            stationAdapter.updateStationState(station, isPlaying, isLoading)
        }

        lastPlayedStation = if (isPlaying) station else lastPlayedStation
        updatePlayPauseButton(isPlaying, isLoading)
        savePlaybackState(isPlaying, isLoading, station)
    }

    private fun savePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        sharedPreferences.edit().apply {
            putBoolean("isPlaying", isPlaying)
            putBoolean("isLoading", isLoading)
            putString("currentStation", station)
            apply()
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean, isLoading: Boolean) {
        binding.playPauseButtonMain.apply {
            setImageResource(
                when {
                    isPlaying -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                }
            )
            isEnabled = !isLoading // Disable button during loading
        }
    }

    private fun pauseAnyStation(station: String?) {
        requireActivity().startService(Intent(requireActivity(), AudioService::class.java).apply {
            action = "PAUSE"
        })
        updatePlaybackState(isPlaying = false, isLoading = false, station)
        savePlaybackState(isPlaying = false, isLoading = false, station)
    }

    private fun playStation(station: String) {
        requireActivity().startService(Intent(requireActivity(), AudioService::class.java).apply {
            putExtra("STATION_NAME", station)
            action = "PLAY"
        })
        updatePlaybackState(isPlaying = true, isLoading = false, station = station)
        updateNotificationFromActivity(station)
    }

    private fun updateNotificationFromActivity(station: String) {
        requireActivity().startService(Intent(requireActivity(), AudioService::class.java).apply {
            action = "UPDATE_NOTIFICATION"
            putExtra("STATION_NAME", station)
        })
    }

    override fun onResume() {
        super.onResume()
        updateUIFromPreferences()
    }

    private fun updateUIFromPreferences() {
        val isPlaying = sharedPreferences.getBoolean("isPlaying", false)
        val isLoading = sharedPreferences.getBoolean("isLoading", false)
        lastPlayedStation = sharedPreferences.getString("currentStation", null)

        updatePlayPauseButton(isPlaying, isLoading)

        // Update the state of the station list
        lastPlayedStation?.let { station ->
            stationAdapter.updateStationState(station, isPlaying, isLoading)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(playbackReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().startService(Intent(requireActivity(), AudioService::class.java).apply {
            action = "STOP"
        })

        val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

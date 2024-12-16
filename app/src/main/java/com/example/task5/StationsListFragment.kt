package com.example.task5

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
import com.example.task5.AudioService.Companion.NOTIFICATION_ID
import com.example.task5.databinding.FragmentStationsListBinding

class StationsListFragment : Fragment(), PlaybackStateListener {
    private var _binding: FragmentStationsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var stationAdapter: StationAdapter
    private val stations = listOf(
        "https://radio.plaza.one/mp3",
        "https://hermitage.hostingradio.ru/hermitage128.mp3",
        "https://radiorecord.hostingradio.ru/sd9096.aacp",
        "https://radiorecord.hostingradio.ru/christmas96.aacp",
        "https://radiorecord.hostingradio.ru/beach96.aacp",
        "https://radiorecord.hostingradio.ru/hypno96.aacp",
        "https://radiorecord.hostingradio.ru/198096.aacp"
    )
    private var lastPlayedStation: String? = null
    private lateinit var playbackReceiver: PlaybackReceiver
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationsListBinding.inflate(inflater, container, false)
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

        stationAdapter = StationAdapter(stations, requireActivity())
        binding.recyclerView.apply {
            adapter = stationAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.playPauseButtonMain.setOnClickListener { handlePlayPauseButtonClick() }

        playbackReceiver = PlaybackReceiver()
        requireActivity().registerReceiver(playbackReceiver, IntentFilter("UPDATE_PLAYBACK_STATE"))

        binding.icFavorite.setOnClickListener {
            // Переход на FavoriteStationsFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FavoriteStationsFragment())
                .addToBackStack(null)
                .commit()
        }

        updateUIFromPreferences()
    }

    private fun handlePlayPauseButtonClick() {
        if (sharedPreferences.getBoolean("isLoading", false)) return // Блокируем нажатие, если идет загрузка
        if (stationAdapter.isAnyStationPlaying()) {
            pauseAnyStation(lastPlayedStation)
        } else {
            playStation(lastPlayedStation ?: stations.first())
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
            isEnabled = !isLoading // Блокировка кнопки во время загрузки
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

        // Обновляем состояние списка станций
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

package com.example.task5

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.task5.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)

        stationAdapter = StationAdapter(stations, this)
        binding.recyclerView.apply {
            adapter = stationAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        binding.playPauseButtonMain.setOnClickListener { handlePlayPauseButtonClick() }

        playbackReceiver = PlaybackReceiver()
        val filter = IntentFilter("UPDATE_PLAYBACK_STATE")
        registerReceiver(playbackReceiver, filter)

        updateUIFromPreferences() // Initialize UI state from preferences
    }

    private fun handlePlayPauseButtonClick() {
        if (stationAdapter.isAnyStationPlaying()) {
            // If any station is playing, pause all stations
            pauseAllStations()
        } else {
            playStation(lastPlayedStation ?: stations.first())
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        station?.let {
            stationAdapter.updateStationState(station, isPlaying, !isLoading)
        } ?: stationAdapter.resetAllStations()

        lastPlayedStation = if (isPlaying) station else lastPlayedStation
        updatePlayPauseButton(isPlaying, isLoading)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean, isLoading: Boolean) {
        binding.playPauseButtonMain.setImageResource(
            when {
                isLoading -> R.drawable.ic_loading
                isPlaying -> R.drawable.ic_pause
                else -> R.drawable.ic_play
            }
        )
        binding.playPauseButtonMain.isEnabled = !isLoading // Disable button during loading
    }

    private fun pauseAllStations() {
        startService(Intent(this, AudioService::class.java).apply {
            action = "PAUSE_ALL"
        })
        updatePlaybackState(isPlaying = false, isLoading = false, station = null)
    }

    private fun playStation(station: String) {
        startService(Intent(this, AudioService::class.java).apply {
            putExtra("STATION_NAME", station)
            action = "PLAY"
        })
        updatePlaybackState(isPlaying = true, isLoading = false, station = station)
    }

    override fun onResume() {
        super.onResume()
        updateUIFromPreferences() // Update UI from SharedPreferences
    }

    private fun updateUIFromPreferences() {
        val isPlaying = sharedPreferences.getBoolean("isPlaying", false)
        val station = sharedPreferences.getString("currentStation", null)

        updatePlayPauseButton(isPlaying, isLoading = false) // Always false for the initial state
        lastPlayedStation = station
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(playbackReceiver) // Unregister the receiver when the activity is destroyed
    }
}

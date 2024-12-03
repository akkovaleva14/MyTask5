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
    //  private lateinit var playbackPrefs: PlaybackPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
        //    playbackPrefs = PlaybackPreferences(this)
        stationAdapter = StationAdapter(stations, this)
        binding.recyclerView.apply {
            adapter = stationAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        binding.playPauseButtonMain.setOnClickListener { handlePlayPauseButtonClick() }

        playbackReceiver = PlaybackReceiver()
        registerReceiver(playbackReceiver, IntentFilter("UPDATE_PLAYBACK_STATE"))

        updateUIFromPreferences() // Initialize UI state from preferences
    }

    private fun handlePlayPauseButtonClick() {
        if (stationAdapter.isAnyStationPlaying()) {
            pauseAnyStation()
        } else {
            playStation(lastPlayedStation ?: stations.first())
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        station?.let {
            stationAdapter.updateStationState(station, isPlaying, !isLoading)
        } ?: stationAdapter.resetCurrentPlayingStation()

        lastPlayedStation = if (isPlaying) station else lastPlayedStation
        updatePlayPauseButton(isPlaying, isLoading)
     //   savePlaybackState(isPlaying, isLoading, station)
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

    private fun pauseAnyStation() {
        startService(Intent(this, AudioService::class.java).apply {
            action = "PAUSE"
        })
        updatePlaybackState(isPlaying = false, isLoading = false, station = null)
    }

    private fun playStation(station: String) {
        startService(Intent(this, AudioService::class.java).apply {
            putExtra("STATION_NAME", station)
            action = "PLAY"
        })
        updatePlaybackState(isPlaying = true, isLoading = false, station = station)
        //       updatePlaybackState(isPlaying = true, isLoading = true, station = station)
    }

    override fun onResume() {
        super.onResume()
        updateUIFromPreferences()
    }

    //    private fun updateUIFromPreferences() {
//        updatePlayPauseButton(
//            sharedPreferences.getBoolean("isPlaying", false),
//            sharedPreferences.getBoolean("isLoading", false),
//        )
//        lastPlayedStation = sharedPreferences.getString("currentStation", null)
//    }
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


//    private fun savePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
//        sharedPreferences.edit().apply {
//            putBoolean("isPlaying", isPlaying)
//            putBoolean("isLoading", isLoading)
//            putString("currentStation", station)
//            apply() // Асинхронное сохранение
//        }
//    }

    override fun onStart() {
        super.onStart()
        registerReceiver(playbackReceiver, IntentFilter("UPDATE_PLAYBACK_STATE"))
    }

//    override fun onStop() {
//        super.onStop()
//        unregisterReceiver(playbackReceiver)
//    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(playbackReceiver)
    }
}

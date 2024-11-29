package com.example.task5

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
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
    private lateinit var playPauseButton: ImageButton
    private var lastPlayedStation: String? = null
    private lateinit var playbackReceiver: PlaybackReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        stationAdapter = StationAdapter(stations, this)
        recyclerView.adapter = stationAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        playPauseButton = findViewById(R.id.playPauseButton)
        playPauseButton.setOnClickListener {
            handlePlayPauseButtonClick()
        }
        playbackReceiver = PlaybackReceiver()
        val filter = IntentFilter("UPDATE_PLAYBACK_STATE")
        registerReceiver(playbackReceiver, filter)
    }

    private fun handlePlayPauseButtonClick() {
        if (stationAdapter.isAnyStationPlaying()) {
            // Если какая-то радиостанция играет, то ставим на паузу
            pauseAllStations()
        } else {
            // Если ничего не играет
            if (lastPlayedStation != null) {
                // Запускаем последнюю игравшую радиостанцию
                playStation(lastPlayedStation!!)
            } else {
                // Запускаем первую радиостанцию из списка
                playStation(stations.first())
            }
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, station: String?) {
        playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        lastPlayedStation = if (isPlaying) station else lastPlayedStation // Сохраняем последнюю игравшую радиостанцию
        stationAdapter.updatePlaybackState(isPlaying, station) // Обновляем состояние в адаптере
    }

    private fun pauseAllStations() {
        val intent = Intent(this, AudioService::class.java).apply {
            action = "PAUSE_ALL"
        }
        startService(intent)
        updatePlaybackState(false, null)
    }

    private fun playStation(station: String) {
        val intent = Intent(this, AudioService::class.java).apply {
            putExtra("STATION_NAME", station)
            action = "PLAY"
        }
        startService(intent)
        updatePlaybackState(true, station)
    }

    override fun onResume() {
        super.onResume()
        updateUIFromPreferences() // Обновляем UI из SharedPreferences
    }

    private fun updateUIFromPreferences() {
        val sharedPreferences = getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
        val isPlaying = sharedPreferences.getBoolean("isPlaying", false)
        val currentStation = sharedPreferences.getString("currentStation", null)

        // Обновите состояние кнопки
        playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        lastPlayedStation = currentStation // Обновите последнюю станцию
        stationAdapter.updatePlaybackState(isPlaying, currentStation) // Обновите адаптер
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(playbackReceiver) // Отменяем регистрацию при уничтожении
    }

}

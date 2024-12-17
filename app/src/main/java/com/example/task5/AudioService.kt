package com.example.task5

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.task5.presentation.MainActivity
import com.example.task5.presentation.StationState

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class AudioService : Service() {

    companion object {
        const val NOTIFICATION_ID = 2024
    }

    private var exoPlayer: ExoPlayer? = null
    private lateinit var notificationManager: NotificationManager
    private val stationStates = mutableMapOf<String, StationState>()
    private var currentStation: String? = null
    private val defaultStation = "https://radio.plaza.one/mp3"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        stationStates[defaultStation] = StationState()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            stationStates[currentStation]?.let {
                                it.isPlaying = true
                                it.isLoading = false
                            }
                            updateNotification(currentStation.orEmpty())
                            sendPlaybackStateUpdate()
                        }

                        Player.STATE_BUFFERING -> {
                            stationStates[currentStation]?.isPlaying = false
                            stationStates[currentStation]?.isLoading = true
                            updateNotification(currentStation.orEmpty())
                        }

                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            stationStates[currentStation]?.isLoading = false
                            stationStates[currentStation]?.isPlaying = false
                            updateNotification(currentStation.orEmpty())
                            sendPlaybackStateUpdate()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    stationStates[currentStation]?.let {
                        it.isPlaying = false
                        it.isLoading = false
                    }
                    updateNotification(currentStation.orEmpty())
                    sendPlaybackStateUpdate()
                }
            })
        }
    }

    private fun playStream(station: String) {
        val stationState = stationStates.getOrPut(station) { StationState() }
        stationState.isLoading = true
        updateNotification(station)

        exoPlayer?.apply {
            stop()
            setMediaItem(MediaItem.fromUri(station))
            prepare()
            play()
        }
        currentStation = station
        saveCurrentStation(station)
    }

    private fun saveCurrentStation(station: String) {
        val sharedPreferences = getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("currentStation", station)
            apply()
        }
    }

    private fun pauseStream(station: String) {
        val stationState = stationStates[station] ?: return
        if (stationState.isPlaying) {
            exoPlayer?.pause()
            stationState.isPlaying = false
            stationState.isLoading = false
            updateNotification(station)
            sendPlaybackStateUpdate()
        }
        //stopSelf()
       // stopForeground(false)
    }

    private fun stopStream(station: String) {
        val stationState = stationStates[station] ?: return
        if (stationState.isPlaying) {
            exoPlayer?.stop()
            stationState.isPlaying = false
            stationState.isLoading = false
            updateNotification(station)
            sendPlaybackStateUpdate()
        }
        stopSelf()
    }

    private fun sendPlaybackStateUpdate() {
        val currentStationState = stationStates[currentStation] ?: return
        sendBroadcast(Intent("UPDATE_PLAYBACK_STATE").apply {
            putExtra("isPlaying", currentStationState.isPlaying)
            putExtra("isLoading", currentStationState.isLoading)
            putExtra("currentStation", currentStation)
        })
    }

    private fun updateNotification(station: String) {
        val stationState = stationStates[station] ?: StationState()

        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, "AUDIO_CHANNEL")
                .setContentTitle(station)
                .setContentText(
                    when {
                        stationState.isLoading -> "Loading..."
                        stationState.isPlaying -> "Playing"
                        else -> "Paused"
                    }
                )
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .addAction(
                    if (stationState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (stationState.isPlaying) "Pause" else "Play",
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, AudioService::class.java).apply {
                            action = if (stationState.isPlaying) "PAUSE" else "PLAY"
                            putExtra("STATION_NAME", station)
                        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setDeleteIntent(
                    PendingIntent.getService(
                        this, 0, Intent(this, AudioService::class.java).apply {
                            action = "STOP"
                            putExtra("STATION_NAME", station)
                        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "AUDIO_CHANNEL", "Audio Playback", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Channel for audio playback notifications" }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val station = intent?.getStringExtra("STATION_NAME") ?: defaultStation
        currentStation = station

        when (intent?.action) {
            "UPDATE_NOTIFICATION" -> {
                updateNotification(station)
            }

            "PLAY" -> playStream(station)
            "PAUSE" -> pauseStream(station)
            "STOP" -> stopStream(station)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        serviceScope.cancel()
        stopSelf()
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
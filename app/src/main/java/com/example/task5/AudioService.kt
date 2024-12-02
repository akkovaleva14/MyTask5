package com.example.task5

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class AudioService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
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
    }

    private fun playStream(station: String) {
        val stationState = stationStates.getOrPut(station) { StationState() }
        stationState.isLoading = true
        updateNotification(station)

        if (!::mediaPlayer.isInitialized) {
            mediaPlayer = MediaPlayer()
        }

        mediaPlayer.apply {
            reset()
            setDataSource(station)
            setOnPreparedListener {
                stationState.isPlaying = true
                stationState.isLoading = false
                currentStation = station
                start()
                updateNotification(station)
                sendPlaybackStateUpdate()
            }
            setOnCompletionListener {
                stationState.isPlaying = false
                updateNotification(station)
            }
            prepareAsync()
        }
    }

    private fun pauseStream(station: String) {
        val stationState = stationStates[station] ?: return
        if (::mediaPlayer.isInitialized && stationState.isPlaying) {
            mediaPlayer.pause()
            stationState.isPlaying = false
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
            1,
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
                    if (stationState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, // TODO
                    if (stationState.isPlaying) "Pause" else "Play",
                    PendingIntent.getService(
                        this, 0, Intent(this, AudioService::class.java).apply {
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
                .setAutoCancel(true)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                "AUDIO_CHANNEL", "Audio Playback", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Channel for audio playback notifications" })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val station = intent?.getStringExtra("STATION_NAME") ?: defaultStation
        currentStation = station

        when (intent?.action) {
            "PLAY" -> playStream(station)
            "PAUSE" -> pauseStream(station)
            "STOP" -> stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        serviceScope.cancel()
    }
}

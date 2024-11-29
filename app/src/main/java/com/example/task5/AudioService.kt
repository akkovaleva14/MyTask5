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

class AudioService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private var isPlaying = false
    private var currentStation: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun playStream(station: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(station) // Укажите URL потока
            prepareAsync()
            setOnPreparedListener { start() }
        }
        isPlaying = true
    }

    private fun pauseStream() {
        mediaPlayer.pause()
        isPlaying = false
    }

    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Создание Intent для кнопки Play/Pause
        val playPauseIntent = Intent(this, AudioService::class.java).apply {
            action = if (isPlaying) "PAUSE" else "PLAY"
            putExtra("STATION_NAME", currentStation) // Убедитесь, что передаете текущую станцию
        }
        val playPausePendingIntent =
            PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "AUDIO_CHANNEL")
            .setContentTitle(currentStation)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .setOngoing(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AUDIO_CHANNEL",
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for audio playback notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentStation = intent?.getStringExtra("STATION_NAME")

        when (intent?.action) {
            "PLAY" -> {
                if (!isPlaying) {
                    playStream(currentStation!!)
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        true,
                        currentStation
                    ) // Обновляем состояние
                }
            }

            "PAUSE" -> {
                if (isPlaying) {
                    pauseStream()
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        false,
                        currentStation
                    ) // Обновляем состояние
                    showNotification() // Обновляем уведомление
                }
            }

            "PAUSE_ALL" -> {
                pauseStream()
                (application as MyApplication).mainActivity?.updatePlaybackState(
                    false,
                    null
                ) // Обновляем состояние
                showNotification() // Обновляем уведомление
            }

            else -> {
                playStream(currentStation!!)
                (application as MyApplication).mainActivity?.updatePlaybackState(
                    true,
                    currentStation
                ) // Обновляем состояние
            }
        }

        showNotification()
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}

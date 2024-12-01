package com.example.task5

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private var isPlaying = false
    private var isLoading = false
    private var currentStation: String? = null
    private var defaultStation: String = "https://radio.plaza.one/mp3"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun playStream(station: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(station) // Укажите URL потока
                setOnPreparedListener {
                    this@AudioService.isPlaying = true
                    // isPlaying = true
                    currentStation = station
                    val endTime = System.currentTimeMillis()
                    Log.d("AudioService", "Preparation time: ${endTime - startTime} ms")
                    start()
                    savePlaybackState(true) // Сохраняем состояние
                    showNotification() // Обновляем уведомление
                }
                setOnBufferingUpdateListener { _, percent ->
                    CoroutineScope(Dispatchers.Main).launch {
                        isLoading = percent < 100
                        showNotification() // Update notification on the main thread
                    }
                }
                prepareAsync() // Подготовка к воспроизведению
            }
        }
    }

    private fun pauseStream() {
        if (::mediaPlayer.isInitialized && isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            savePlaybackState(false)
            showNotification()
            sendPlaybackStateUpdate() // Передача состояния
            stopSelf() // Stop the service when music is paused
        }
    }

    private fun sendPlaybackStateUpdate() {
        val intent = Intent("UPDATE_PLAYBACK_STATE").apply {
            putExtra("isPlaying", isPlaying)
            putExtra("isLoading", false)
            putExtra("currentStation", currentStation)
        }
        sendBroadcast(intent)
    }

    private fun showNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationIntent = Intent(this@AudioService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@AudioService,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Создание Intent для кнопки Play/Pause
            val playPauseIntent = Intent(this@AudioService, AudioService::class.java).apply {
                action = if (isPlaying) "PAUSE" else "PLAY"
                putExtra("STATION_NAME", currentStation)
            }
            val playPausePendingIntent = PendingIntent.getService(
                this@AudioService,
                0,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this@AudioService, "AUDIO_CHANNEL")
                .setContentTitle(currentStation)
                .setContentText(
                    when {
                        isLoading -> "Loading..."
                        isPlaying -> "Playing"
                        else -> "Paused"
                    }
                )
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .addAction(
                    if (isPlaying) R.drawable.ic_pause else if (isLoading) R.drawable.ic_loading else R.drawable.ic_play,
                    if (isPlaying) "Pause" else if (isLoading) "Loading" else "Play",
                    playPausePendingIntent
                )
                .setOngoing(true)
                .build()

            notificationManager.notify(1, notification)

            // Отправляем обновление состояния
            withContext(Dispatchers.Main) { // Переключаемся на главный поток
                val intent = Intent("UPDATE_PLAYBACK_STATE").apply {
                    putExtra("isPlaying", isPlaying)
                    putExtra("isLoading", isLoading)
                    putExtra("currentStation", currentStation)
                }
                sendBroadcast(intent)
            }
        }
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
        currentStation = intent?.getStringExtra("STATION_NAME") ?: defaultStation

        when (intent?.action) {
            "PLAY" -> {
                if (!isPlaying) {
                    playStream(currentStation!!)
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        isPlaying = true,
                        isLoading = false,
                        currentStation
                    ) // Обновляем состояние
                }
            }

            "PAUSE" -> {
                if (isPlaying) {
                    pauseStream()
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        isPlaying = false,
                        isLoading = false,
                        currentStation
                    ) // Обновляем состояние
                }
            }

            "PAUSE_ALL" -> {
                pauseStream()
                (application as MyApplication).mainActivity?.updatePlaybackState(
                    isPlaying = false,
                    isLoading = false,
                    null
                ) // Обновляем состояние
            }

            else -> {
                if (currentStation != null) {
                    playStream(currentStation!!)
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        isPlaying = true,
                        isLoading = false,
                        currentStation
                    ) // Обновляем состояние
                }
            }
        }

        showNotification() // Обновляем уведомление
        return START_NOT_STICKY
    }

    private fun savePlaybackState(isPlaying: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val sharedPreferences = getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("isPlaying", isPlaying)
                putString("currentStation", currentStation)
                apply()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}

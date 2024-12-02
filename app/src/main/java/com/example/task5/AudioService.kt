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
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private var isPlaying = false
    private var isLoading = false
    private var currentStation: String? = null
    private var defaultStation: String = "https://radio.plaza.one/mp3"
    private val serviceScope = CoroutineScope(Dispatchers.IO) // Используем общий scope для всех корутин

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun playStream(station: String) {
        if (!::mediaPlayer.isInitialized) {
            mediaPlayer = MediaPlayer() // Инициализация MediaPlayer, если его еще нет
        }

        mediaPlayer.apply {
            reset() // Сбрасываем плеер перед новой загрузкой потока
            setDataSource(station) // Указываем URL потока
            setOnPreparedListener {
                this@AudioService.isPlaying = true
                currentStation = station
                start()
                savePlaybackState(true) // Сохраняем состояние
                showNotification() // Обновляем уведомление
            }
            prepareAsync() // Подготовка к воспроизведению
        }
    }

    private fun pauseStream() {
        if (::mediaPlayer.isInitialized && isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            savePlaybackState(false)
            showNotification()
            sendPlaybackStateUpdate() // Передача состояния
            stopSelf() // Останавливаем сервис, когда музыка приостановлена
        }
    }

    private fun sendPlaybackStateUpdate() {
        sendBroadcast(Intent("UPDATE_PLAYBACK_STATE").apply {
            putExtra("isPlaying", isPlaying)
            putExtra("isLoading", isLoading)
            putExtra("currentStation", currentStation)
        })
    }

    private fun showNotification() {
        serviceScope.launch {
            val notificationIntent = Intent(this@AudioService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@AudioService,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val playPausePendingIntent = PendingIntent.getService(
                this@AudioService,
                0,
                Intent(this@AudioService, AudioService::class.java).apply {
                    action = if (isPlaying) "PAUSE" else "PLAY"
                    putExtra("STATION_NAME", currentStation)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = PendingIntent.getService(
                this@AudioService,
                0,
                Intent(this@AudioService, AudioService::class.java).apply {
                    action = "STOP"
                },
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
                .setDeleteIntent(stopIntent) // Устанавливаем Intent для остановки при свайпе
                .setAutoCancel(true) // Разрешаем свайп для закрытия
                .build()

            notificationManager.notify(1, notification)

            // Отправляем обновление состояния
            withContext(Dispatchers.Main) { // Переключаемся на главный поток
                sendBroadcast(Intent("UPDATE_PLAYBACK_STATE").apply {
                    putExtra("isPlaying", isPlaying)
                    putExtra("isLoading", isLoading)
                    putExtra("currentStation", currentStation)
                })
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                "AUDIO_CHANNEL",
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for audio playback notifications"
            })
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

            "STOP" -> {
                if (isPlaying) {
                    pauseStream() // Останавливаем воспроизведение
                    (application as MyApplication).mainActivity?.updatePlaybackState(
                        isPlaying = false,
                        isLoading = false,
                        currentStation
                    )
                }
                stopSelf() // Полностью останавливаем сервис
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
        return START_NOT_STICKY // Гарантируем, что сервис не перезапустится
    }

    private fun savePlaybackState(isPlaying: Boolean) {
        serviceScope.launch {
            with(getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE).edit()) {
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
            mediaPlayer.stop() // Останавливаем воспроизведение
            mediaPlayer.release() // Освобождаем ресурсы
        }
        serviceScope.cancel() // Отменяем все корутины при уничтожении сервиса
    }
}

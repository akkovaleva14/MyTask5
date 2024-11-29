package com.example.task5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isPlaying = intent.getBooleanExtra("isPlaying", false)
        val currentStation = intent.getStringExtra("currentStation")

        if (context is MainActivity) {
            context.updatePlaybackState(isPlaying, currentStation)
        }

        // Обновите состояние в MainActivity
        // TODO
//        val mainActivityIntent = Intent(context, MainActivity::class.java)
//        mainActivityIntent.putExtra("isPlaying", isPlaying)
//        mainActivityIntent.putExtra("currentStation", currentStation)
//        context.startActivity(mainActivityIntent)
    }
}

package com.example.task5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isPlaying = intent.getBooleanExtra("isPlaying", false)
        val isLoading = intent.getBooleanExtra("isLoading", false)
        val currentStation = intent.getStringExtra("currentStation")

        if (context is MainActivity) {
            context.updatePlaybackState(isPlaying, isLoading, currentStation)
        }
    }
}

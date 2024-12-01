package com.example.task5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context is MainActivity) {
            context.updatePlaybackState(
                intent.getBooleanExtra("isPlaying", false),
                intent.getBooleanExtra("isLoading", false),
                intent.getStringExtra("currentStation")
            )
        }
    }
}

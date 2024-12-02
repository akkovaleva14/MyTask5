package com.example.task5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        (context as MainActivity).updatePlaybackState(
            intent?.getBooleanExtra("isPlaying", false) ?: false,
            intent?.getBooleanExtra("isLoading", false) ?: false,
            intent?.getStringExtra("currentStation")
        )
    }
}

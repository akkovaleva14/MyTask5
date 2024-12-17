package com.example.task5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.task5.presentation.PlaybackStateListener

class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val listener = context as? PlaybackStateListener
        listener?.updatePlaybackState(
            intent?.getBooleanExtra("isPlaying", false) ?: false,
            intent?.getBooleanExtra("isLoading", false) ?: false,
            intent?.getStringExtra("currentStation")
        )
    }
}

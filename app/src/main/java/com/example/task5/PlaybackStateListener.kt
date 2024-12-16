package com.example.task5

interface PlaybackStateListener {
    fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?)
}

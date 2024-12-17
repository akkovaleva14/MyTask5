package com.example.task5.presentation

interface PlaybackStateListener {
    fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?)
}

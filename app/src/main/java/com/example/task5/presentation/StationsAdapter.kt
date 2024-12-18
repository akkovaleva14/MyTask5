package com.example.task5.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.AudioService
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.data.addStationToFirebase
import com.example.task5.data.removeStationFromFirebase
import com.example.task5.databinding.ItemStationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StationsAdapter(
    private val context: Context,
    private val database: AppDatabase
) : ListAdapter<Pair<String, String>, StationsAdapter.StationViewHolder>(StationDiffCallback()) {

    private var stationStates = mutableMapOf<String, StationState>()
    var likedStations = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val (url, name) = getItem(position)
        val state = stationStates[url] ?: StationState()
        holder.bind(name, url, state)
    }

    fun updateStationState(station: String, isPlaying: Boolean, isLoading: Boolean) {
        stationStates[station]?.apply {
            this.isPlaying = isPlaying
            this.isLoading = isLoading
        }
        notifyItemChanged(currentList.indexOfFirst { it.first == station })
    }

    fun resetCurrentPlayingStation() {
        val currentStation = stationStates.entries.find { it.value.isPlaying || it.value.isLoading }
        currentStation?.let {
            it.value.isPlaying = false
            it.value.isLoading = false
            notifyItemChanged(currentList.indexOfFirst { station -> station.first == it.key })
        }
    }

    inner class StationViewHolder(private val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, url: String, state: StationState) {
            binding.stationName.text = name

            // Play/Pause Button State
            binding.playPauseButtonItem.setImageResource(
                when {
                    state.isLoading -> R.drawable.ic_loading
                    state.isPlaying -> R.drawable.ic_music_note
                    else -> R.drawable.ic_play
                }
            )
            binding.playPauseButtonItem.isEnabled = !state.isLoading

            // Like Button State
            binding.likeButton.setImageResource(
                if (likedStations.contains(url)) R.drawable.ic_heart_dark else R.drawable.ic_heart
            )

            // Play/Pause Button Click Listener
            binding.playPauseButtonItem.setOnClickListener {
                if (!state.isLoading) {
                    resetCurrentPlayingStation()
                    stationStates[url]?.isLoading = true
                    notifyItemChanged(adapterPosition)

                    context.startService(
                        Intent(context, AudioService::class.java).apply {
                            putExtra("STATION_URL", url)
                            action = if (state.isPlaying) "PAUSE" else "PLAY"
                        }
                    )
                }
            }

            // Like Button Click Listener
            binding.likeButton.setOnClickListener {
                if (likedStations.contains(url)) {
                    likedStations.remove(url)
                    binding.likeButton.setImageResource(R.drawable.ic_heart)
                    CoroutineScope(Dispatchers.IO).launch {
                        database.favoriteStationDao().delete(url)
                        removeStationFromFirebase(sanitizeStationUrl(url))
                    }
                } else {
                    likedStations.add(url)
                    binding.likeButton.setImageResource(R.drawable.ic_heart_dark)
                    val favoriteStation = FavoriteStation(url, name, url)
                    CoroutineScope(Dispatchers.IO).launch {
                        database.favoriteStationDao().insert(favoriteStation)
                        addStationToFirebase(favoriteStation.copy(id = sanitizeStationUrl(url)))
                    }
                }
            }
        }
    }

    private fun sanitizeStationUrl(url: String): String {
        return url.replace("https://", "").replace("http://", "").replace("/", "_")
    }

    class StationDiffCallback : DiffUtil.ItemCallback<Pair<String, String>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem.first === newItem.first
        }

        override fun areContentsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem == newItem
        }
    }
}
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

class StationAdapter(
    private val stations: List<Pair<String, String>>, // List of pairs (URL, Name)
    private val context: Context,
    private val database: AppDatabase
) : ListAdapter<Pair<String, String>, StationAdapter.StationViewHolder>(StationDiffCallback()) {

    private var stationStates = mutableMapOf<String, StationState>().apply {
        stations.forEach { this[it.first] = StationState() }
    }

    var likedStations = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        return StationViewHolder(
            ItemStationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = stations.size

    fun isAnyStationPlaying(): Boolean {
        return stationStates.values.any { it.isPlaying }
    }

    fun updateStationState(station: String, isPlaying: Boolean, isLoading: Boolean) {
        stationStates[station]?.apply {
            this.isPlaying = isPlaying
            this.isLoading = isLoading
        }
        notifyItemChanged(stations.indexOfFirst { it.first == station })
    }

    fun resetCurrentPlayingStation() {
        val currentStation = stationStates.entries.find { it.value.isPlaying || it.value.isLoading }
        currentStation?.let {
            it.value.isPlaying = false
            it.value.isLoading = false
            notifyItemChanged(stations.indexOfFirst { station -> station.first == it.key })
        }
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val (url, name) = stations[position]
        holder.bind(name, stationStates[url] ?: StationState())

        // Установите состояние кнопки "нравится"
        holder.binding.likeButton.setImageResource(
            if (likedStations.contains(url)) R.drawable.ic_heart_dark else R.drawable.ic_heart
        )

        holder.binding.likeButton.setOnClickListener {
            Log.d("StationAdapter", "Like button clicked for station: $name")
            try {
                if (likedStations.contains(url)) {
                    likedStations.remove(url)
                    holder.binding.likeButton.setImageResource(R.drawable.ic_heart)
                    // Удаление станции из локального хранилища и Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        database.favoriteStationDao().delete(url)
                        val sanitizedUrl = sanitizeStationUrl(url)
                        removeStationFromFirebase(sanitizedUrl)
                        Log.d("StationAdapter", "Removed station from favorites: $name")
                    }
                } else {
                    likedStations.add(url)
                    holder.binding.likeButton.setImageResource(R.drawable.ic_heart_dark)
                    // Добавление станции в локальное хранилище и Firebase
                    val favoriteStation = FavoriteStation(url, name, url)
                    CoroutineScope(Dispatchers.IO).launch {
                        database.favoriteStationDao().insert(favoriteStation)
                        val sanitizedUrl = sanitizeStationUrl(url)
                        addStationToFirebase(favoriteStation.copy(id = sanitizedUrl))
                        Log.d("StationAdapter", "Added station to favorites: $name")
                    }
                }
            } catch (e: Exception) {
                Log.e("StationAdapter", "Error while updating favorites for station: $name", e)
            }
        }
    }

    private fun sanitizeStationUrl(url: String): String {
        return url.replace("https://", "").replace("http://", "").replace("/", "_")
    }


    inner class StationViewHolder(val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, state: StationState) {
            binding.stationName.text = name
            binding.playPauseButtonItem.setImageResource(
                when {
                    state.isLoading && !state.isPlaying -> R.drawable.ic_loading
                    !state.isLoading && !state.isPlaying -> R.drawable.ic_play
                    else -> R.drawable.ic_music_note
                }
            )
            binding.playPauseButtonItem.isEnabled = !state.isLoading

            binding.likeButton.setImageResource(
                if (likedStations.contains(name)) R.drawable.ic_heart_dark else R.drawable.ic_heart
            )

            binding.playPauseButtonItem.setOnClickListener {
                if (!state.isLoading) {
                    resetCurrentPlayingStation()
                    stationStates[stations[adapterPosition].first]?.isLoading = true
                    notifyItemChanged(adapterPosition)

                    context.startService(
                        Intent(context, AudioService::class.java).apply {
                            putExtra("STATION_NAME", stations[adapterPosition].first)
                            action = if (state.isPlaying) "PAUSE" else "PLAY"
                        }
                    )

                    val sharedPreferences =
                        context.getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("isPlaying", !state.isPlaying)
                        putBoolean("isLoading", state.isLoading)
                        putString("currentStation", stations[adapterPosition].first)
                        apply()
                    }
                }
            }
        }
    }

    class StationDiffCallback : DiffUtil.ItemCallback<Pair<String, String>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem == newItem
        }
    }
}

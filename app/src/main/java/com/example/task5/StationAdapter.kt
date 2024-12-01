package com.example.task5

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.databinding.ItemStationBinding

class StationAdapter(
    private val stations: List<String>,
    private val context: Context
) : ListAdapter<String, StationAdapter.StationViewHolder>(StationDiffCallback()) {

    private var stationStates = mutableMapOf<String, StationState>().apply {
        stations.forEach { this[it] = StationState() }
    }

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

    // Обновление состояния станции
    fun updateStationState(station: String, isPlaying: Boolean, isLoading: Boolean) {
        stationStates[station]?.apply {
            this.isPlaying = isPlaying
            this.isLoading = isLoading
        }
        notifyItemChanged(stations.indexOf(station))
    }

    // Сброс всех станций
    fun resetAllStations() {
        stationStates.forEach { it.value.isPlaying = false; it.value.isLoading = false }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        val state = stationStates[station] ?: StationState()
        holder.bind(station, state)
    }

    inner class StationViewHolder(private val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: String, state: StationState) {
            binding.stationName.text = station
            binding.playPauseButtonItem.setImageResource(
                when {
                    state.isLoading -> R.drawable.ic_loading
                    state.isPlaying -> R.drawable.ic_pause
                    else -> R.drawable.ic_play
                }
            )
            binding.playPauseButtonItem.isEnabled = !state.isLoading
            binding.playPauseButtonItem.setOnClickListener {
                if (!state.isLoading) {
                    resetAllStations() // Сбрасываем другие станции
                    stationStates[station]?.isLoading = true
                    notifyItemChanged(adapterPosition)
                    context.startService(
                        Intent(context, AudioService::class.java).apply {
                            putExtra("STATION_NAME", station)
                            action = if (state.isPlaying) "PAUSE" else "PLAY"
                        }
                    )
                }
            }
        }
    }

    class StationDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

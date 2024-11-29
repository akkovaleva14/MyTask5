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

    private var isPlaying = mutableMapOf<String, Boolean>().apply {
        stations.forEach { this[it] = false }
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

    // Обновляем состояние воспроизведения для определенной радиостанции
    fun updatePlaybackState(isPlaying: Boolean, station: String?) {
        station?.let {
            this.isPlaying[it] = isPlaying
            val position = stations.indexOf(it)
            if (position != -1) {
                notifyItemChanged(position) // Обновляем только измененный элемент
            }
        }
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position], isPlaying[stations[position]] == true)
    }

    override fun getItemCount(): Int = stations.size

    private fun togglePlayback(station: String) {
        context.startService(
            Intent(context, AudioService::class.java).apply {
                putExtra("STATION_NAME", station)
                this.action = if (isPlaying[station] == true) "PAUSE" else "PLAY"
            }
        )
        isPlaying[station] = !(isPlaying[station] ?: false)

        // Обновляем состояние кнопки в MainActivity
        (context as MainActivity).updatePlaybackState(
            isPlaying.values.any { it },
            if (isPlaying.values.any { it }) station else null
        )
    }

    fun isAnyStationPlaying(): Boolean {
        return isPlaying.values.any { it }
    }

    inner class StationViewHolder(private val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: String, isPlaying: Boolean) {
            binding.stationName.text = station
            binding.playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.playPauseButton.setOnClickListener {
                togglePlayback(station)  // Обрабатываем переключение воспроизведения
            }
        }
    }

    // DiffUtil для эффективного обновления элементов списка
    class StationDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

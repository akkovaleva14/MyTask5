package com.example.task5

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StationAdapter(
    private val stations: List<String>,
    private val context: Context
) : RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    private var isPlaying = mutableMapOf<String, Boolean>()

    init {
        stations.forEach { isPlaying[it] = false }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        holder.stationName.text = station
        holder.playPauseButton.setImageResource(
            if (isPlaying[station] == true) R.drawable.ic_pause else R.drawable.ic_play
        )

        holder.playPauseButton.setOnClickListener {
            togglePlayback(station)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = stations.size

    private fun togglePlayback(station: String) {
        val action = if (isPlaying[station] == true) "PAUSE" else "PLAY"
        val intent = Intent(context, AudioService::class.java).apply {
            putExtra("STATION_NAME", station)
            this.action = action
        }
        context.startService(intent)
        isPlaying[station] = !isPlaying[station]!!
    }

    fun updatePlaybackState(isPlaying: Boolean, station: String?) {
        stations.forEach { s ->
            this.isPlaying[s] = (s == station) && isPlaying // Обновляем только текущую радиостанцию
        }
        notifyDataSetChanged() // Обновляем весь адаптер
    }


    fun isAnyStationPlaying(): Boolean {
        return isPlaying.values.any { it }
    }

    class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stationName: TextView = itemView.findViewById(R.id.stationName)
        val playPauseButton: ImageButton = itemView.findViewById(R.id.playPauseButton)
    }
}

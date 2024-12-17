package com.example.task5.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.R
import com.example.task5.data.FavoriteStation

class FavoriteStationsAdapter(
    private val favoriteStations: List<FavoriteStation>,
    private val onPlayPauseClick: (FavoriteStation) -> Unit,
    private val onLikeClick: (FavoriteStation) -> Unit
) : RecyclerView.Adapter<FavoriteStationsAdapter.ViewHolder>() {

    // ViewHolder to hold the views for each item
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stationName: TextView = view.findViewById(R.id.stationName)
        val playPauseButton: ImageButton = view.findViewById(R.id.playPauseButtonItem)
        val likeButton: ImageButton = view.findViewById(R.id.likeButton)
    }

    // Inflates the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return ViewHolder(view)
    }

    // Binds data to the views in each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = favoriteStations[position]
        holder.stationName.text = station.name

        // Handle play/pause button click
        holder.playPauseButton.setOnClickListener {
            onPlayPauseClick(station)
        }

        // Handle like button click
        holder.likeButton.setOnClickListener {
            onLikeClick(station)
        }
    }

    // Returns the number of items in the list
    override fun getItemCount(): Int = favoriteStations.size
}

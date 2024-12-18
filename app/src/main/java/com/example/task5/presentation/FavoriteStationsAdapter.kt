package com.example.task5.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.data.FavoriteStation
import com.example.task5.databinding.ItemStationBinding

class FavoriteStationsAdapter(
    private val favoriteStations: List<FavoriteStation>,
    private val onPlayPauseClick: (FavoriteStation) -> Unit,
    private val onLikeClick: (FavoriteStation) -> Unit
) : RecyclerView.Adapter<FavoriteStationsAdapter.ViewHolder>() {

    // ViewHolder to hold the views for each item using View Binding
    class ViewHolder(private val binding: ItemStationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(station: FavoriteStation, onPlayPauseClick: (FavoriteStation) -> Unit, onLikeClick: (FavoriteStation) -> Unit) {
            binding.stationName.text = station.name

            // Handle play/pause button click
            binding.playPauseButtonItem.setOnClickListener {
                onPlayPauseClick(station)
            }

            // Handle like button click
            binding.likeButton.setOnClickListener {
                onLikeClick(station)
            }
        }
    }

    // Inflates the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Binds data to the views in each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = favoriteStations[position]
        holder.bind(station, onPlayPauseClick, onLikeClick)
    }

    // Returns the number of items in the list
    override fun getItemCount(): Int = favoriteStations.size
}

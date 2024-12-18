package com.example.task5.presentation

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.data.removeStationFromFirebase
import com.example.task5.databinding.ItemStationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteStationsAdapter(
    private var favoriteStations: List<FavoriteStation>,
    private val onPlayPauseClick: (FavoriteStation) -> Unit,
    private val onLikeClick: (FavoriteStation) -> Unit,
    private val viewModel: FavoriteStationsViewModel,
    private val onStationsUpdated: (List<FavoriteStation>) -> Unit // Новый callback
) : RecyclerView.Adapter<FavoriteStationsAdapter.ViewHolder>() {

    private val likedStations = mutableSetOf<String>()

    class ViewHolder(private val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            station: FavoriteStation,
            onPlayPauseClick: (FavoriteStation) -> Unit,
            onLikeClick: (FavoriteStation) -> Unit,
            likedStations: Set<String>
        ) {
            binding.stationName.text = station.name
            binding.likeButton.setImageResource(R.drawable.ic_heart_dark)

            binding.playPauseButtonItem.setOnClickListener {
                onPlayPauseClick(station)
            }

            binding.likeButton.setOnClickListener {
                onLikeClick(station)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = favoriteStations[position]
        holder.bind(station, onPlayPauseClick, ::handleLikeClick, likedStations)
    }

    override fun getItemCount(): Int = favoriteStations.size

    private fun handleLikeClick(station: FavoriteStation) {
        viewModel.deleteStation(station.url) { updatedStations ->
            updateStations(updatedStations)
            // Вызываем callback для обновления состояния пустого списка
            onStationsUpdated(updatedStations)
        }
    }

    fun updateStations(updatedStations: List<FavoriteStation>) {
        favoriteStations = updatedStations
        likedStations.clear()
        updatedStations.forEach { station ->
            likedStations.add(station.url)
        }
        notifyDataSetChanged()
    }
}

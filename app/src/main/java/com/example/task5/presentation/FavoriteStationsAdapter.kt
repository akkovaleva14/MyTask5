package com.example.task5.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.task5.R
import com.example.task5.data.FavoriteStation
import com.example.task5.databinding.ItemStationBinding

class FavoriteStationsAdapter(
    private var favoriteStations: List<FavoriteStation>, // Измените на var, чтобы можно было обновлять
    private val onPlayPauseClick: (FavoriteStation) -> Unit,
    private val onLikeClick: (FavoriteStation) -> Unit
) : RecyclerView.Adapter<FavoriteStationsAdapter.ViewHolder>() {

    // Добавляем свойство likedStations для отслеживания "нравится" состояния
    val likedStations = mutableSetOf<String>()

    // ViewHolder to hold the views for each item using View Binding
    class ViewHolder(private val binding: ItemStationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(station: FavoriteStation, onPlayPauseClick: (FavoriteStation) -> Unit, onLikeClick: (FavoriteStation) -> Unit, likedStations: Set<String>) {
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

    // Inflates the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Binds data to the views in each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = favoriteStations[position]
        holder.bind(station, onPlayPauseClick, onLikeClick, likedStations) // Передаем likedStations
    }

    // Returns the number of items in the list
    override fun getItemCount(): Int = favoriteStations.size

    // Метод для обновления списка станций
    fun updateStations(updatedStations: List<FavoriteStation>) {
        favoriteStations = updatedStations
        likedStations.clear() // Очистите likedStations, если необходимо
        updatedStations.forEach { station ->
            likedStations.add(station.url) // Обновите likedStations
        }
        notifyDataSetChanged() // Уведомите адаптер об изменениях
    }

    // Метод для обновления состояния "нравится"
    fun updateLikedStations(updatedStations: List<FavoriteStation>) {
        likedStations.clear()
        updatedStations.forEach { station ->
            likedStations.add(station.url) // Добавьте URL в likedStations
        }
        notifyDataSetChanged() // Уведомите адаптер об изменениях
    }

}

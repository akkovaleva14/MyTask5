package com.example.task5.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteStationsViewModel(private val database: AppDatabase) : ViewModel() {
    private val favoriteStations = mutableListOf<FavoriteStation>()

    fun loadFavoriteStations(onStationsLoaded: (List<FavoriteStation>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            favoriteStations.clear()
            favoriteStations.addAll(database.favoriteStationDao().getAllFavoriteStations())
            withContext(Dispatchers.Main) {
                onStationsLoaded(favoriteStations)
            }
        }
    }

    fun deleteStation(stationUrl: String, onStationsUpdated: (List<FavoriteStation>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.favoriteStationDao().delete(stationUrl)
            val updatedStations = database.favoriteStationDao().getAllFavoriteStations()
            withContext(Dispatchers.Main) {
                onStationsUpdated(updatedStations)
            }
        }
    }
}

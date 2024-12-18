package com.example.task5.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.data.removeStationFromFirebase
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
            // Удаление из базы данных
            database.favoriteStationDao().delete(stationUrl)

            // Удаление из Firebase
            val sanitizedUrl = sanitizeStationUrl(stationUrl)
            removeStationFromFirebase(sanitizedUrl)

            // Обновление списка станций
            favoriteStations.clear()
            favoriteStations.addAll(database.favoriteStationDao().getAllFavoriteStations())

            withContext(Dispatchers.Main) {
                onStationsUpdated(favoriteStations)
            }
        }
    }

    private fun sanitizeStationUrl(url: String): String {
        return url.replace("https://", "").replace("http://", "").replace("/", "_")
    }
}

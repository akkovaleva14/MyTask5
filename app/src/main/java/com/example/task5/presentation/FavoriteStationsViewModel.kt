package com.example.task5.presentation

import android.util.Log
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
            Log.d("FavoriteStationsViewModel", "Starting deletion process for station URL: $stationUrl")

            // Удаление из базы данных
            Log.d("FavoriteStationsViewModel", "Deleting station from local database...")
            database.favoriteStationDao().delete(stationUrl)
            Log.d("FavoriteStationsViewModel", "Station deleted from local database.")

            // Удаление из Firebase
            val sanitizedUrl = sanitizeStationUrl(stationUrl)
            Log.d("FavoriteStationsViewModel", "Deleting station from Firebase with sanitized URL: $sanitizedUrl")
            removeStationFromFirebase(sanitizedUrl)
            Log.d("FavoriteStationsViewModel", "Station deleted from Firebase.")

            // Обновление списка станций
            favoriteStations.clear()
            favoriteStations.addAll(database.favoriteStationDao().getAllFavoriteStations())
            Log.d("FavoriteStationsViewModel", "Favorite stations list updated. New size: ${favoriteStations.size}")

            withContext(Dispatchers.Main) {
                Log.d("FavoriteStationsViewModel", "Notifying UI about updated stations list.")
                onStationsUpdated(favoriteStations)
            }
        }
    }


    private fun sanitizeStationUrl(url: String): String {
        return url.replace("https://", "").replace("http://", "").replace("/", "_")
    }
}

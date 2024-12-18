package com.example.task5.presentation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task5.data.AppDatabase
import com.example.task5.data.FavoriteStation
import com.example.task5.data.getFavoriteStationsFromFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StationsListViewModel(private val context: Context, private val database: AppDatabase) : ViewModel() {

    // LiveData for playback state
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _currentStation = MutableLiveData<String?>()
    val currentStation: LiveData<String?> get() = _currentStation

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AudioPrefs", Context.MODE_PRIVATE)

    // Map of station URLs to names
    private val stations = mapOf(
        "https://radio.plaza.one/mp3" to "Plaza Radio",
        "https://hermitage.hostingradio.ru/hermitage128.mp3" to "Hermitage Radio",
        "https://radiorecord.hostingradio.ru/sd9096.aacp" to "Record Radio 9096",
        "https://radiorecord.hostingradio.ru/christmas96.aacp" to "Christmas Radio 96",
        "https://radiorecord.hostingradio.ru/beach96.aacp" to "Beach Radio 96",
        "https://radiorecord.hostingradio.ru/hypno96.aacp" to "Hypno Radio 96",
        "https://radiorecord.hostingradio.ru/198096.aacp" to "Record Radio 198096"
    )

    private val _favoriteStations = MutableLiveData<List<FavoriteStation>>()
    val favoriteStations: LiveData<List<FavoriteStation>> get() = _favoriteStations

    init {
        _isPlaying.value = sharedPreferences.getBoolean("isPlaying", false)
        _isLoading.value = sharedPreferences.getBoolean("isLoading", false)
        _currentStation.value = sharedPreferences.getString("currentStation", null)
    }

    fun savePlaybackState(isPlaying: Boolean, isLoading: Boolean, station: String?) {
        _isPlaying.value = isPlaying
        _isLoading.value = isLoading
        _currentStation.value = station

        sharedPreferences.edit().apply {
            putBoolean("isPlaying", isPlaying)
            putBoolean("isLoading", isLoading)
            putString("currentStation", station)
            apply()
        }
    }

    fun loadFavoriteStations() {
        viewModelScope.launch(Dispatchers.IO) {
            val favoriteStations = database.favoriteStationDao().getAllFavoriteStations()
            _favoriteStations.postValue(favoriteStations)
        }
    }

    fun loadFavoriteStationsFromFirebase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Загружаем избранные станции из Firebase
                val favoriteStations = getFavoriteStationsFromFirebase()
                Log.d("StationsListViewModel", "Favorite stations from Firebase: $favoriteStations")

                // Сохраняем станции в локальную базу данных
                favoriteStations.forEach { station ->
                    database.favoriteStationDao().insert(station)
                }

                // Обновляем LiveData
                _favoriteStations.postValue(favoriteStations)
            } catch (e: Exception) {
                Log.e("StationsListViewModel", "Error loading favorite stations from Firebase", e)
            }
        }
    }


    // Function to get the list of stations
    fun getStations(): Map<String, String> {
        return stations
    }
}
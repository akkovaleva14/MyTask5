package com.example.task5.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.task5.data.AppDatabase

class FavoriteStationsViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteStationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteStationsViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

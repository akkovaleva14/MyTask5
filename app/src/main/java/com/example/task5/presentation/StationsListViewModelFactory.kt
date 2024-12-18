package com.example.task5.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.task5.data.AppDatabase

class StationsListViewModelFactory(
    private val context: Context,
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StationsListViewModel(context, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

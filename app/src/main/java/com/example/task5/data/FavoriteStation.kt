package com.example.task5.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stations")
data class FavoriteStation(
    @PrimaryKey val id: String,
    val name: String,
    val url: String
)

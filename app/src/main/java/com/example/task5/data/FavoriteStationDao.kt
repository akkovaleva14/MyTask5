package com.example.task5.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteStationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: FavoriteStation)

    @Query("DELETE FROM favorite_stations WHERE id = :stationId")
    suspend fun delete(stationId: String)

    @Query("SELECT * FROM favorite_stations")
    suspend fun getAllFavoriteStations(): List<FavoriteStation>
}
package com.example.task5.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val firestore = FirebaseFirestore.getInstance()
private val auth = FirebaseAuth.getInstance()

suspend fun addStationToFirebase(station: FavoriteStation) {
    val userId = auth.currentUser?.uid ?: return
    Log.d("Firebase", "Adding station: $station for user: $userId")
    firestore.collection("users").document(userId)
        .collection("favoriteStations").document(station.id)
        .set(station)
        .addOnSuccessListener {
            Log.d("Firebase", "Station added successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error adding station", e)
        }
}

fun removeStationFromFirebase(stationUrl: String) {
    Log.d("Firebase", "Attempting to remove station from Firebase with URL: $stationUrl")
    // Пример реализации удаления
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .document(userId)
        .collection("favorite_stations")
        .whereEqualTo("url", stationUrl)
        .get()
        .addOnSuccessListener { snapshot ->
            for (document in snapshot.documents) {
                document.reference.delete()
                Log.d("Firebase", "Station successfully removed from Firebase: $stationUrl")
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error removing station from Firebase: $stationUrl", e)
        }
}


suspend fun getFavoriteStationsFromFirebase(): List<FavoriteStation> {
    val userId = auth.currentUser?.uid ?: return emptyList()
    val favoriteStations = mutableListOf<FavoriteStation>()

    try {
        Log.d("Firebase", "Fetching favorite stations for user: $userId")
        val snapshot = firestore.collection("users").document(userId)
            .collection("favoriteStations")
            .get()
            .await()

        Log.d("Firebase", "Snapshot size: ${snapshot.size()}")

        for (document in snapshot.documents) {
            Log.d("Firebase", "Document data: ${document.data}")
            val station = document.toObject(FavoriteStation::class.java)
            if (station != null) {
                favoriteStations.add(station)
                Log.d("Firebase", "Station added: $station")
            } else {
                Log.e("Firebase", "Station is null for document: ${document.id}")
            }
        }

        Log.d("Firebase", "Fetched favorite stations: $favoriteStations")
    } catch (e: Exception) {
        Log.e("Firebase", "Error fetching favorite stations", e)
    }

    return favoriteStations
}

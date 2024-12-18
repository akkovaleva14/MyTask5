package com.example.task5.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

suspend fun removeStationFromFirebase(stationId: String) {
    val userId = auth.currentUser?.uid ?: return
    Log.d("Firebase", "Removing station: $stationId for user: $userId")
    firestore.collection("users").document(userId)
        .collection("favoriteStations").document(stationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firebase", "Station removed successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error removing station", e)
        }
}
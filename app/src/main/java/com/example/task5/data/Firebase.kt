package com.example.task5.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val firestore = FirebaseFirestore.getInstance()
private val auth = FirebaseAuth.getInstance()

suspend fun addStationToFirebase(station: FavoriteStation) {
    val userId = auth.currentUser?.uid ?: return
    firestore.collection("users").document(userId)
        .collection("favoriteStations").document(station.id)
        .set(station)
}

suspend fun removeStationFromFirebase(stationId: String) {
    val userId = auth.currentUser?.uid ?: return
    firestore.collection("users").document(userId)
        .collection("favoriteStations").document(stationId)
        .delete()
}

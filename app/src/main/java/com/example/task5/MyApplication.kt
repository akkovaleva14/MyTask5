package com.example.task5

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    var mainActivity: MainActivity? = null
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

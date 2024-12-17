package com.example.task5.presentation

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.task5.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.authenticationFragment)
            sharedPreferences.edit().putBoolean("isFirstRun", false).apply()
        } else {
            findNavController(R.id.nav_host_fragment).navigate(R.id.stationsListFragment)
        }
    }
}

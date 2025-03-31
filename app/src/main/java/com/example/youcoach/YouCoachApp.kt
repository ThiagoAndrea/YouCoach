package com.example.youcoach

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class YouCoachApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 🔒 Forza il tema chiaro per tutta l'app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}

package com.TOTOMOFYP.VTOAPP

import android.app.Application
import com.google.firebase.FirebaseApp
import com.banuba.sdk.manager.BanubaSdkManager

/**
 * Custom Application class for initializing Firebase and other app-wide configurations.
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Banuba SDK
        BanubaSdkManager.initialize(this, BuildConfig.BANUBA_TOKEN)
    }
} 
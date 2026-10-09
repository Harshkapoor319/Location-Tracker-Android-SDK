package com.it.bglocation

import android.app.Application
import android.util.Log
import com.it.bglocation.manager.LocationConfig
import com.it.bglocation.manager.LocationManager

class BGLocationApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - Initializing LocationManager")

        // Configure using LocationConfig
        val config = LocationConfig.Builder()
            .setInterval(10000L)            // 10-second updates
            .setFastestInterval(5000L)       // 5-second fastest interval
            .setMinDistanceMeters(0f)        // updates regardless of movement distance
            .setNotificationTitle("Location Tracker Active")
            .setNotificationContent("Continuous real-time tracking in background & killed state")
            .setEnableWatchdog(true)         // WorkManager watchdog enabled
            .setWatchdogIntervalMinutes(15L) // 15-min periodic watchdog
            .setEnableBootRestart(true)      // restart after reboot
            .build()

        // Initialize LocationManager!
        LocationManager.init(this, config)
    }

    companion object {
        private const val TAG = "BGLocationApp"
    }
}

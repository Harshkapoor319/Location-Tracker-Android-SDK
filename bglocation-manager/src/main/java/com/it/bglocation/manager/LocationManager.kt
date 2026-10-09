package com.it.bglocation.manager

import android.content.Context
import android.content.Intent
import com.it.bglocation.sdk.BGLocationTracker
import com.it.bglocation.sdk.callback.LocationListener
import com.it.bglocation.sdk.model.LocationModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Public LocationManager class used by host applications to control background tracking.
 *
 * This class abstracts the closed-source core engine and exposes a clean, intuitive API.
 */
object LocationManager {

    /**
     * Initializes the background tracking engine.
     * Call this in your Application.onCreate().
     */
    fun init(context: Context, config: LocationConfig = LocationConfig()) {
        BGLocationTracker.initialize(context, config.toCoreConfig())
    }

    /**
     * Starts continuous background location tracking.
     * Launches the persistent Foreground Service and schedules the WorkManager watchdog.
     */
    fun startTracking(context: Context) {
        BGLocationTracker.startTracking(context)
    }

    /**
     * Stops continuous location tracking.
     * Stops the Foreground Service, removes ongoing notification, and cancels the watchdog.
     */
    fun stopTracking(context: Context) {
        BGLocationTracker.stopTracking(context)
    }

    /**
     * Checks if tracking is currently active.
     */
    fun isTracking(context: Context): Boolean {
        return BGLocationTracker.isTracking(context)
    }

    /**
     * Checks if the Foreground Service is actively alive right now.
     */
    fun isServiceRunning(): Boolean {
        return BGLocationTracker.isServiceRunning()
    }

    /**
     * Exposes a reactive Kotlin StateFlow of the latest location updates.
     */
    fun getLocationFlow(context: Context): StateFlow<LocationModel?> {
        return BGLocationTracker.getLocationFlow(context)
    }

    /**
     * Exposes a reactive Kotlin StateFlow of tracking active/stopped status.
     */
    fun getTrackingStateFlow(context: Context): StateFlow<Boolean> {
        return BGLocationTracker.getTrackingStateFlow(context)
    }

    /**
     * Adds a callback listener for location updates (Java & non-coroutines friendly).
     */
    fun addListener(listener: LocationListener) {
        BGLocationTracker.addLocationListener(listener)
    }

    /**
     * Removes a callback listener.
     */
    fun removeListener(listener: LocationListener) {
        BGLocationTracker.removeLocationListener(listener)
    }

    /**
     * Retrieves the last known recorded location from persistent storage.
     */
    fun getLastLocation(context: Context): LocationModel? {
        return BGLocationTracker.getLastLocation(context)
    }

    /**
     * Retrieves recent location logs recorded across background/killed sessions.
     */
    fun getLocationHistory(context: Context): List<LocationModel> {
        return BGLocationTracker.getLocationHistory(context)
    }

    /**
     * Clears all recorded location history.
     */
    fun clearHistory(context: Context) {
        BGLocationTracker.clearLocationHistory(context)
    }

    /**
     * Returns total count of locations recorded.
     */
    fun getLocationCount(context: Context): Int {
        return BGLocationTracker.getLocationCount(context)
    }

    // --- Permission & Battery Utilities ---

    fun hasRequiredPermissions(context: Context): Boolean {
        return BGLocationTracker.hasRequiredPermissions(context)
    }

    fun hasBackgroundPermission(context: Context): Boolean {
        return BGLocationTracker.hasBackgroundPermission(context)
    }

    fun getRequiredForegroundPermissions(): Array<String> {
        return BGLocationTracker.getRequiredForegroundPermissions()
    }

    fun isGpsEnabled(context: Context): Boolean {
        return BGLocationTracker.isGpsEnabled(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return BGLocationTracker.isIgnoringBatteryOptimizations(context)
    }

    fun createIgnoreBatteryOptimizationIntent(context: Context): Intent {
        return BGLocationTracker.createIgnoreBatteryOptimizationIntent(context)
    }
}

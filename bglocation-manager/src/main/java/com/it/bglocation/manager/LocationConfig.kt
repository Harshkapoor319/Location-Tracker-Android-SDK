package com.it.bglocation.manager

import com.it.bglocation.sdk.BGLocationConfig
import java.io.Serializable

/**
 * Public configuration class for the BGLocation Manager module.
 */
class LocationConfig(
    val intervalMs: Long = 10000L,
    val fastestIntervalMs: Long = 5000L,
    val minDistanceMeters: Float = 0f,
    val notificationTitle: String = "Location Tracking Active",
    val notificationContent: String = "Tracking continuous location in background...",
    val notificationIcon: Int = android.R.drawable.ic_menu_mylocation,
    val notificationChannelId: String = "continuous_location_channel",
    val notificationChannelName: String = "Continuous Location Service",
    val enableWatchdog: Boolean = true,
    val watchdogIntervalMinutes: Long = 15L,
    val enableBootRestart: Boolean = true,
    val maxHistoryCount: Int = 100
) : Serializable {

    internal fun toCoreConfig(): BGLocationConfig {
        return BGLocationConfig(
            intervalMs = intervalMs,
            fastestIntervalMs = fastestIntervalMs,
            minDistanceMeters = minDistanceMeters,
            notificationTitle = notificationTitle,
            notificationContent = notificationContent,
            notificationIcon = notificationIcon,
            notificationChannelId = notificationChannelId,
            notificationChannelName = notificationChannelName,
            enableWatchdog = enableWatchdog,
            watchdogIntervalMinutes = watchdogIntervalMinutes,
            enableBootRestart = enableBootRestart,
            maxHistoryCount = maxHistoryCount
        )
    }

    class Builder {
        private var intervalMs: Long = 10000L
        private var fastestIntervalMs: Long = 5000L
        private var minDistanceMeters: Float = 0f
        private var notificationTitle: String = "Location Tracking Active"
        private var notificationContent: String = "Tracking continuous location in background..."
        private var notificationIcon: Int = android.R.drawable.ic_menu_mylocation
        private var notificationChannelId: String = "continuous_location_channel"
        private var notificationChannelName: String = "Continuous Location Service"
        private var enableWatchdog: Boolean = true
        private var watchdogIntervalMinutes: Long = 15L
        private var enableBootRestart: Boolean = true
        private var maxHistoryCount: Int = 100

        fun setInterval(intervalMs: Long) = apply { this.intervalMs = intervalMs }
        fun setFastestInterval(fastestIntervalMs: Long) = apply { this.fastestIntervalMs = fastestIntervalMs }
        fun setMinDistanceMeters(distance: Float) = apply { this.minDistanceMeters = distance }
        fun setNotificationTitle(title: String) = apply { this.notificationTitle = title }
        fun setNotificationContent(content: String) = apply { this.notificationContent = content }
        fun setNotificationIcon(iconResId: Int) = apply { this.notificationIcon = iconResId }
        fun setNotificationChannel(id: String, name: String) = apply {
            this.notificationChannelId = id
            this.notificationChannelName = name
        }
        fun setEnableWatchdog(enable: Boolean) = apply { this.enableWatchdog = enable }
        fun setWatchdogIntervalMinutes(minutes: Long) = apply { this.watchdogIntervalMinutes = minutes }
        fun setEnableBootRestart(enable: Boolean) = apply { this.enableBootRestart = enable }
        fun setMaxHistoryCount(count: Int) = apply { this.maxHistoryCount = count }

        fun build(): LocationConfig = LocationConfig(
            intervalMs = intervalMs,
            fastestIntervalMs = fastestIntervalMs,
            minDistanceMeters = minDistanceMeters,
            notificationTitle = notificationTitle,
            notificationContent = notificationContent,
            notificationIcon = notificationIcon,
            notificationChannelId = notificationChannelId,
            notificationChannelName = notificationChannelName,
            enableWatchdog = enableWatchdog,
            watchdogIntervalMinutes = watchdogIntervalMinutes,
            enableBootRestart = enableBootRestart,
            maxHistoryCount = maxHistoryCount
        )
    }
}

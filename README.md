# 🛰️ BGLocation SDK — Continuous Background Location Tracking for Android

[![Min SDK](https://img.shields.io/badge/minSdk-24%20(Android%207.0)-brightgreen.svg)](#compatibility)
[![Target SDK](https://img.shields.io/badge/compileSdk-36%20(Android%2016)-blue.svg)](#compatibility)
[![Language](https://img.shields.io/badge/Language-Kotlin%20%2F%20Java-orange.svg)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](#)

A plug-and-play, production-ready Android SDK that delivers **guaranteed continuous, real-time location updates** across **all** application states:

- 🟢 **Foreground**: App is open and in use.
- 🟡 **Background**: App is minimized or the screen is locked.
- 🔴 **Killed / Terminated**: App was swiped away from the Recent Apps list.
- 🔵 **Rebooted**: Device was restarted or updated.

---

## 📑 Table of Contents
1. [Why This SDK?](#-why-this-sdk)
2. [Installation](#-installation)
3. [Step-by-Step Integration Guide (Which Methods to Call)](#-step-by-step-integration-guide)
   - [Step 1: Initialize in Application](#step-1-initialize-in-application-class)
   - [Step 2: Check & Request Permissions](#step-2-check--request-permissions)
   - [Step 3: Start Tracking](#step-3-start-tracking)
   - [Step 4: Receive Live Location Updates](#step-4-receive-live-location-updates)
   - [Step 5: Stop Tracking](#step-5-stop-tracking)
   - [Step 6: Access Stored History & Cache](#step-6-access-stored-location-history)
   - [Step 7: Handle Battery Optimizations](#step-7-handle-oem-battery-optimizations)
4. [Complete Method Reference (`BGLocationTracker`)](#-complete-method-reference)
5. [Configuration Reference (`BGLocationConfig`)](#-configuration-reference)
6. [Android Version Compatibility (API 24 to API 36)](#-android-version-compatibility-api-24---36)
7. [Publishing as an AAR / JitPack](#-publishing-as-an-aar--maven-library)
8. [Troubleshooting & OEM Device Guidelines](#-troubleshooting--oem-guidelines)

---

## 💡 Why This SDK?

Android 8.0 through Android 15/16 impose strict restrictions on background location:
- Without a Foreground Service, background location is throttled to **2–4 updates per hour**.
- Android WorkManager has a minimum periodic interval of **15 minutes** and cannot provide continuous real-time streaming.
- Android 12+ throws `ForegroundServiceStartNotAllowedException` if a background service is launched from the background.

### The Hybrid Solution
This SDK combines an **Android Foreground Service** (for continuous 5–10 second streaming) with a **WorkManager Watchdog** (for crash resilience across kills). Because WorkManager runs as a system `JobService`, it is **exempt** from Android 12+ background launch limits, allowing it to resurrect the foreground service if the OS terminates it under memory pressure.

---

## 📦 Installation

### Option 1: As a Module in Your Android Studio Project
1. Copy the `bglocation-sdk` folder into your Android project root.
2. In your `settings.gradle.kts`:
   ```kotlin
   include(":bglocation-sdk")
   ```
3. In your app's `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":bglocation-sdk"))
   }
   ```

### Option 2: As an AAR File
1. Build the library: `./gradlew :bglocation-sdk:assembleRelease`
2. Copy `bglocation-sdk/build/outputs/aar/bglocation-sdk-release.aar` into your app's `libs/` folder.
3. In your app's `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(files("libs/bglocation-sdk-release.aar"))
   }
   ```

> 🪄 **Automatic Manifest Merging**: You **DO NOT** need to add `<service>`, `<receiver>`, or `<uses-permission>` tags into your host app's `AndroidManifest.xml`. The SDK manifest is automatically merged into your APK during compilation!

---

## 🚀 Step-by-Step Integration Guide

Here is the exact sequence of methods to call in your app.

### Step 1: Initialize in `Application` Class
Call `BGLocationTracker.initialize()` inside your `Application.onCreate()`. This registers your notification channel and sets default tracking parameters.

```kotlin
import android.app.Application
import com.it.bglocation.sdk.BGLocationConfig
import com.it.bglocation.sdk.BGLocationTracker

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Customize settings using the Builder (all parameters are optional)
        val config = BGLocationConfig.Builder()
            .setInterval(10000L)              // Location request interval: 10 seconds
            .setFastestInterval(5000L)        // Fastest interval: 5 seconds
            .setMinDistanceMeters(0f)         // Receive all updates regardless of distance
            .setNotificationTitle("Delivery Tracker Active")
            .setNotificationContent("Continuous location tracking in progress...")
            .setEnableWatchdog(true)          // WorkManager watchdog enabled
            .setWatchdogIntervalMinutes(15L)  // Check every 15 min if service died
            .setEnableBootRestart(true)       // Auto-restart after phone reboot
            .setMaxHistoryCount(100)          // Keep latest 100 coordinates
            .build()

        // 👈 METHOD TO CALL: Initialize SDK
        BGLocationTracker.initialize(this, config)
    }
}
```

---

### Step 2: Check & Request Permissions
Android 11+ strictly requires a **two-step permission flow**: request Foreground Location first, then Background Location.

```kotlin
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.it.bglocation.sdk.BGLocationTracker

class MainActivity : AppCompatActivity() {

    // 1. Foreground permissions launcher (Fine + Coarse + Notifications)
    private val foregroundPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (BGLocationTracker.hasRequiredPermissions(this)) {
            // Once foreground is granted, prompt for background location
            checkBackgroundPermission()
        }
    }

    // 2. Background location launcher (Android 10+ / API 29+)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Start tracking regardless — Foreground Service works in both cases
        startTracking()
    }

    private fun checkPermissionsAndStart() {
        // 👈 METHOD TO CALL: Check GPS enabled
        if (!BGLocationTracker.isGpsEnabled(this)) {
            // Prompt user to enable device GPS
            return
        }

        // 👈 METHOD TO CALL: Check required foreground permissions
        if (!BGLocationTracker.hasRequiredPermissions(this)) {
            // 👈 METHOD TO CALL: Get permissions array
            foregroundPermissionsLauncher.launch(BGLocationTracker.getRequiredForegroundPermissions())
        } else {
            checkBackgroundPermission()
        }
    }

    private fun checkBackgroundPermission() {
        // 👈 METHOD TO CALL: Check background permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            !BGLocationTracker.hasBackgroundPermission(this)) {
            // Show educational dialog explaining why background location is needed, then:
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        // 👈 METHOD TO CALL: Start tracking
        BGLocationTracker.startTracking(this)
    }
}
```

---

### Step 3: Start Tracking
Call `BGLocationTracker.startTracking(context)`.

```kotlin
// Starts Foreground Service + ongoing persistent notification + WorkManager watchdog
BGLocationTracker.startTracking(context)
```

---

### Step 4: Receive Live Location Updates

#### Method A: Using Kotlin Coroutines `StateFlow` (Recommended)
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        
        // 👈 METHOD TO CALL: Observe real-time coordinates
        launch {
            BGLocationTracker.getLocationFlow(context).collect { location ->
                location?.let {
                    val lat = it.latitude
                    val lng = it.longitude
                    val accuracy = it.accuracy
                    val speed = it.speed
                    val time = it.getFormattedTime()
                    println("Received: $lat, $lng (Acc: ${accuracy}m at $time)")
                }
            }
        }

        // 👈 METHOD TO CALL: Observe tracking toggle state (true/false)
        launch {
            BGLocationTracker.getTrackingStateFlow(context).collect { isTracking ->
                btnStart.isEnabled = !isTracking
                btnStop.isEnabled = isTracking
            }
        }
    }
}
```

#### Method B: Using Listener Callback (Java or Non-Coroutines)
```kotlin
import com.it.bglocation.sdk.callback.LocationListener

// Define listener
val listener = LocationListener { location ->
    println("New location: ${location.latitude}, ${location.longitude}")
}

// 👈 METHOD TO CALL: Register listener
BGLocationTracker.addLocationListener(listener)

// 👈 METHOD TO CALL: Unregister when done (e.g., onDestroy)
BGLocationTracker.removeLocationListener(listener)
```

---

### Step 5: Stop Tracking
Call `BGLocationTracker.stopTracking(context)` whenever tracking is no longer needed.

```kotlin
// 👈 METHOD TO CALL: Stops Foreground Service, removes notification & cancels watchdog
BGLocationTracker.stopTracking(context)
```

---

### Step 6: Access Stored Location History
The SDK automatically persists recorded coordinates so they survive app restarts and kills.

```kotlin
// 👈 METHOD TO CALL: Get the most recent location recorded
val lastLocation = BGLocationTracker.getLastLocation(context)

// 👈 METHOD TO CALL: Get the list of last 100 recorded locations
val historyList = BGLocationTracker.getLocationHistory(context)

// 👈 METHOD TO CALL: Get total count of recorded coordinates
val totalCount = BGLocationTracker.getLocationCount(context)

// 👈 METHOD TO CALL: Clear all saved history
BGLocationTracker.clearLocationHistory(context)
```

---

### Step 7: Handle OEM Battery Optimizations
Aggressive manufacturers (Samsung, Xiaomi, Oppo, Vivo, OnePlus) kill background services unless battery optimization is disabled.

```kotlin
// 👈 METHOD TO CALL: Check if battery optimization is already disabled
if (!BGLocationTracker.isIgnoringBatteryOptimizations(context)) {
    // 👈 METHOD TO CALL: Launch system dialog to exempt app from battery saver
    startActivity(BGLocationTracker.createIgnoreBatteryOptimizationIntent(context))
}
```

---

## 📖 Complete Method Reference

All public methods are accessible statically via `BGLocationTracker`:

| Method | Return Type | Description |
|---|---|---|
| `initialize(context, config)` | `Unit` | Sets configuration, initializes notification channel. Call in `Application.onCreate()`. |
| `startTracking(context)` | `Unit` | Starts continuous Foreground Service & arms WorkManager watchdog. |
| `stopTracking(context)` | `Unit` | Stops Foreground Service, removes ongoing notification & cancels watchdog. |
| `isTracking(context)` | `Boolean` | Returns `true` if continuous tracking is enabled. |
| `isServiceRunning()` | `Boolean` | Returns `true` if the Foreground Service is alive right now. |
| `getLocationFlow(context)` | `StateFlow<LocationModel?>` | Reactive Kotlin StateFlow emitting new coordinates in real-time. |
| `getTrackingStateFlow(context)` | `StateFlow<Boolean>` | Reactive Kotlin StateFlow emitting tracking state changes (`true`/`false`). |
| `addLocationListener(listener)` | `Unit` | Adds a callback listener to receive location updates. |
| `removeLocationListener(listener)` | `Unit` | Removes a previously registered callback listener. |
| `getLastLocation(context)` | `LocationModel?` | Returns the last known recorded location from persistent cache. |
| `getLocationHistory(context)` | `List<LocationModel>` | Returns list of recent coordinates recorded while backgrounded/killed. |
| `clearLocationHistory(context)` | `Unit` | Clears all cached coordinates. |
| `getLocationCount(context)` | `Int` | Returns the total number of recorded locations. |
| `hasRequiredPermissions(context)` | `Boolean` | Returns `true` if Fine/Coarse and Notification permissions are granted. |
| `hasBackgroundPermission(context)` | `Boolean` | Returns `true` if `ACCESS_BACKGROUND_LOCATION` is granted (always `true` on < Android 10). |
| `getRequiredForegroundPermissions()`| `Array<String>` | Returns array of permissions to pass to `ActivityResultLauncher`. |
| `isGpsEnabled(context)` | `Boolean` | Returns `true` if device GPS or Network location provider is turned on. |
| `isIgnoringBatteryOptimizations(c)` | `Boolean` | Returns `true` if app is exempt from OS battery restrictions. |
| `createIgnoreBatteryOptimizationIntent(c)` | `Intent` | Returns intent to prompt user for battery exemption. |

---

## ⚙️ Configuration Reference

Use `BGLocationConfig.Builder()` to customize the SDK:

| Builder Method | Default Value | Description |
|---|---|---|
| `.setInterval(ms: Long)` | `10000L` (10s) | Location request interval in milliseconds. |
| `.setFastestInterval(ms: Long)` | `5000L` (5s) | Fastest interval your app can handle location updates. |
| `.setMinDistanceMeters(m: Float)`| `0f` | Minimum movement distance in meters to trigger an update. |
| `.setNotificationTitle(title: String)` | `"Location Tracking Active"` | Title shown on the persistent foreground notification. |
| `.setNotificationContent(content: String)` | `"Tracking continuous location in background..."` | Body text shown on the notification. |
| `.setNotificationIcon(iconResId: Int)` | `ic_menu_mylocation` | Drawable resource ID for the notification icon. |
| `.setNotificationChannel(id, name)` | `"continuous_location_channel"` | Custom notification channel ID and display name. |
| `.setEnableWatchdog(enable: Boolean)` | `true` | Enables WorkManager periodic health-check watchdog. |
| `.setWatchdogIntervalMinutes(min: Long)` | `15L` | Interval for WorkManager watchdog (min: 15 minutes). |
| `.setEnableBootRestart(enable: Boolean)` | `true` | Automatically resumes tracking after phone restarts. |
| `.setMaxHistoryCount(count: Int)` | `100` | Maximum number of location records kept in cache. |

---

## 📱 Android Version Compatibility (API 24 - 36)

The SDK handles all OS-level differences internally:

| Version | API | How the SDK Handles It |
|---|---|---|
| **Android 7.0–7.1** | **24–25** | Standard background service execution and baseline location permissions. |
| **Android 8.0–8.1** | **26–27** | Checks `Build.VERSION_CODES.O`: Creates Notification Channel; promotes service to foreground via `startForegroundService()`. |
| **Android 9.0** | **28** | Declares `android.permission.FOREGROUND_SERVICE` in manifest. |
| **Android 10** | **29** | Declares `foregroundServiceType="location"`; handles `ACCESS_BACKGROUND_LOCATION`. |
| **Android 11** | **30** | Supports Google's 2-step permission mandate (separate foreground and background prompts). |
| **Android 12–12L** | **31–32** | All `PendingIntent`s use `FLAG_IMMUTABLE`. WorkManager watchdog holds OS exemption against background launch limits. |
| **Android 13** | **33** | Dynamically checks and requests `POST_NOTIFICATIONS` runtime permission. |
| **Android 14** | **34** | Declares `FOREGROUND_SERVICE_LOCATION` permission; uses `ServiceCompat.startForeground` with location type. |
| **Android 15–16** | **35–36** | Location services with ongoing notification are exempt from 6-hour timeouts; 16KB memory page compatible. |

---

## 🚢 Publishing as an AAR / Maven Library

The `bglocation-sdk` module includes the Gradle `maven-publish` plugin.

- **Generate Standalone `.aar`**:
  ```bash
  ./gradlew :bglocation-sdk:assembleRelease
  ```
  The compiled `.aar` will be located at:
  `bglocation-sdk/build/outputs/aar/bglocation-sdk-release.aar`

- **Publish to Local Maven Cache (`~/.m2`)**:
  ```bash
  ./gradlew :bglocation-sdk:publishToMavenLocal
  ```

---

## 🛠️ Troubleshooting & OEM Guidelines

### 1. App killed when swiped away on Xiaomi / Samsung / OnePlus?
Chinese and custom Android skins have aggressive battery optimizers.
- **Solution**: Call `BGLocationTracker.createIgnoreBatteryOptimizationIntent(context)` and prompt the user to select **"No restrictions / Don't optimize"**.
- The WorkManager watchdog will automatically relaunch the service during the next periodic window.

### 2. Can the notification be dismissed?
No. Android OS mandates an ongoing, persistent notification for any Foreground Service performing real-time location tracking. Tapping the "Stop Tracking" button on the notification cleanly stops the service.

### 3. Does tracking survive device reboots?
Yes! The SDK includes a `BootReceiver` that listens for `BOOT_COMPLETED`. If tracking was active before shutdown, it automatically relaunches upon reboot.

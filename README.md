# 🛰️ BGLocation — Continuous Background Location Tracking for Android

[![Min SDK](https://img.shields.io/badge/minSdk-24%20(Android%207.0)-brightgreen.svg)](#-android-version-compatibility-api-24---36)
[![Target SDK](https://img.shields.io/badge/compileSdk-36%20(Android%2016)-blue.svg)](#-android-version-compatibility-api-24---36)
[![Language](https://img.shields.io/badge/Language-Kotlin%20%2F%20Java-orange.svg)](#)
[![Architecture](https://img.shields.io/badge/Architecture-Private%20AAR%20%2B%20Public%20Manager-purple.svg)](#-architecture--security-model)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](#)

A plug-and-play, enterprise-ready Android background location tracking library that guarantees **continuous, real-time location updates** across **all** application states:

- 🟢 **Foreground**: App is open and in active use.
- 🟡 **Background**: App is minimized or the screen is turned off.
- 🔴 **Killed / Swiped Away**: App was terminated from Recent Apps.
- 🔵 **Rebooted**: Device was restarted or updated.

---

## 📑 Table of Contents
1. [Architecture & Security Model](#-architecture--security-model)
2. [Installation](#-installation)
3. [Quick Start Integration Guide](#-quick-start-integration-guide)
   - [Step 1: Initialize in Application Class](#step-1-initialize-in-application-class)
   - [Step 2: Check & Request Permissions](#step-2-check--request-permissions)
   - [Step 3: Start Tracking](#step-3-start-tracking)
   - [Step 4: Receive Real-Time Location Updates](#step-4-receive-real-time-location-updates)
   - [Step 5: Stop Tracking](#step-5-stop-tracking)
   - [Step 6: Access Stored History & Cache](#step-6-access-stored-location-history)
   - [Step 7: Handle Battery Optimizations](#step-7-handle-oem-battery-optimizations)
4. [Complete Method Reference (`LocationManager`)](#-complete-method-reference-locationmanager)
5. [Configuration Reference (`LocationConfig`)](#-configuration-reference-locationconfig)
6. [Data Model Reference (`LocationModel`)](#-data-model-reference-locationmodel)
7. [SDK Maintainer Guide (Rebuilding Private Core Engine)](#-sdk-maintainer-guide)
8. [Android Version Compatibility (API 24 to API 36)](#-android-version-compatibility-api-24---36)
9. [OEM Device Troubleshooting](#-oem-device-troubleshooting)

---

## 🛡️ Architecture & Security Model

To protect your intellectual property, this project is split into a **Two-Tier Architecture**:

```
┌────────────────────────────────────────────────────────┐
│                      Client App                        │
│             (Imports :bglocation-manager)              │
└──────────────────────────┬─────────────────────────────┘
                           │ Calls public API
                           ▼
┌────────────────────────────────────────────────────────┐
│              Public Wrapper Module                     │
│               (:bglocation-manager)                    │
│  • Public LocationManager, LocationConfig, models      │
│  • Automatic AndroidManifest merging                   │
│  • Consumes closed-source engine via libs/*.aar        │
└──────────────────────────┬─────────────────────────────┘
                           │ Links compiled binary
                           ▼
┌────────────────────────────────────────────────────────┐
│              Private Core Engine                       │
│                (:bglocation-sdk)                       │
│  • Proprietary tracking engine, watchdog & database    │
│  • Obfuscated release .aar binary in libs/             │
│  • Source code is kept private & confidential          │
└────────────────────────────────────────────────────────┘
```

- **For Client Developers**: You only add and interact with `bglocation-manager`. You never need to manage raw `.aar` files or add service/receiver tags to your manifest.
- **For the Maintainer**: You develop proprietary algorithms inside `bglocation-sdk`, export the compiled/obfuscated `.aar` into `bglocation-manager/libs/`, and distribute `bglocation-manager` without exposing your proprietary source code.

---

## 📦 Installation

### Option 1: Via JitPack (Recommended)

1. Add JitPack repository in your project's `settings.gradle.kts`:
   ```kotlin
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://jitpack.io") }
       }
   }
   ```
   *(Or if using Groovy `settings.gradle`: `maven { url 'https://jitpack.io' }`)*

2. Add the dependency in your application module (`app/build.gradle.kts`):
   ```kotlin
   dependencies {
       implementation("com.github.Harshkapoor319:Location-Tracker-Android-SDK:v1.0.0")
   }
   ```
   *(Or if using Groovy `app/build.gradle`: `implementation 'com.github.Harshkapoor319:Location-Tracker-Android-SDK:v1.0.0'`)*

### Option 2: As a Local Module

1. Include `bglocation-manager` in your `settings.gradle.kts`:
   ```kotlin
   include(":bglocation-manager")
   ```

2. Add the dependency in your application module (`app/build.gradle.kts`):
   ```kotlin
   dependencies {
       implementation(project(":bglocation-manager"))
   }
   ```

> 🪄 **Zero Manifest Boilerplate**: You **DO NOT** need to add `<service>`, `<receiver>`, or `<uses-permission>` tags to your app's `AndroidManifest.xml`. All required permissions, the foreground `LocationService`, and the `BootReceiver` are automatically merged into your final APK at build time.

---

## 🚀 Quick Start Integration Guide

### Step 1: Initialize in `Application` Class
Initialize `LocationManager` inside your `Application.onCreate()`. This registers the notification channel and sets default tracking parameters.

```kotlin
import android.app.Application
import com.it.bglocation.manager.LocationConfig
import com.it.bglocation.manager.LocationManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Customize configuration (all builder parameters are optional)
        val config = LocationConfig.Builder()
            .setInterval(10000L)              // Location request interval: 10 seconds
            .setFastestInterval(5000L)        // Fastest interval: 5 seconds
            .setMinDistanceMeters(0f)         // Update regardless of distance moved
            .setNotificationTitle("Location Tracking Active")
            .setNotificationContent("Continuous real-time tracking in progress...")
            .setEnableWatchdog(true)          // WorkManager watchdog enabled (resurrects service on kill)
            .setWatchdogIntervalMinutes(15L)  // Check every 15 minutes
            .setEnableBootRestart(true)       // Auto-restart tracking after phone reboot
            .setMaxHistoryCount(100)          // Keep latest 100 coordinates in local storage
            .build()

        // 👈 Initialize the manager
        LocationManager.init(this, config)
    }
}
```

---

### Step 2: Check & Request Permissions
Android 11+ strictly requires a **two-step permission flow**: request Foreground Location (+ Notifications on Android 13+) first, followed by Background Location.

`LocationManager` provides built-in helper methods to make this seamless:

```kotlin
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.it.bglocation.manager.LocationManager

class MainActivity : AppCompatActivity() {

    // 1. Foreground Location and Notification permissions launcher
    private val foregroundPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            checkAndRequestBackgroundLocation()
        } else {
            Toast.makeText(this, "Location permission is required for tracking", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Background Location launcher (Android 10+ / API 29+)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Start tracking regardless — Foreground Service operates in both states
        startTracking()
    }

    private fun handleStartTrackingClick() {
        // Step 2a: Check if GPS is enabled
        if (!LocationManager.isGpsEnabled(this)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        // Step 2b: Check required foreground permissions
        if (!LocationManager.hasRequiredPermissions(this)) {
            foregroundPermissionsLauncher.launch(LocationManager.getRequiredForegroundPermissions())
        } else {
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            !LocationManager.hasBackgroundPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle("Background Location Permission")
                .setMessage("To track your location continuously when minimized or killed, please select 'Allow all the time'.")
                .setPositiveButton("Continue") { _, _ ->
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                .setNegativeButton("Skip") { _, _ ->
                    startTracking()
                }
                .show()
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        LocationManager.startTracking(this)
    }
}
```

---

### Step 3: Start Tracking
To start continuous tracking, call:

```kotlin
// Starts persistent Foreground Service, displays ongoing notification & arms watchdog
LocationManager.startTracking(context)
```

---

### Step 4: Receive Real-Time Location Updates

#### Option A: Using Kotlin Coroutines `StateFlow` (Recommended)
`LocationManager.getLocationFlow(context)` emits real-time `LocationModel` objects whenever new coordinates arrive.

```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.it.bglocation.manager.LocationManager
import kotlinx.coroutines.launch

lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // 1. Observe real-time location stream
        launch {
            LocationManager.getLocationFlow(this@MainActivity).collect { location ->
                location?.let {
                    val lat = it.latitude
                    val lng = it.longitude
                    val accuracy = it.accuracy
                    val speed = it.speed
                    val altitude = it.altitude
                    val time = it.getFormattedTime()
                    val source = it.source

                    println("Update: $lat, $lng (Acc: ${accuracy}m at $time via $source)")
                }
            }
        }

        // 2. Observe tracking state (true/false) to toggle UI buttons
        launch {
            LocationManager.getTrackingStateFlow(this@MainActivity).collect { isTracking ->
                btnStart.isEnabled = !isTracking
                btnStop.isEnabled = isTracking
            }
        }
    }
}
```

#### Option B: Using Java / Callback Listener
For Java codebases or non-coroutine architectures:

```kotlin
import com.it.bglocation.manager.LocationListener
import com.it.bglocation.manager.LocationManager

// Define listener
val listener = LocationListener { location ->
    println("New Location: ${location.latitude}, ${location.longitude}")
}

// Register listener (e.g., in onStart or onResume)
LocationManager.addListener(listener)

// Unregister listener (e.g., in onStop or onDestroy)
LocationManager.removeListener(listener)
```

---

### Step 5: Stop Tracking
Call `LocationManager.stopTracking(context)` to shut down the foreground service, remove the persistent notification, and cancel the WorkManager watchdog:

```kotlin
LocationManager.stopTracking(context)
```

---

### Step 6: Access Stored Location History
The SDK automatically persists recorded coordinates locally so you can inspect travel history even across device reboots and app kills:

```kotlin
// Get the most recent location record
val lastLocation = LocationManager.getLastLocation(context)

// Get list of recorded locations (up to maxHistoryCount)
val historyList = LocationManager.getLocationHistory(context)

// Get total count of recorded coordinates
val count = LocationManager.getLocationCount(context)

// Clear all stored coordinates
LocationManager.clearHistory(context)
```

---

### Step 7: Handle OEM Battery Optimizations
Custom OEM operating systems (Samsung, Xiaomi, Oppo, Vivo, OnePlus) may kill background processes unless battery optimization is disabled.

```kotlin
// Check if battery optimization is already disabled
if (!LocationManager.isIgnoringBatteryOptimizations(context)) {
    try {
        // Open the system dialog asking the user to exempt the app
        startActivity(LocationManager.createIgnoreBatteryOptimizationIntent(context))
    } catch (e: Exception) {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
```

---

## 📖 Complete Method Reference (`LocationManager`)

All public methods are accessed statically via `LocationManager`:

| Method | Return Type | Description |
|---|---|---|
| `init(context, config)` | `Unit` | Initializes SDK parameters and creates the notification channel. Call in `Application.onCreate()`. |
| `startTracking(context)` | `Unit` | Starts continuous tracking via Foreground Service and arms WorkManager watchdog. |
| `stopTracking(context)` | `Unit` | Stops tracking, dismisses ongoing notification, and cancels watchdog. |
| `isTracking(context)` | `Boolean` | Returns `true` if continuous tracking is enabled. |
| `isServiceRunning()` | `Boolean` | Returns `true` if the Foreground Service is actively running right now. |
| `getLocationFlow(context)` | `StateFlow<LocationModel?>` | Reactive Kotlin StateFlow emitting live coordinates. |
| `getTrackingStateFlow(context)` | `StateFlow<Boolean>` | Reactive Kotlin StateFlow emitting tracking state changes (`true`/`false`). |
| `addListener(listener)` | `Unit` | Adds a callback listener for location updates. |
| `removeListener(listener)` | `Unit` | Removes a registered callback listener. |
| `getLastLocation(context)` | `LocationModel?` | Returns the most recent cached location record. |
| `getLocationHistory(context)` | `List<LocationModel>` | Returns list of recent location records. |
| `clearHistory(context)` | `Unit` | Clears all stored coordinates from local storage. |
| `getLocationCount(context)` | `Int` | Returns the total count of recorded locations. |
| `hasRequiredPermissions(context)` | `Boolean` | Checks if Fine/Coarse and Notification permissions are granted. |
| `hasBackgroundPermission(context)` | `Boolean` | Checks if `ACCESS_BACKGROUND_LOCATION` is granted. |
| `getRequiredForegroundPermissions()` | `Array<String>` | Returns array of permissions to request for foreground tracking. |
| `isGpsEnabled(context)` | `Boolean` | Checks if device GPS or Network location providers are enabled. |
| `isIgnoringBatteryOptimizations(context)` | `Boolean` | Checks if app is exempt from OS battery saver restrictions. |
| `createIgnoreBatteryOptimizationIntent(context)` | `Intent` | Intent to prompt user for battery saver exemption. |

---

## ⚙️ Configuration Reference (`LocationConfig`)

Configure parameters using `LocationConfig.Builder()`:

| Builder Method | Default Value | Description |
|---|---|---|
| `.setInterval(ms: Long)` | `10000L` (10s) | Desired location request interval in milliseconds. |
| `.setFastestInterval(ms: Long)` | `5000L` (5s) | Fastest interval your app can handle location updates. |
| `.setMinDistanceMeters(m: Float)` | `0f` | Minimum movement distance in meters to trigger an update. |
| `.setNotificationTitle(title: String)` | `"Location Tracking Active"` | Title shown on the persistent foreground notification. |
| `.setNotificationContent(content: String)` | `"Tracking continuous location in background..."` | Body text shown on the persistent notification. |
| `.setNotificationIcon(iconResId: Int)` | `android.R.drawable.ic_menu_mylocation` | Drawable resource ID for the notification icon. |
| `.setNotificationChannel(id, name)` | `"continuous_location_channel"` | Custom notification channel ID and display name. |
| `.setEnableWatchdog(enable: Boolean)` | `true` | Enables periodic WorkManager watchdog to revive service on kill. |
| `.setWatchdogIntervalMinutes(min: Long)` | `15L` | Watchdog periodic check interval (minimum allowed by Android is 15m). |
| `.setEnableBootRestart(enable: Boolean)` | `true` | Automatically restarts tracking when device reboots. |
| `.setMaxHistoryCount(count: Int)` | `100` | Maximum number of location records retained in local cache. |

---

## 📊 Data Model Reference (`LocationModel`)

The `LocationModel` (also aliased as `LocationData`) object contains full geospatial and telemetry data:

| Property | Type | Description |
|---|---|---|
| `latitude` | `Double` | Latitude in degrees. |
| `longitude` | `Double` | Longitude in degrees. |
| `accuracy` | `Float` | Estimated horizontal accuracy radius in meters. |
| `altitude` | `Double` | Altitude in meters above the WGS 84 reference ellipsoid. |
| `speed` | `Float` | Speed in meters/second. |
| `bearing` | `Float` | Bearing in degrees (0.0 to 360.0). |
| `timestamp` | `Long` | UTC timestamp in milliseconds. |
| `source` | `String` | Source provider (e.g., `"FUSED"`, `"GPS"`, `"NETWORK"`, `"RESTORED"`). |
| `getFormattedTime()` | `String` | Helper function returning timestamp formatted as `yyyy-MM-dd HH:mm:ss`. |

---

## 🛠️ SDK Maintainer Guide

If you are updating the proprietary tracking algorithms in `bglocation-sdk`:

### 1. Make Changes to Core Engine
Edit code inside `bglocation-sdk/src/main/java/...`.

### 2. Build & Export Obfuscated `.aar`
Run the automated Gradle task:

```bash
# On Windows
.\gradlew.bat :bglocation-sdk:buildAndExportAar

# On Linux / macOS
./gradlew :bglocation-sdk:buildAndExportAar
```

This task:
1. Compiles `bglocation-sdk` in `release` mode with ProGuard / R8 code obfuscation.
2. Automatically copies `bglocation-sdk-release.aar` into `bglocation-manager/libs/`.
3. `bglocation-manager` will automatically unpack and link `classes.jar` during build/sync.

### 3. Commit / Distribute
You can commit and share `bglocation-manager` publicly. The core source code inside `bglocation-sdk` remains safe, private, and closed-source on your private repository.

---

## 📱 Android Version Compatibility (API 24 - 36)

The SDK dynamically adapts to every Android release from API 24 through API 36:

| Version | API | OS Specifics Handled Internally |
|---|---|---|
| **Android 7.0–7.1** | **24–25** | Standard service execution and baseline location permissions. |
| **Android 8.0–8.1** | **26–27** | Notification channels, `startForegroundService()` promotion. |
| **Android 9.0** | **28** | `FOREGROUND_SERVICE` permission integration. |
| **Android 10** | **29** | `foregroundServiceType="location"`, two-step `ACCESS_BACKGROUND_LOCATION`. |
| **Android 11** | **30** | Separate background permission prompt flow requirement. |
| **Android 12–12L** | **31–32** | `PendingIntent.FLAG_IMMUTABLE`, WorkManager system exemption for background starts. |
| **Android 13** | **33** | Runtime `POST_NOTIFICATIONS` permission integration. |
| **Android 14** | **34** | `FOREGROUND_SERVICE_LOCATION` permission, `ServiceCompat` location type. |
| **Android 15–16** | **35–36** | Exempt from 6-hour foreground service timeout; **16KB memory page size compatible**. |

---

## 🔍 OEM Device Troubleshooting

### 1. Tracking stops when app is swiped away on Xiaomi / Samsung / OnePlus?
Aggressive battery optimizers terminate background processes.
- **Fix**: Request battery optimization exemption using `LocationManager.createIgnoreBatteryOptimizationIntent(context)`.
- If terminated by OEM memory cleaners, the **WorkManager Watchdog** will automatically resurrect tracking within its periodic window.

### 2. Can the persistent notification be removed?
No. Starting with Android 8.0 (API 26), Google mandates an ongoing persistent notification for any service maintaining active background location tracking. Users can tap the notification or call `LocationManager.stopTracking(context)` to stop tracking cleanly.

### 3. Does tracking resume after phone reboot?
Yes! The library includes a `BootReceiver` that listens for `BOOT_COMPLETED`. If tracking was active before shutdown, it automatically relaunches upon system boot.

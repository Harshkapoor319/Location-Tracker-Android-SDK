package com.it.bglocation.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.it.bglocation.R
import com.it.bglocation.databinding.ActivityMainBinding
import com.it.bglocation.manager.LocationManager
import com.it.bglocation.manager.LocationModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Activity demonstrating how easily any app can control continuous tracking
 * using the public LocationManager.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val historyAdapter = LocationHistoryAdapter()

    // 1. Foreground Location and Notification permissions launcher
    private val foregroundPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            checkAndRequestBackgroundLocation()
        } else {
            Toast.makeText(this, "Location permission is required for tracking", Toast.LENGTH_LONG).show()
        }
    }

    // 2. Background Location launcher (Android 10+ / API 29+)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Background location granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Background location not set to 'Allow all the time'. Foreground Service will maintain tracking.",
                Toast.LENGTH_LONG
            ).show()
        }
        startTracking()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeManagerState()
    }

    private fun setupRecyclerView() {
        binding.rvLocationHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.btnStartTracking.setOnClickListener {
            handleStartTrackingClick()
        }

        binding.btnStopTracking.setOnClickListener {
            // One-line stop call using LocationManager!
            LocationManager.stopTracking(this)
            Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
        }

        binding.btnBatteryOptimization.setOnClickListener {
            promptBatteryOptimizationExemption()
        }

        binding.btnClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all logged coordinates?")
                .setPositiveButton("Clear") { _, _ ->
                    LocationManager.clearHistory(this)
                    refreshHistoryList()
                    binding.tvTotalPoints.text = "0 points"
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun handleStartTrackingClick() {
        if (!LocationManager.isGpsEnabled(this)) {
            AlertDialog.Builder(this)
                .setTitle("Enable GPS / Location")
                .setMessage("Device GPS / Location is turned off. Please turn on Location services.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        if (!LocationManager.hasRequiredPermissions(this)) {
            foregroundPermissionsLauncher.launch(LocationManager.getRequiredForegroundPermissions())
        } else {
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !LocationManager.hasBackgroundPermission(this)
        ) {
            AlertDialog.Builder(this)
                .setTitle("Background Location Permission")
                .setMessage(
                    "To track your location continuously when the app is minimized, closed, or killed, " +
                    "please select 'Allow all the time' on the next screen."
                )
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
        // One-line start call using LocationManager!
        LocationManager.startTracking(this)
        Toast.makeText(this, "Continuous tracking started via LocationManager!", Toast.LENGTH_SHORT).show()
    }

    private fun promptBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!LocationManager.isIgnoringBatteryOptimizations(this)) {
                try {
                    startActivity(LocationManager.createIgnoreBatteryOptimizationIntent(this))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            } else {
                Toast.makeText(this, "Battery saver exemption is already enabled!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Battery optimization exemption not required on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observes live reactive flows from LocationManager!
     */
    private fun observeManagerState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Tracking state flow
                launch {
                    LocationManager.getTrackingStateFlow(this@MainActivity).collect { isTracking ->
                        updateTrackingStatusUI(isTracking)
                    }
                }
                // Location coordinates flow
                launch {
                    LocationManager.getLocationFlow(this@MainActivity).collect { location ->
                        updateLocationMetricsUI(location)
                        refreshHistoryList()
                    }
                }
            }
        }
    }

    private fun updateTrackingStatusUI(isTracking: Boolean) {
        if (isTracking) {
            binding.ivStatusDot.setImageResource(R.drawable.dot_active)
            binding.tvStatusText.text = "Tracking is ACTIVE"
            binding.tvStatusSubtext.text =
                "Foreground Service and WorkManager Watchdog are running. Tracking is active across foreground, background, and killed states."
            binding.btnStartTracking.isEnabled = false
            binding.btnStopTracking.isEnabled = true
        } else {
            binding.ivStatusDot.setImageResource(R.drawable.dot_inactive)
            binding.tvStatusText.text = "Tracking is STOPPED"
            binding.tvStatusSubtext.text =
                "Service and WorkManager are stopped. Tap 'Start Tracking' to begin continuous tracking."
            binding.btnStartTracking.isEnabled = true
            binding.btnStopTracking.isEnabled = false
        }
    }

    private fun updateLocationMetricsUI(location: LocationModel?) {
        if (location == null) {
            binding.tvCurrentCoordinates.text = "Waiting for coordinates..."
            binding.tvAccuracy.text = "-- m"
            binding.tvSpeedAltitude.text = "-- m/s | -- m"
            binding.tvLastUpdated.text = "--"
            binding.tvTotalPoints.text = "${LocationManager.getLocationCount(this)} points"
            return
        }

        binding.tvCurrentCoordinates.text = String.format(
            Locale.US,
            "Lat: %.6f\nLng: %.6f",
            location.latitude,
            location.longitude
        )
        binding.tvAccuracy.text = String.format(Locale.US, "± %.1f meters", location.accuracy)
        binding.tvSpeedAltitude.text = String.format(
            Locale.US,
            "%.1f m/s | %.0f m",
            location.speed,
            location.altitude
        )
        binding.tvLastUpdated.text = "${location.getFormattedTime()} (${location.source})"
        binding.tvTotalPoints.text = "${LocationManager.getLocationCount(this)} points"
    }

    private fun refreshHistoryList() {
        val history = LocationManager.getLocationHistory(this)
        if (history.isEmpty()) {
            binding.tvEmptyHistory.visibility = View.VISIBLE
            binding.rvLocationHistory.visibility = View.GONE
        } else {
            binding.tvEmptyHistory.visibility = View.GONE
            binding.rvLocationHistory.visibility = View.VISIBLE
            historyAdapter.submitList(history)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHistoryList()
        binding.tvTotalPoints.text = "${LocationManager.getLocationCount(this)} points"
    }
}

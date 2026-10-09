package com.it.bglocation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.it.bglocation.databinding.ItemLocationLogBinding
import com.it.bglocation.manager.LocationModel
import java.util.Locale

class LocationHistoryAdapter : RecyclerView.Adapter<LocationHistoryAdapter.LocationViewHolder>() {

    private val items = mutableListOf<LocationModel>()

    fun submitList(newItems: List<LocationModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LocationViewHolder(private val binding: ItemLocationLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationModel) {
            binding.tvLogSource.text = item.source
            binding.tvLogTime.text = item.getFormattedTime()
            binding.tvLogCoordinates.text = String.format(
                Locale.US,
                "Lat: %.6f | Lng: %.6f",
                item.latitude,
                item.longitude
            )
            binding.tvLogMetrics.text = String.format(
                Locale.US,
                "Accuracy: %.1fm | Speed: %.1f m/s | Alt: %.0fm",
                item.accuracy,
                item.speed,
                item.altitude
            )

            // Distinct badge coloring
            if (item.source.contains("Watchdog", ignoreCase = true)) {
                binding.tvLogSource.setBackgroundColor(0xFFFFF3E0.toInt())
                binding.tvLogSource.setTextColor(0xFFE65100.toInt())
            } else {
                binding.tvLogSource.setBackgroundColor(0xFFE3F2FD.toInt())
                binding.tvLogSource.setTextColor(0xFF1976D2.toInt())
            }
        }
    }
}

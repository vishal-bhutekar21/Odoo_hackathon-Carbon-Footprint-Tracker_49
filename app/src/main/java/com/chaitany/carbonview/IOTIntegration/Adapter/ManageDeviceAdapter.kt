package com.chaitany.carbonview.IOTIntegration.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.IOTIntegration.Model.DailyData
import com.chaitany.carbonview.IOTIntegration.Model.RealtimeDevice
import com.chaitany.carbonview.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ManageDeviceAdapter(private val deviceList: MutableList<RealtimeDevice>) :
    RecyclerView.Adapter<ManageDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDeviceName: TextView = view.findViewById(R.id.txtDeviceName)
        val txtModel: TextView = view.findViewById(R.id.txtModel)

        val txtEmission: TextView = view.findViewById(R.id.txtEmission)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val imgEnergySource: ImageView = view.findViewById(R.id.imgEnergySource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manege_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Display data from Firebase only
        updateUI(holder, device, currentDate)

        // Make toggle button read-only
    }

    private fun updateUI(holder: DeviceViewHolder, device: RealtimeDevice, currentDate: String) {
        holder.txtDeviceName.text = device.deviceName
        holder.txtModel.text = "Realtime Device" // Placeholder since no modelName


        // Get daily data for today, default to 0 if not present
        val dailyData = device.data?.get(currentDate) ?: DailyData()
        holder.txtEmission.text = "Emissions: ${String.format("%.4f", dailyData.emissions)} kg CO2"
        updateTimeDisplay(holder.txtTime, dailyData.time)
        setupImageView(holder.imgEnergySource, device)
    }

    private fun setupImageView(imageView: ImageView, device: RealtimeDevice) {
        val imageRes = when {
            device.state == "Off" -> R.drawable.noenergy
            device.state == "On" && device.energySource == "Solar" -> R.drawable.solargrid
            device.state == "On" && device.energySource == "Grid Electricity" -> R.drawable.powergrid
            else -> R.drawable.noenergy
        }
        imageView.setImageResource(imageRes)
    }

    private fun updateTimeDisplay(textView: TextView, totalTimeInHours: Double) {
        val totalSeconds = (totalTimeInHours * 3600).toLong()
        val days = TimeUnit.SECONDS.toDays(totalSeconds)
        val hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        textView.text = "Time: ${days}d ${hours}h ${minutes}m ${seconds}s"
    }

    override fun getItemCount(): Int = deviceList.size
}
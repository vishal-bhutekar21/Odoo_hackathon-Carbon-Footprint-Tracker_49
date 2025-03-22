package com.chaitany.carbonview.IOTIntegration.Adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.IOTIntegration.Model.Device
import com.chaitany.carbonview.IOTIntegration.Model.EmissionData
import com.chaitany.carbonview.IOTIntegration.Model.UserDevice
import com.chaitany.carbonview.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DeviceAdapter(
    private val context: Context,
    private var deviceList: List<Device>,
    private val userId: String
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("UserDevices")

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val modelName: TextView = itemView.findViewById(R.id.device_model)
        val powerRating: TextView = itemView.findViewById(R.id.power_rating)
        val energySource: TextView = itemView.findViewById(R.id.energy_source)
        val co2Emission: TextView = itemView.findViewById(R.id.co2_emission)
        val imgEnergySource: ImageView = itemView.findViewById(R.id.img_energy_source)
        val btnAdd: Button = itemView.findViewById(R.id.btn_add)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]

        holder.deviceName.text = device.deviceName
        holder.modelName.text = "Model: ${device.modelName}"
        holder.powerRating.text = "Power: ${device.powerRating} W"
        holder.energySource.text = "Source: ${device.energySource}"
        holder.co2Emission.text = "CO₂: ${device.co2EmissionFactor} kg/kWh"

        // Set energy source image dynamically
        if (device.energySource.equals("Solar", ignoreCase = true)) {
            holder.imgEnergySource.setImageResource(R.drawable.solargrid) // Solar Image
        } else {
            holder.imgEnergySource.setImageResource(R.drawable.powergrid) // Electric Image
        }

        holder.btnAdd.setOnClickListener {
            showConfirmationDialog(device)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    fun updateList(newList: List<Device>) {
        deviceList = newList
        notifyDataSetChanged()
    }

    private fun showConfirmationDialog(device: Device) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle("Add Device")
            .setMessage("Are you sure you want to add this device?")
            .setPositiveButton("Yes") { _, _ ->
                addDeviceToUser(device)
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.black))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        alertDialog.show()
    }

    private fun addDeviceToUser(device: Device) {
        val deviceId = databaseRef.push().key ?: return
        val userDevice = UserDevice(
            deviceId = deviceId,  // Fix: Assign generated device ID
            deviceName = device.deviceName,
            modelName = device.modelName,
            powerRating = device.powerRating,
            energySource = device.energySource,
            co2EmissionFactor = device.co2EmissionFactor,
            totalTime = 0.0,
            totalEmissions = 0.0,
            state = "Off",
            solarRunningTime = "0",
            data = mapOf(
                "2024-02-20" to EmissionData(0.0, 0.0, 0.0) // Corrected package reference
            )  )

        databaseRef.child(deviceId).setValue(userDevice) // Fix: Add under userId
            .addOnSuccessListener {
                Toast.makeText(context, "Device added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add device. Try again.", Toast.LENGTH_SHORT).show()
            }
    }


}

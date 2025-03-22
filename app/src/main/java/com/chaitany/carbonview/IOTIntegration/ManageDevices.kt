package com.chaitany.carbonview.IOTIntegration

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.IOTIntegration.Adapter.ManageDeviceAdapter
import com.chaitany.carbonview.IOTIntegration.Model.RealtimeDevice
import com.chaitany.carbonview.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class ManageDevices : AppCompatActivity() {

    private lateinit var recyclerDevices: RecyclerView
    private lateinit var tvTotalEmissions: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnGetSuggestions: Button
    private lateinit var deviceAdapter: ManageDeviceAdapter
    private val deviceList = mutableListOf<RealtimeDevice>()
    private val databaseRef = FirebaseDatabase.getInstance().getReference("UserRealtimeDevices")
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_devices)

        // Initialize views
        recyclerDevices = findViewById(R.id.recyclerDevices)
        tvTotalEmissions = findViewById(R.id.tvTotalEmissions)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnGetSuggestions = findViewById(R.id.btnGetSuggestions)

        // Setup RecyclerView
        recyclerDevices.layoutManager = LinearLayoutManager(this)
        deviceAdapter = ManageDeviceAdapter(deviceList)
        recyclerDevices.adapter = deviceAdapter

        // Fetch devices and update totals
        fetchDevices()



        // Get Suggestions button action
        btnGetSuggestions.setOnClickListener {
            Toast.makeText(this, "Suggestions: Reduce usage during peak hours!", Toast.LENGTH_LONG).show()
            // Add more sophisticated suggestion logic here if needed
        }
    }

    private fun fetchDevices() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceList.clear()
                var totalEmissions = 0.0
                var totalTimeHours = 0.0

                for (deviceSnapshot in snapshot.children) {
                    val device = deviceSnapshot.getValue(RealtimeDevice::class.java)
                    device?.let {
                        it.deviceId = deviceSnapshot.key
                        deviceList.add(it)
                        // Accumulate totals from Firebase
                        totalEmissions += it.totalEmissions
                        totalTimeHours += it.totalTime
                    }
                }

                // Update UI with totals
                updateTotals(totalEmissions, totalTimeHours)

                // Update switch states

                deviceAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageDevices, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTotals(totalEmissions: Double, totalTimeHours: Double) {
        // Format emissions
        tvTotalEmissions.text = "${String.format("%.2f", totalEmissions)} kg CO2"

        // Convert total time to hours and minutes
        val totalSeconds = (totalTimeHours * 3600).toLong()
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        tvTotalTime.text = "${hours}h ${minutes}m"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
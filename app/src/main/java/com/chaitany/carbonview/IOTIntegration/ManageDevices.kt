package com.chaitany.carbonview.IOTIntegration

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.IOTIntegration.Adapter.ManageDeviceAdapter
import com.chaitany.carbonview.IOTIntegration.Model.RealtimeDevice
import com.chaitany.carbonview.R
import com.google.android.material.card.MaterialCardView
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
    private lateinit var userRef: DatabaseReference
    private val handler = Handler(Looper.getMainLooper())
    private var totalEmissions = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_devices)

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get email from SharedPreferences and initialize user reference
        val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)?.replace(".", ",")
        if (email != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(email).child("emissions_data").child("device_emissions")
        } else {
            Log.e("ManageDevices", "Email not found in SharedPreferences")
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

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

        // Animate cards entrance
        val cardViewStats = findViewById<CardView>(R.id.cardViewStats)
        cardViewStats.translationY = 200f
        cardViewStats.alpha = 0f
        cardViewStats.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .start()
    }




    private fun fetchDevices() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceList.clear()
                totalEmissions = 0.0
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
                storeDeviceEmissions(totalEmissions, totalTimeHours)

                // Notify adapter
                deviceAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching device data: ${error.message}", error.toException())
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

    private fun storeDeviceEmissions(totalEmissions: Double, totalTimeHours: Double) {
        val emissionsData = mapOf(
            "emissions" to totalEmissions
        )

        userRef.setValue(emissionsData)
            .addOnSuccessListener {
                Log.d("FirebaseSuccess", "Device emissions stored successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Error storing device emissions", e)
                Toast.makeText(this, "Failed to store device emissions", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
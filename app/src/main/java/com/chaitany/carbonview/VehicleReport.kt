package com.chaitany.carbonview

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*
import com.google.android.material.textview.MaterialTextView
import android.view.View
import android.view.Window
import android.view.WindowManager

class VehicleReport : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var carRef: DatabaseReference
    private lateinit var bikeRef: DatabaseReference

    private lateinit var carKmText: MaterialTextView
    private lateinit var carCO2Text: MaterialTextView
    private lateinit var bikeKmText: MaterialTextView
    private lateinit var bikeCO2Text: MaterialTextView
    private lateinit var totalKmText: MaterialTextView
    private lateinit var totalCO2Text: MaterialTextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    private var totalCarKm = 0.0
    private var totalBikeKm = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_report)

        // Set up the Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set status bar color to match the Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        carRef = database.getReference("car_data")
        bikeRef = database.getReference("bike_data")

        // Initialize Views
        carKmText = findViewById(R.id.carKmText)
        carCO2Text = findViewById(R.id.carCO2Text)
        bikeKmText = findViewById(R.id.bikeKmText)
        bikeCO2Text = findViewById(R.id.bikeCO2Text)
        totalKmText = findViewById(R.id.totalKmText)
        totalCO2Text = findViewById(R.id.totalCO2Text)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        fetchVehicleData()
    }

    private fun fetchVehicleData() {
        carRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalCarKm = 0.0
                for (entry in snapshot.children) {
                    val km = entry.child("car_current_km").getValue(Double::class.java) ?: 0.0
                    totalCarKm += km
                }
                fetchBikeData()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching car data", error.toException())
            }
        })
    }

    private fun fetchBikeData() {
        bikeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalBikeKm = 0.0
                for (entry in snapshot.children) {
                    val km = entry.child("bike_current_km").getValue(Double::class.java) ?: 0.0
                    totalBikeKm += km
                }
                calculateAndDisplayData()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching bike data", error.toException())
            }
        })
    }

    private fun calculateAndDisplayData() {
        val carCO2 = (totalCarKm / 15) * 2.68  // Car CO2 in kg
        val bikeCO2 = (totalBikeKm / 40) * 2.32 // Bike CO2 in kg
        val totalCO2 = carCO2 + bikeCO2

        // Update text views (remove prefixes as they are now separate TextViews in the layout)
        carKmText.text = "%.2f km".format(totalCarKm)
        carCO2Text.text = "%.2f kg CO₂".format(carCO2)
        bikeKmText.text = "%.2f km".format(totalBikeKm)
        bikeCO2Text.text = "%.2f kg CO₂".format(bikeCO2)
        totalKmText.text = "%.2f km".format(totalCarKm + totalBikeKm)
        totalCO2Text.text = "%.2f kg CO₂".format(totalCO2)

        updateCharts(carCO2, bikeCO2)
    }

    private fun updateCharts(carCO2: Double, bikeCO2: Double) {
        // Pie Chart
        val pieEntries = listOf(
            PieEntry(carCO2.toFloat(), "Car"),
            PieEntry(bikeCO2.toFloat(), "Bike")
        )
        val pieDataSet = PieDataSet(pieEntries, "CO₂ Emissions")
        pieDataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.primary), // Car - Teal
            ContextCompat.getColor(this, android.R.color.holo_red_dark) // Bike - Red
        )
        pieChart.data = PieData(pieDataSet)
        pieChart.setUsePercentValues(true)
        pieChart.invalidate()

        // Bar Chart
        val barEntries = listOf(
            BarEntry(1f, carCO2.toFloat()),
            BarEntry(2f, bikeCO2.toFloat())
        )
        val barDataSet = BarDataSet(barEntries, "CO₂ Emissions")
        barDataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.primary), // Car - Teal
            ContextCompat.getColor(this, android.R.color.holo_red_dark) // Bike - Red
        )
        barChart.data = BarData(barDataSet)
        barChart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
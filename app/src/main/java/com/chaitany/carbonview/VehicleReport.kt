package com.chaitany.carbonview

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.*
import android.view.Menu
import android.view.MenuItem
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class VehicleReport : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var carRef: DatabaseReference
    private lateinit var bikeRef: DatabaseReference
    private lateinit var userRef: DatabaseReference

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
    private var totalCO2 = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_report)

        // Setup toolbar


        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        carRef = database.getReference("car_data")
        bikeRef = database.getReference("bike_data")

        // Get email from SharedPreferences and initialize user reference
        val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)?.replace(".", ",")
        if (email != null) {
            userRef = database.getReference("users").child(email).child("emissions_data").child("vehicle_emissions")
        } else {
            Log.e("VehicleReport", "Email not found in SharedPreferences")
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

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

        // Animate cards entrance
        val cards = listOf(
            findViewById<MaterialCardView>(R.id.cardCar),
            findViewById<MaterialCardView>(R.id.cardBike),
            findViewById<MaterialCardView>(R.id.cardSummary),
            findViewById<MaterialCardView>(R.id.cardPieChart),
            findViewById<MaterialCardView>(R.id.cardBarChart)
        )

        cards.forEachIndexed { index, card ->
            card.translationY = 200f
            card.alpha = 0f
            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(OvershootInterpolator())
                .start()
        }
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
                Toast.makeText(this@VehicleReport, "Error fetching car data", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@VehicleReport, "Error fetching bike data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateAndDisplayData() {
        val carCO2 = (totalCarKm / 15) * 2.68  // Car CO2 in kg (15 km/L, 2.68 kg CO2/L)
        val bikeCO2 = (totalBikeKm / 40) * 2.32 // Bike CO2 in kg (40 km/L, 2.32 kg CO2/L)
        totalCO2 = carCO2 + bikeCO2

        carKmText.text = "%.2f km".format(totalCarKm)
        carCO2Text.text = "%.2f kg CO₂".format(carCO2)
        bikeKmText.text = "%.2f km".format(totalBikeKm)
        bikeCO2Text.text = "%.2f kg CO₂".format(bikeCO2)
        totalKmText.text = "%.2f km".format(totalCarKm + totalBikeKm)
        totalCO2Text.text = "%.2f kg CO₂".format(totalCO2)

        updateCharts(carCO2, bikeCO2)
        storeVehicleEmissions(totalCO2)
    }

    private fun updateCharts(carCO2: Double, bikeCO2: Double) {
        // Pie Chart
        val pieEntries = listOf(
            PieEntry(carCO2.toFloat(), "Car"),
            PieEntry(bikeCO2.toFloat(), "Bike")
        )
        val pieDataSet = PieDataSet(pieEntries, "CO₂ Emissions")
        pieDataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.red)
        )
        pieDataSet.valueTextColor = Color.WHITE
        pieDataSet.valueTextSize = 12f
        val pieData = PieData(pieDataSet)
        pieChart.apply {
            data = pieData
            setUsePercentValues(true)
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.textColor = ContextCompat.getColor(this@VehicleReport, R.color.primary_dark)
            animateY(1000)
            invalidate()
        }

        // Bar Chart
        val barEntries = listOf(
            BarEntry(1f, carCO2.toFloat()),
            BarEntry(2f, bikeCO2.toFloat())
        )
        val barDataSet = BarDataSet(barEntries, "CO₂ Emissions")
        barDataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.red)
        )
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 12f
        val barData = BarData(barDataSet)
        barChart.apply {
            data = barData
            description.isEnabled = false
            setDrawValueAboveBar(true)
            xAxis.textColor = ContextCompat.getColor(this@VehicleReport, R.color.primary_dark)
            axisLeft.textColor = ContextCompat.getColor(this@VehicleReport, R.color.primary_dark)
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun storeVehicleEmissions(totalCO2: Double) {
        val emissionsData = mapOf(
            "emissions" to totalCO2
        )

        userRef.setValue(emissionsData)
            .addOnSuccessListener {
                Log.d("FirebaseSuccess", "Vehicle emissions stored successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Error storing vehicle emissions", e)
                Toast.makeText(this, "Failed to store emissions data", Toast.LENGTH_SHORT).show()
            }
    }
}
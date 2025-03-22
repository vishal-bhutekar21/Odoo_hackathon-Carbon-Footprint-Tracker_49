package com.chaitany.carbonview

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class IOT_Simulator : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var scope1Text: TextView
    private lateinit var scope2Text: TextView
    private lateinit var scope3Text: TextView
    private lateinit var recentEntryText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var generatePdfButton: MaterialButton
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iot_simulator)

        // Initialize UI Elements
        scope1Text = findViewById(R.id.scope1Text)
        scope2Text = findViewById(R.id.scope2Text)
        scope3Text = findViewById(R.id.scope3Text)
        recentEntryText = findViewById(R.id.recentEntryText)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        generatePdfButton = findViewById(R.id.generatePdfButton)

        database = FirebaseDatabase.getInstance().getReference("emissions_data")

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS", "TextToSpeech initialized successfully")
            } else {
                Log.e("TTS", "Failed to initialize Text-to-Speech")
            }
        }
        fetchData()


        generatePdfButton.setOnClickListener {
            val intent = Intent(this, IOTReport::class.java)
            startActivity(intent)
        }
    }

    private fun fetchData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val emissionsList = mutableListOf<CarbonEmission>()

                for (data in snapshot.children) {
                    val emissionData = data.getValue(CarbonEmission::class.java)

                    // Ensure that retrieved data is not null
                    if (emissionData != null && emissionData.activityType.isNotBlank()) {
                        emissionsList.add(emissionData)
//                        if(emissionData.consumptionAmount*emissionData.emissionFactor > 12000){
//                            showWarningDialog(emissionData)
//                            speakWarning(emissionData)
//
//                        }
                    } else {
                        Log.e("FirebaseError", "Null or Invalid emission data")
                    }
                }

                if (emissionsList.isNotEmpty()) {
                    val classifiedEmissions = ClassificationEngine.classifyAndCalculate(emissionsList)

                    val scope1 = classifiedEmissions["Scope 1"] ?: 0.0
                    val scope2 = classifiedEmissions["Scope 2"] ?: 0.0
                    val scope3 = classifiedEmissions["Scope 3"] ?: 0.0

                    // Update UI
                    scope1Text.text = "Scope 1: %.2f kg".format(scope1)
                    scope2Text.text = "Scope 2: %.2f kg".format(scope2)
                    scope3Text.text = "Scope 3: %.2f kg".format(scope3)

                    // Show most recent entry
                    emissionsList.lastOrNull()?.let {
                        recentEntryText.text = """
                        🔹 Activity: ${it.activityType}
                        📅 Date: ${it.date}
                        🔥 Emission: %.2f kg CO2e
                        🌍 Scope: ${it.classifyScope()}
                    """.trimIndent().format(it.calculateEmission())
                    }

                    // Update Charts
                    updatePieChart(scope1, scope2, scope3)
                    updateBarChart(scope1, scope2, scope3)
                } else {
                    Log.e("FirebaseError", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database Error: ${error.message}")
            }
        })
    }

    private fun showWarningDialog(emissionData: CarbonEmission) {
        val message = """
            ⚠️ High Emission Alert!
            
            🔹 Activity: ${emissionData.activityType}
            🔥 Consumption: ${emissionData.consumptionAmount} units
            🌍 Emission Factor: ${emissionData.emissionFactor}
            ⚡ Total Emission: ${emissionData.consumptionAmount * emissionData.emissionFactor} kg CO2e
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("⚠️ High Emission Detected")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun speakWarning(emissionData: CarbonEmission) {
        val warningMessage = "Warning! High emissions detected for ${emissionData.activityType}. " +
                "Consumption amount is ${emissionData.consumptionAmount} units with an emission factor of " +
                "${emissionData.emissionFactor}. Please investigate the cause."

        textToSpeech?.speak(warningMessage, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    private fun updatePieChart(scope1: Double, scope2: Double, scope3: Double) {
        val entries = listOf(
            PieEntry(scope1.toFloat(), "Scope 1"),
            PieEntry(scope2.toFloat(), "Scope 2"),
            PieEntry(scope3.toFloat(), "Scope 3")
        )

        val dataSet = PieDataSet(entries, "Emissions").apply {
            colors = listOf(Color.RED, Color.BLUE, Color.GREEN)  // Distinct colors
            valueTextColor = Color.WHITE
            valueTextSize = 14f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.text = "Emission Scope Distribution"
            setEntryLabelColor(Color.BLACK)
            setUsePercentValues(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun updateBarChart(scope1: Double, scope2: Double, scope3: Double) {
        val entries = listOf(
            BarEntry(1f, scope1.toFloat()),
            BarEntry(2f, scope2.toFloat()),
            BarEntry(3f, scope3.toFloat())
        )

        val dataSet = BarDataSet(entries, "Emission Data").apply {
            colors = listOf(Color.RED, Color.YELLOW, Color.GREEN)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        barChart.apply {
            data = BarData(dataSet)
            description.text = "Emission Scope Comparison"
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }
}

package com.chaitany.carbonview

import android.util.Log

object CO2Calculator {

    private const val EMISSION_FACTOR = 0.82 // kg CO₂ per kWh

    fun calculateCO2(kWh: Double): Double {
        val emission = kWh * EMISSION_FACTOR
        Log.d("CO2Calculator", "🔥 CO₂ Emission Calculated: $emission kg for $kWh kWh")
        return emission
    }

    fun extractElectricityConsumption(billText: String): Double? {
        Log.d("CO2Calculator", "🔍 Searching for electricity usage in:\n$billText")

        val regexList = listOf(
            Regex("""Units Consumed:\s*([\d,]+(\.\d+)?)"""),
            Regex("""Total kWh:\s*([\d,]+(\.\d+)?)"""),
            Regex("""Energy Used:\s*([\d,]+(\.\d+)?)"""),
            Regex("""([\d,]+(\.\d+)?)\s*kWh""")
        )

        for (regex in regexList) {
            val match = regex.find(billText)
            val extractedValue = match?.groups?.get(1)?.value
            if (extractedValue != null) {
                // Remove commas and extra spaces from the number
                val cleanedValue = extractedValue.replace(",", "").trim()
                val numericValue = cleanedValue.toDoubleOrNull()

                if (numericValue != null) {
                    Log.d("CO2Calculator", "✅ Extracted kWh: $numericValue")
                    return numericValue
                }
            }
        }

        Log.e("CO2Calculator", "❌ No valid electricity usage found in bill!")
        return null
    }
}

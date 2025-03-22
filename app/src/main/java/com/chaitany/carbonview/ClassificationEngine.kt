package com.chaitany.carbonview

import android.util.Log

object ClassificationEngine {
    fun classifyAndCalculate(emissionsList: List<CarbonEmission>): Map<String, Double> {
        val scopeEmissions = mutableMapOf(
            "Scope 1" to 0.0,
            "Scope 2" to 0.0,
            "Scope 3" to 0.0
        )

        for (emission in emissionsList) {
            val calculatedEmission = emission.calculateEmission()
            val scope = emission.classifyScope()

            scopeEmissions[scope] = scopeEmissions[scope]!! + calculatedEmission
        }

        Log.d("ClassificationEngine", "Scope-wise Emissions: $scopeEmissions")
        return scopeEmissions
    }
}

data class CarbonEmission(
    var activityType: String = "",
    var companyName: String = "",
    var consumptionAmount: Double = 0.0,
    var date: String = "",
    var emissionFactor: Double = 0.0
) {
    fun calculateEmission(): Double {
        return consumptionAmount * emissionFactor
    }

    fun classifyScope(): String {
        return when (activityType.lowercase()) {
            // Scope 1: Direct emissions from owned sources
            "business travel", "fuel combustion", "company vehicles", "natural gas usage", "diesel generators" -> "Scope 1"

            // Scope 2: Indirect emissions from purchased energy
            "electricity usage", "purchased heat", "cooling", "office lighting", "data center electricity", "electricity usage","data center energy" -> "Scope 2"

            // Scope 3: Indirect value chain emissions
            "employee commuting", "waste disposal", "supply chain emissions", "logistics", "product transportation",
            "customer product usage", "end-of-life product disposal", "purchased goods and services", "leased assets",
            "water usage", "paper consumption", "vendor activities" -> "Scope 3"

            // Default to Scope 3 if not explicitly listed
            else -> "Scope 3"
        }
    }
}

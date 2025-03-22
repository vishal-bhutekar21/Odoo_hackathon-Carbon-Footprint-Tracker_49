package com.chaitany.carbonview.AISuggestions

import kotlin.random.Random

object MockData {
    data class EmissionData(val scope1: Double, val scope2: Double, val scope3: Double, val devices: List<Device>)
    data class Device(val name: String, val energyKwh: Double, val emissionKg: Double)

    fun getRandomEmissionData(): EmissionData {
        val scope1 = Random.nextDouble(1000.0, 5000.0) // Random Scope 1 (kg CO₂)
        val scope2 = Random.nextDouble(500.0, 2000.0)  // Random Scope 2 (kg CO₂)
        val scope3 = Random.nextDouble(2000.0, 10000.0) // Random Scope 3 (kg CO₂)

        val devices = listOf(
            Device("Air Conditioner", Random.nextDouble(50.0, 200.0), Random.nextDouble(40.0, 160.0)),
            Device("Refrigerator", Random.nextDouble(20.0, 100.0), Random.nextDouble(16.0, 80.0)),
            Device("Vehicle", Random.nextDouble(100.0, 500.0), Random.nextDouble(80.0, 400.0))
        )
        return EmissionData(scope1, scope2, scope3, devices)
    }
}
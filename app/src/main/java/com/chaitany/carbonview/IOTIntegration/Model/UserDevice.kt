package com.chaitany.carbonview.IOTIntegration.Model

data class UserDevice(
    var deviceId: String? = null,
    val deviceName: String = "",
    val modelName: String = "",
    val powerRating: Double = 0.0,  // Fix: Changed from String to Double
    val energySource: String = "",
    val co2EmissionFactor: Double = 0.0,
    var totalTime: Double = 0.0,
    var totalEmissions: Double = 0.0,
    var state: String = "Off",
    var solarRunningTime:String="0",
    var data: Map<String, EmissionData>? = null // Initially null, will store date-wise emissions, time, and distance
)

data class EmissionData(
    val emissions: Double = 0.0,  // CO2 emissions for the date
    val time: Double = 0.0,       // Time duration for the date
    val km: Double = 0.0          // Distance covered in km for the date
)

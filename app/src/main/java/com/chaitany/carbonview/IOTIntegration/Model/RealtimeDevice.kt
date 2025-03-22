package com.chaitany.carbonview.IOTIntegration.Model

data class RealtimeDevice(
    var deviceId: String? = null,
    var deviceName: String = "Unknown Device",
    var powerRating: Double = 0.0,
    var co2EmissionFactor: Double = 0.4,
    var energySource: String = "Grid Electricity",
    var totalTime: Double = 0.0,
    var totalEmissions: Double = 0.0,
    var solarRunningTime: String = "0",
    var state: String = "Off",
    var data: Map<String, DailyData>? = null // Daily data like "2025-03-21" -> {emissions, time, km}
)

data class DailyData(
    var emissions: Double = 0.0,
    var time: Double = 0.0,
    var km: Double = 0.0
)
package com.chaitany.carbonview.IOTIntegration.Model

data class Device(
    val deviceName: String = "",
    val modelName: String = "",
    val powerRating: Double = 0.0,
    val energySource: String = "",
    val co2EmissionFactor: Double = 0.0
)

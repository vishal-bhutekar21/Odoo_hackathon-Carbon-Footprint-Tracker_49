package com.chaitany.carbonview

data class EmissionData(
    val Activity: String = "",
    val Date: String = "",
    val `Emission`: Double = 0.0,  // Use backticks for special characters
    val Scope: String = "",
    val timestamp: String = ""
)

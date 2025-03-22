package com.chaitany.carbonview.SocialPlatform

data class UserProfile(
    val email: String = "", // Sanitized email
    val rawEmail: String = "", // Original email
    val profilePicUrl: String = "", // URL of uploaded profile pic
    val totalPoints: Int = 0 // Calculated total points
)
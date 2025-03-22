package com.chaitany.carbonview.SocialPlatform

data class UserProfile(
    val email: String = "", // Sanitized email
    val rawEmail: String = "", // Original email
    val profilePicUrl: String = "", // Getting Url Link Of Uploaded Image Link
    val totalPoints: Int = 0 // Calculated total points //here we calculates Points
)
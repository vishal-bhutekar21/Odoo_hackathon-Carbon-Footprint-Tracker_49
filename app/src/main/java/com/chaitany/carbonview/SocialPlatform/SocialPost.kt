package com.chaitany.carbonview.SocialPlatform

data class SocialPost(
    val id: String = "",
    val email: String = "", // Sanitized email for Firebase key
    val rawEmail: String = "", // Original email for display
    val eventName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val likes: Int = 0, // Ensure default is 0
    val likedBy: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableMap<String, String> = mutableMapOf(),
    val pointsFromThisPost: Int = 0 // Default to 0
)
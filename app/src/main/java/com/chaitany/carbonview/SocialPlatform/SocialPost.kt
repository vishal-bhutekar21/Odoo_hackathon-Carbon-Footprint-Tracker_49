package com.chaitany.carbonview.SocialPlatform

data class SocialPost(
    val id: String = "",
    val email: String = "", // Sanitized email for Firebase key
    val rawEmail: String = "", // Original email for display
    val eventName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val likes: Int = 0,
    val likedBy: MutableMap<String, Boolean> = mutableMapOf(), // Tracks users who liked (email -> true)
    val comments: MutableMap<String, String> = mutableMapOf(), // Timestamp -> Comment
    val pointsFromThisPost: Int = 0
)
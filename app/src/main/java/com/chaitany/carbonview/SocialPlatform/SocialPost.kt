package com.chaitany.carbonview.SocialPlatform

data class SocialPost(
    val id: String = "",
    val email: String = "", // Sanitized email
    val rawEmail: String = "", // Original email
    val eventName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val likes: Int = 0,
    val likedBy: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableMap<String, String> = mutableMapOf(),
    val pointsFromThisPost: Int = 0
)
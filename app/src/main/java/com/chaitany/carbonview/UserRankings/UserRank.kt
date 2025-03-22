package com.chaitany.carbonview.UserRankings

data class UserRank(
    val email: String = "",
    val totalLikes: Int = 0,
    val totalPoints: Int = 0,
    val rankBadge: String = ""
)
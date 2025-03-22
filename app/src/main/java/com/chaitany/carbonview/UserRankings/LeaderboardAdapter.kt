package com.chaitany.carbonview.UserRankings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R

class LeaderboardAdapter(private val userList: List<UserRank>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rankText)
        val badgeImage: ImageView = itemView.findViewById(R.id.badgeImage)
        val emailText: TextView = itemView.findViewById(R.id.emailText)
        val likesText: TextView = itemView.findViewById(R.id.likesText)
        val pointsText: TextView = itemView.findViewById(R.id.pointsText)
        val pointsProgress: ProgressBar = itemView.findViewById(R.id.pointsProgress)
        val rankIndicator: View = itemView.findViewById(R.id.rankIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        // Rank
        holder.rankText.text = "#${position + 1}"

        // Email
        holder.emailText.text = user.email

        // Likes
        holder.likesText.text = "Likes: ${user.totalLikes}"

        // Points and Progress
        holder.pointsText.text = "${user.totalPoints} pts"
        holder.pointsProgress.progress = user.totalPoints.coerceAtMost(1000)

        // Badge
        val badgeResId = when (user.rankBadge) {
            "diamond" -> R.drawable.diamond
            "platinum" -> R.drawable.gold
            "silver" -> R.drawable.silver
            "bronze" -> R.drawable.bronze
            else -> R.drawable.master
        }
        holder.badgeImage.setImageResource(badgeResId)

        // Dynamic Rank Indicator Color
        val indicatorColor = when (position) {
            0 -> ContextCompat.getColor(context, R.color.gold)
            1 -> ContextCompat.getColor(context, R.color.silver)
            2 -> ContextCompat.getColor(context, R.color.bronze)
            else -> ContextCompat.getColor(context, R.color.grey)
        }
        holder.rankIndicator.setBackgroundColor(indicatorColor)

        // Animation
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 100f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay((position * 100).toLong())
            .start()
    }

    override fun getItemCount() = userList.size
}
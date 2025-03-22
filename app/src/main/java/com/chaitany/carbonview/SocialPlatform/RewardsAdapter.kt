package com.chaitany.carbonview.SocialPlatform

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R
import com.google.android.material.button.MaterialButton

class RewardsAdapter(
    private val rewards: List<Reward>,
    private val onRedeemClick: (Reward) -> Unit
) : RecyclerView.Adapter<RewardsAdapter.RewardViewHolder>() {

    inner class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.reward_name)
        val description: TextView = itemView.findViewById(R.id.reward_description)
        val cost: TextView = itemView.findViewById(R.id.reward_cost)
        val redeemButton: MaterialButton = itemView.findViewById(R.id.redeem_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]
        holder.name.text = reward.name
        holder.description.text = reward.description
        holder.cost.text = "Cost: ${reward.cost} Points"
        holder.redeemButton.setOnClickListener { onRedeemClick(reward) }
    }

    override fun getItemCount(): Int = rewards.size
}
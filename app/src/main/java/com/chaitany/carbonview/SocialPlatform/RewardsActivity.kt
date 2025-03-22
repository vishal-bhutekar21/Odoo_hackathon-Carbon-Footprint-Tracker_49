package com.chaitany.carbonview.SocialPlatform

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaitany.carbonview.R
import com.chaitany.carbonview.databinding.ActivityRewardsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var rewardsAdapter: RewardsAdapter
    private var totalPoints = 0

    private val rewards = listOf(
        Reward("Your Company Gets Free Solar Power for a Week", 50, "A week of free solar energy for your company", "Massive energy savings"),
        Reward("You Got 20% Off Your Next Rent Payment", 750, "Reduce your next rent bill by 20%", "Huge personal savings"),
        Reward("Company Office Goes Carbon Neutral for a Month", 1000, "Offset all office carbon emissions for 30 days", "Epic eco-impact"),
        Reward("Free Electric Vehicle Rental for a Weekend", 1250, "Drive an EV free for 3 days", "Luxury eco-experience"),
        Reward("Your Name on a Wind Turbine Blade", 2000, "Get your name etched on a wind turbine", "Legendary recognition"),
        Reward("Company Gets a Free Green Roof Installation", 3000, "A sustainable roof for your office building", "Game-changing upgrade"),
        Reward("You Win a Year of Free Public Transport", 4000, "Unlimited transit rides for 12 months", "Ultimate commuter perk"),
        Reward("Sponsor a Mini Forest in Your City", 5000, "Fund a small urban forest with your name", "Monumental eco-legacy"),
        Reward("Company Gets a Zero-Waste Audit + Makeover", 7500, "Full waste reduction plan and execution", "Revolutionary sustainability"),
        Reward("You Get a Personal Solar-Powered Tiny Home", 10000, "A mini solar home delivered to you", "Life-changing reward")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rewardsAdapter = RewardsAdapter(rewards) { reward ->
            redeemReward(reward)
        }
        binding.rewardsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RewardsActivity)
            adapter = rewardsAdapter
        }

        fetchUserPoints()
    }

    private fun fetchUserPoints() {
        val sharedPref = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val currentUserEmail = sharedPref.getString("email", "") ?: run {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            return
        }
        val sanitizedEmail = sanitizeEmail(currentUserEmail)
        val database = FirebaseDatabase.getInstance().getReference("SocialPlatform").child(sanitizedEmail)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPointsFromPosts = 0
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(SocialPost::class.java)
                    post?.let { totalPointsFromPosts += it.pointsFromThisPost }
                }
                totalPoints = totalPointsFromPosts // No multiplication if points are pre-calculated
                binding.totalPointsHeader.text = "Your Points: $totalPoints"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RewardsActivity, "Failed to load points: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun redeemReward(reward: Reward) {
        if (totalPoints >= reward.cost) {
            val sharedPref = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
            val currentUserEmail = sharedPref.getString("email", "") ?: return
            val sanitizedEmail = sanitizeEmail(currentUserEmail)
            val database = FirebaseDatabase.getInstance().getReference("Users").child(sanitizedEmail)

            val newPoints = totalPoints - reward.cost
            database.child("points").setValue(newPoints).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    totalPoints = newPoints
                    binding.totalPointsHeader.text = "Your Points: $totalPoints"
                    val redemptionRef = database.child("redeemedRewards").push()
                    redemptionRef.setValue(mapOf(
                        "name" to reward.name,
                        "cost" to reward.cost,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    Toast.makeText(this, "Redeemed: ${reward.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to redeem: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Not enough points for ${reward.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }
}
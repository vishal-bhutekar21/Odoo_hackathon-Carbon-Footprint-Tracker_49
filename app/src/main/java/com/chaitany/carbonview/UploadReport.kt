package com.chaitany.carbonview

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.UserRankings.LeaderboardAdapter
import com.chaitany.carbonview.UserRankings.SocialPost
import com.chaitany.carbonview.UserRankings.UserRank
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UploadReport : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_report)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchAndDisplayLeaderboard()
    }

    private fun fetchAndDisplayLeaderboard() {
        // Assuming you're using Firebase
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("SocialPlatform")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRankList = mutableListOf<UserRank>()

                // Process each user
                snapshot.children.forEach { userSnapshot ->
                    var totalLikes = 0
                    val email = userSnapshot.key ?: ""

                    // Process each post for this user
                    userSnapshot.children.forEach { postSnapshot ->
                        val post = postSnapshot.getValue(SocialPost::class.java)
                        post?.let {
                            totalLikes += it.likes
                        }
                    }

                    val totalPoints = totalLikes * 10
                    val rankBadge = when {
                        totalLikes > 1000 -> "diamond"
                        totalLikes > 250 -> "platinum"
                        totalLikes > 100 -> "silver"
                        totalLikes > 50 -> "bronze"
                        else -> "none"
                    }

                    userRankList.add(UserRank(email, totalLikes, totalPoints, rankBadge))
                }

                // Sort by points descending
                userRankList.sortByDescending { it.totalPoints }
                adapter = LeaderboardAdapter(userRankList)
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UploadReport, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
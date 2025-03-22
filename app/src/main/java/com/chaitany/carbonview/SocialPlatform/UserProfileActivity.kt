package com.chaitany.carbonview.SocialPlatform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chaitany.carbonview.R
import com.chaitany.carbonview.databinding.ActivityUserProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var postsAdapter: UserPostsAdapter
    private val postsList = mutableListOf<SocialPost>()
    private var totalPoints = 0
    private val badges = listOf(
        Badge("Bronze", 50, R.drawable.bronze),
        Badge("Silver", 100, R.drawable.silver),
        Badge("Gold", 250, R.drawable.gold),
        Badge("Diamond", 500, R.drawable.diamond),
        Badge("Heroic", 1000, R.drawable.heroic),
        Badge("Master", 2000, R.drawable.master),
        Badge("Grandmaster", 5000, R.drawable.user)
    )

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfilePic(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get current user's email from SharedPreferences
        val sharedPref = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val currentUserEmail = sharedPref.getString("email", "") ?: ""
        val userName=sharedPref.getString("name","Karan Bankar") ?:""
        val sanitizedEmail = sanitizeEmail(currentUserEmail)

        // Load profile pic from SharedPreferences first
        val profilePicUrl = sharedPref.getString("profilePicUrl", "")
        if (profilePicUrl.isNullOrEmpty()) {
            loadUserProfileFromFirebase(sanitizedEmail, currentUserEmail)
        } else {
            Glide.with(this).load(profilePicUrl).placeholder(R.drawable.user).into(binding.profilePic)
            binding.username.text = userName
        }

        // Set up RecyclerView for posts
        postsAdapter = UserPostsAdapter(postsList)
        binding.postsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserProfileActivity, 3) // 3-column grid like Instagram
            adapter = postsAdapter
        }

        // Set up RecyclerView for badges
        binding.badgesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@UserProfileActivity, 4) // 4 badges per row
            adapter = BadgesAdapter(badges, totalPoints)
        }

        // Fetch user posts and calculate points
        fetchUserPosts(sanitizedEmail)

        // Profile pic click to upload
        binding.profilePic.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadUserProfileFromFirebase(sanitizedEmail: String, rawEmail: String) {
        val database = FirebaseDatabase.getInstance().getReference("Carbonusers").child(sanitizedEmail)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java) ?: UserProfile(sanitizedEmail, rawEmail)
                val sharedPref = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("profilePicUrl", profile.profilePicUrl)
                    apply()
                }
                Glide.with(this@UserProfileActivity)
                    .load(profile.profilePicUrl)
                    .placeholder(R.drawable.user)
                    .into(binding.profilePic)
                binding.username.text = profile.rawEmail
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun fetchUserPosts(sanitizedEmail: String) {
        val database = FirebaseDatabase.getInstance().getReference("SocialPlatform").child(sanitizedEmail)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                var totalLikes = 0
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(SocialPost::class.java)
                    post?.let {
                        postsList.add(it)
                        totalLikes += it.likes
                    }
                }
                totalPoints = totalLikes * 10
                binding.totalPoints.text = totalPoints.toString()
                binding.badgesEarned.text = badges.count { it.threshold <= totalPoints }.toString()
                postsAdapter.notifyDataSetChanged()
                (binding.badgesRecyclerView.adapter as BadgesAdapter).updatePoints(totalPoints)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfileActivity, "Failed to load posts: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun uploadProfilePic(uri: Uri) {
        val sharedPref = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val currentUserEmail = sharedPref.getString("email", "") ?: return
        val sanitizedEmail = sanitizeEmail(currentUserEmail)
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$sanitizedEmail.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val database = FirebaseDatabase.getInstance().getReference("Users").child(sanitizedEmail)
                    val profilePicUrl = downloadUrl.toString()
                    database.child("profilePicUrl").setValue(profilePicUrl)
                    // Save to SharedPreferences
                    with(sharedPref.edit()) {
                        putString("profilePicUrl", profilePicUrl)
                        apply()
                    }
                    Glide.with(this).load(profilePicUrl).into(binding.profilePic)
                    Toast.makeText(this, "Profile pic uploaded", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
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

// Adapter for user's posts
class UserPostsAdapter(private val posts: List<SocialPost>) : RecyclerView.Adapter<UserPostsAdapter.PostViewHolder>() {
    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.itemView.context).load(post.imageUrl).into(holder.postImage)
    }

    override fun getItemCount() = posts.size
}

// Badge data class
data class Badge(val name: String, val threshold: Int, val iconResId: Int)

// Adapter for badges
class BadgesAdapter(private val badges: List<Badge>, private var totalPoints: Int) : RecyclerView.Adapter<BadgesAdapter.BadgeViewHolder>() {
    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeIcon: ImageView = itemView.findViewById(R.id.badge_icon)
        val badgeName: TextView = itemView.findViewById(R.id.badge_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        holder.badgeName.text = badge.name
        if (totalPoints >= badge.threshold) {
            Glide.with(holder.itemView.context).load(badge.iconResId).into(holder.badgeIcon)
            holder.badgeIcon.alpha = 1f // Unlocked
        } else {
            Glide.with(holder.itemView.context).load(R.drawable.lockk  ).into(holder.badgeIcon)
            holder.badgeIcon.alpha = 0.5f // Locked
        }
    }

    override fun getItemCount() = badges.size

    fun updatePoints(newPoints: Int) {
        totalPoints = newPoints
        notifyDataSetChanged()
    }
}
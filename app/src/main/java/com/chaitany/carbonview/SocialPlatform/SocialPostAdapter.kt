package com.chaitany.carbonview.SocialPlatform

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chaitany.carbonview.databinding.ItemSocialPostBinding
import com.google.firebase.database.FirebaseDatabase

class SocialPostAdapter(
    private val posts: MutableList<SocialPost>,
    private val currentUserEmail: String, // Sanitized email of the current user
    private val context: Context // Context for Toast
) : RecyclerView.Adapter<SocialPostAdapter.SocialPostViewHolder>() {

    class SocialPostViewHolder(val binding: ItemSocialPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialPostViewHolder {
        val binding = ItemSocialPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SocialPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SocialPostViewHolder, position: Int) {
        val post = posts[position]
        with(holder.binding) {
            socialUserEmail.text = post.rawEmail // Display original email
            socialEventName.text = post.eventName
            socialDescription.text = post.description
            socialBtnLike.text = "Like (${post.likes})"
            socialCommentsCount.text = "${post.comments.size} Comments"
            Glide.with(root.context).load(post.imageUrl).into(socialPostImage)

            // Like Button Logic
            socialBtnLike.isEnabled = !post.likedBy.containsKey(currentUserEmail) // Disable if already liked
            socialBtnLike.setOnClickListener {
                if (!post.likedBy.containsKey(currentUserEmail)) {
                    Log.d("SocialPlatform", "User $currentUserEmail liking Post ID: ${post.id} owned by ${post.email}")

                    val newLikes = post.likes + 1
                    val newLikedBy = post.likedBy.toMutableMap().apply {
                        put(currentUserEmail, true) // Add current user to likedBy
                    }

                    // Fetch previous points
                    val previousPoints = post.pointsFromThisPost ?: 0

                    // If post owner is liking, increase points differently
                    val newPoints = if (currentUserEmail == post.email) {
                        previousPoints + 10 // Give 10 points to self-like
                    } else {
                        previousPoints + 10 // Give 10 points when others like
                    }

                    val updates = mapOf(
                        "likes" to newLikes,
                        "likedBy" to newLikedBy,
                        "pointsFromThisPost" to newPoints
                    )

                    val sanitizedEmail = sanitizeEmail(post.email)
                    val database = FirebaseDatabase.getInstance().getReference("SocialPlatform")
                    Log.d("SocialPlatform", "Updating Firebase at path: SocialPlatform/$sanitizedEmail/${post.id} with: $updates")

                    database.child(sanitizedEmail).child(post.id).updateChildren(updates)
                        .addOnSuccessListener {
                            posts[position] = post.copy(
                                likes = newLikes,
                                likedBy = newLikedBy,
                                pointsFromThisPost = newPoints
                            )
                            notifyItemChanged(position)
                            Toast.makeText(context, "Liked! Points: $newPoints", Toast.LENGTH_SHORT).show()
                            Log.d("SocialPlatform", "Update succeeded - Likes: $newLikes, Points: $newPoints")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("SocialPlatform", "Update failed: ${e.message}")
                        }
                } else {
                    Log.d("SocialPlatform", "User $currentUserEmail already liked Post ID: ${post.id}")
                }
            }



            // Comment Button Logic with Validation
            socialBtnPostComment.setOnClickListener {
                val comment = socialCommentInput.text.toString().trim()
                if (comment.isNotEmpty()) {
                    post.comments[System.currentTimeMillis().toString()] = "$currentUserEmail: $comment"
                    updatePostInFirebase(post.email, post.id, post) { success ->
                        if (success) {
                            socialCommentInput.text?.clear()
                            notifyItemChanged(position)
                            Toast.makeText(context, "Comment posted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to post comment", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

            // View Comments Logic
            socialViewComments.setOnClickListener {
                socialCommentsList.removeAllViews()
                if (socialCommentsList.visibility == View.GONE) {
                    socialCommentsList.visibility = View.VISIBLE
                    post.comments.forEach { (_, comment) ->
                        val commentView = TextView(root.context).apply {
                            text = comment
                            setPadding(8, 8, 8, 8)
                        }
                        socialCommentsList.addView(commentView)
                    }
                } else {
                    socialCommentsList.visibility = View.GONE
                }
            }
        }
    }

    private fun updatePostInFirebase(email: String, postId: String, post: SocialPost, callback: (Boolean) -> Unit) {
        val sanitizedEmail = sanitizeEmail(email)
        val database = FirebaseDatabase.getInstance().getReference("SocialPlatform")
        val updates = mapOf(
            "likes" to post.likes,
            "likedBy" to post.likedBy,
            "pointsFromThisPost" to post.pointsFromThisPost
        )
        Log.d("SocialPlatform", "Updating Firebase at path: SocialPlatform/$sanitizedEmail/$postId with data: $updates")
        database.child(sanitizedEmail).child(postId).updateChildren(updates)
            .addOnSuccessListener {
                Log.d("SocialPlatform", "Firebase update succeeded for Post ID: $postId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SocialPlatform", "Firebase update failed: ${e.message}")
                callback(false)
            }
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    override fun getItemCount() = posts.size
}
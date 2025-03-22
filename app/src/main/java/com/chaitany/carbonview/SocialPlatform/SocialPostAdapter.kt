package com.chaitany.carbonview.SocialPlatform

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chaitany.carbonview.databinding.ItemSocialPostBinding
import com.google.firebase.database.FirebaseDatabase

class SocialPostAdapter(
    private val posts: MutableList<SocialPost>,
    private val currentUserEmail: String // Sanitized email
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

            // Like Button Logic with Validation
            socialBtnLike.isEnabled = !post.likedBy.containsKey(currentUserEmail) // Disable if already liked
            socialBtnLike.setOnClickListener {
                if (!post.likedBy.containsKey(currentUserEmail)) {
                    val newLikes = post.likes + 1
                    val points = if (newLikes >= 10 && post.likes < 10) 10 else 0
                    post.likedBy[currentUserEmail] = true // Mark as liked by this user
                    val updatedPost = post.copy(
                        likes = newLikes,
                        pointsFromThisPost = post.pointsFromThisPost + points,
                        likedBy = post.likedBy
                    )
                    updatePostInFirebase(post.email, post.id, updatedPost)
                    posts[position] = updatedPost
                    notifyItemChanged(position)
                }
            }

            // Comment Button Logic with Validation
            socialBtnPostComment.setOnClickListener {
                val comment = socialCommentInput.text.toString().trim()
                if (comment.isNotEmpty()) {
                    post.comments[System.currentTimeMillis().toString()] = "$currentUserEmail: $comment"
                    updatePostInFirebase(post.email, post.id, post)
                    socialCommentInput.text?.clear()
                    notifyItemChanged(position)
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

    private fun updatePostInFirebase(email: String, postId: String, post: SocialPost) {
        val sanitizedEmail = sanitizeEmail(email)
        val database = FirebaseDatabase.getInstance().getReference("SocialPlatform")
        database.child(sanitizedEmail).child(postId).setValue(post)
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
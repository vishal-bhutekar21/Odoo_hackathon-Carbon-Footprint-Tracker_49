package com.chaitany.carbonview.SocialPlatform

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.chaitany.carbonview.databinding.ActivitySocialPlatformBinding
import com.chaitany.carbonview.databinding.DialogSocialPostBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar

class SocialPlatformActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialPlatformBinding
    private lateinit var adapter: SocialPostAdapter
    private val posts = mutableListOf<SocialPost>()
    private var selectedImageUri: Uri? = null
    private val rawUserEmail by lazy {
        getSharedPreferences("UserLogin", Context.MODE_PRIVATE).getString("email", "") ?: ""
    }
    private val currentUserEmail by lazy { sanitizeEmail(rawUserEmail) }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        dialogBinding?.socialImagePreview?.let { Glide.with(this).load(uri).into(it) }
    }

    private var dialog: AlertDialog? = null
    private var dialogBinding: DialogSocialPostBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialPlatformBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (currentUserEmail.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        fetchPostsFromFirebase()

        binding.fabAddPost.setOnClickListener { showPostDialog() }
    }

    private fun setupRecyclerView() {
        adapter = SocialPostAdapter(posts, currentUserEmail, this) // Pass 'this' as Context
        binding.socialRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.socialRecyclerView.adapter = adapter
    }

    private fun fetchPostsFromFirebase() {
        val database = FirebaseDatabase.getInstance().getReference("SocialPlatform")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                posts.clear()
                for (userSnapshot in snapshot.children) {
                    val userEmail = userSnapshot.key ?: continue
                    for (postSnapshot in userSnapshot.children) {
                        try {
                            val post = postSnapshot.getValue(SocialPost::class.java)
                            if (post != null) {
                                posts.add(post)
                            } else {
                                Log.e("SocialPlatform", "Null post at $userEmail/${postSnapshot.key}")
                            }
                        } catch (e: Exception) {
                            Log.e("SocialPlatform", "Error parsing post at $userEmail/${postSnapshot.key}: ${e.message}, value: ${postSnapshot.value}")
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SocialPlatformActivity, "Failed to fetch posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPostDialog() {
        dialogBinding = DialogSocialPostBinding.inflate(layoutInflater)
        dialog = AlertDialog.Builder(this)
            .setView(dialogBinding!!.root)
            .setPositiveButton("Submit") { _, _ -> submitPost() }
            .setNegativeButton("Cancel", null)
            .create()

        dialogBinding?.apply {
            socialBtnUploadImage.setOnClickListener { imagePicker.launch("image/*") }
            socialDate.setOnClickListener { showDatePicker() }
        }

        dialog?.show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> dialogBinding?.socialDate?.setText("$day/${month + 1}/$year") },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitPost() {
        if (currentUserEmail.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val eventName = dialogBinding?.socialEventName?.text.toString() ?: ""
        val description = dialogBinding?.socialDescription?.text.toString() ?: ""
        val date = dialogBinding?.socialDate?.text.toString() ?: ""

        if (eventName.isEmpty() || description.isEmpty() || date.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Please fill all fields and upload an image", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("social_images/${System.currentTimeMillis()}.jpg")
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val postId = FirebaseDatabase.getInstance().getReference("SocialPlatform").push().key ?: return@addOnSuccessListener
                    val post = SocialPost(
                        id = postId,
                        email = currentUserEmail,
                        rawEmail = rawUserEmail,
                        eventName = eventName,
                        description = description,
                        imageUrl = downloadUrl.toString(),
                        date = date
                    )
                    FirebaseDatabase.getInstance().getReference("SocialPlatform")
                        .child(currentUserEmail)
                        .child(postId)
                        .setValue(post)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                            dialog?.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to upload post: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to get image URL: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
package com.chaitany.carbonview.SocialPlatform

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
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
    private var dialog: AlertDialog? = null
    private var dialogBinding: DialogSocialPostBinding? = null

    private val prefs by lazy { getSharedPreferences("UserLogin", Context.MODE_PRIVATE) }
    private val rawUserEmail by lazy { prefs.getString("email", "") ?: "" }
    private val currentUserEmail by lazy { sanitizeEmail(rawUserEmail) }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            dialogBinding?.socialImagePreview?.let { imageView ->
                Glide.with(this).load(it).into(imageView)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialPlatformBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!validateUserLogin()) return

        setupUI()
        setupRecyclerView()
        fetchPostsFromFirebase()
    }

    private fun validateUserLogin(): Boolean {
        return if (currentUserEmail.isEmpty()) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show()
            finish()
            false
        } else true
    }

    private fun setupUI() {
        binding.user.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        binding.fabAddPost.setOnClickListener { showPostDialog() }
    }

    private fun setupRecyclerView() {
        adapter = SocialPostAdapter(posts, currentUserEmail, this)
        binding.socialRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SocialPlatformActivity)
            adapter = this@SocialPlatformActivity.adapter
        }
    }

    private fun fetchPostsFromFirebase() {
        FirebaseDatabase.getInstance().getReference("SocialPlatform")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    posts.clear()
                    snapshot.children.forEach { userSnapshot ->
                        val userEmail = userSnapshot.key ?: return@forEach
                        userSnapshot.children.forEach { postSnapshot ->
                            try {
                                postSnapshot.getValue(SocialPost::class.java)?.let { posts.add(it) }
                                    ?: Log.w("SocialPlatform", "Null post at $userEmail/${postSnapshot.key}")
                            } catch (e: Exception) {
                                Log.e("SocialPlatform", "Parse error at $userEmail/${postSnapshot.key}: ${e.message}")
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorToast("Failed to load posts: ${error.message}")
                }
            })
    }

    private fun showPostDialog() {
        dialogBinding = DialogSocialPostBinding.inflate(layoutInflater)
        dialog = AlertDialog.Builder(this)
            .setView(dialogBinding!!.root)
            .setPositiveButton("Post") { _, _ -> submitPost() }
            .setNegativeButton("Cancel") { _, _ -> resetDialog() }
            .setOnDismissListener { resetDialog() }
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
            { _, year, month, day ->
                dialogBinding?.socialDate?.setText(String.format("%02d/%02d/%d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitPost() {
        if (!validatePostInputs()) return

        val eventName = dialogBinding?.socialEventName?.text.toString()
        val description = dialogBinding?.socialDescription?.text.toString()
        val date = dialogBinding?.socialDate?.text.toString()

        uploadImage { imageUrl ->
            savePostToFirebase(eventName, description, date, imageUrl)
        }
    }

    private fun validatePostInputs(): Boolean {
        return when {
            currentUserEmail.isEmpty() -> {
                showErrorToast("User not authenticated")
                false
            }
            dialogBinding?.socialEventName?.text.isNullOrEmpty() ||
                    dialogBinding?.socialDescription?.text.isNullOrEmpty() ||
                    dialogBinding?.socialDate?.text.isNullOrEmpty() ||
                    selectedImageUri == null -> {
                showErrorToast("Please complete all fields and select an image")
                false
            }
            else -> true
        }
    }

    private fun uploadImage(onSuccess: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("social_images/${System.currentTimeMillis()}.jpg")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl
                        .addOnSuccessListener { onSuccess(it.toString()) }
                        .addOnFailureListener { showErrorToast("Failed to get image URL: ${it.message}") }
                }
                .addOnFailureListener { showErrorToast("Image upload failed: ${it.message}") }
        }
    }

    private fun savePostToFirebase(eventName: String, description: String, date: String, imageUrl: String) {
        val postId = FirebaseDatabase.getInstance().getReference("SocialPlatform").push().key ?: return
        val post = SocialPost(
            id = postId,
            email = currentUserEmail,
            rawEmail = rawUserEmail,
            eventName = eventName,
            description = description,
            imageUrl = imageUrl,
            date = date
        )

        FirebaseDatabase.getInstance().getReference("SocialPlatform")
            .child(currentUserEmail)
            .child(postId)
            .setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post created successfully", Toast.LENGTH_SHORT).show()
                dialog?.dismiss()
            }
            .addOnFailureListener { showErrorToast("Failed to save post: ${it.message}") }
    }

    private fun resetDialog() {
        selectedImageUri = null
        dialogBinding = null
        dialog = null
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace(Regex("[.#$\\[\\]]"), "_")
    }

    override fun onDestroy() {
        super.onDestroy()
        resetDialog() // Clean up resources
    }
}
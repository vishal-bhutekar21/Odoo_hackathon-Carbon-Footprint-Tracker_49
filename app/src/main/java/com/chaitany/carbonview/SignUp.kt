package com.chaitany.carbonview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignUp : AppCompatActivity() {

    private lateinit var nameField: TextInputEditText
    private lateinit var mobileField: TextInputEditText
    private lateinit var sizeField: TextInputEditText
    private lateinit var emailField: TextInputEditText
    private lateinit var locationField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var loginButton: MaterialButton

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize views
        nameField = findViewById(R.id.nameField)
        mobileField = findViewById(R.id.mobileField)
        sizeField = findViewById(R.id.sizeField)
        emailField = findViewById(R.id.emailField)
        locationField = findViewById(R.id.locationField)
        passwordField = findViewById(R.id.passwordField)
        signupButton = findViewById(R.id.signupButton)
        progressBar = findViewById(R.id.progressBar)
        loginButton = findViewById(R.id.btnLogin)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        val cbShowPassword = findViewById<CheckBox>(R.id.cbShowPassword)
        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(passwordField, isChecked)
        }

        signupButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val mobile = mobileField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val size = sizeField.text.toString().trim()
            val location = locationField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (validateFields(nameField, mobileField, emailField, sizeField, locationField, passwordField, progressBar)) {
                progressBar.visibility = View.VISIBLE
                checkDeviceRegistration(email, password, name, mobile, size, location)
            }
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun checkDeviceRegistration(email: String, password: String, name: String, mobile: String, size: String, location: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val deviceRef = database.child("DeviceRegistrations").child(deviceId)

        deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Device already registered
                    progressBar.visibility = View.GONE
                    showMultipleAccountDialog()
                } else {
                    // Device not registered, proceed with signup
                    createUser(email, password, name, mobile, size, location, deviceId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SignUp, "Error checking device: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showMultipleAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Account Creation Limit")
            .setMessage("You cannot create multiple accounts from this device. Only one account is allowed per device.")
            .setPositiveButton("OK") { _, _ ->
                // Do nothing, just close the dialog
            }
            .setCancelable(false)
            .show()
    }

    private fun createUser(email: String, password: String, name: String, mobile: String, size: String, location: String, deviceId: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val sanitizedEmail = sanitizeEmail(email)
                val user = User(name, mobile, size, location, email, sanitizedEmail)
                if (userId != null) {
                    // Store user data
                    database.child("Carbonusers").child(userId).setValue(user)
                    // Store in Users/[sanitized_email] for Login compatibility
                    database.child("Users").child(sanitizedEmail).setValue(mapOf(
                        "name" to name,
                        "rawEmail" to email,
                        "email" to sanitizedEmail
                    ))
                    // Register device
                    database.child("DeviceRegistrations").child(deviceId).setValue(mapOf(
                        "userId" to userId,
                        "registeredAt" to System.currentTimeMillis()
                    )).addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                            val sharedPreferences = getSharedPreferences("UserLogin", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("email", email)
                                putString("name", name)
                                putBoolean("deviceRegistered", true) // Optional local flag
                                apply()
                            }
                            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to store user data: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun togglePasswordVisibility(passwordField: TextInputEditText, showPassword: Boolean) {
        passwordField.transformationMethod = if (showPassword) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordField.setSelection(passwordField.text?.length ?: 0)
    }

    private fun validateFields(
        nameField: TextInputEditText,
        mobileField: TextInputEditText,
        emailField: TextInputEditText,
        sizeField: TextInputEditText,
        locationField: TextInputEditText,
        passwordField: TextInputEditText,
        progressBar: ProgressBar
    ): Boolean {
        var isValid = true

        nameField.error = null
        mobileField.error = null
        emailField.error = null
        sizeField.error = null
        locationField.error = null
        passwordField.error = null

        val name = nameField.text.toString().trim()
        val mobile = mobileField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val size = sizeField.text.toString().trim()
        val location = locationField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (name.isEmpty()) {
            nameField.error = "Name cannot be empty"
            isValid = false
        }
        if (mobile.isEmpty()) {
            mobileField.error = "Mobile number cannot be empty"
            isValid = false
        } else if (mobile.length != 10 || !mobile.matches(Regex("^[0-9]{10}$"))) {
            mobileField.error = "Enter a valid 10-digit mobile number"
            isValid = false
        }
        if (email.isEmpty()) {
            emailField.error = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Enter a valid email address"
            isValid = false
        }
        if (size.isEmpty()) {
            findViewById<TextInputLayout>(R.id.sizeInputLayout).error = "Size cannot be empty"
            isValid = false
        }
        if (location.isEmpty()) {
            locationField.error = "Location cannot be empty"
            isValid = false
        }
        if (password.isEmpty()) {
            passwordField.error = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordField.error = "Password must be at least 6 characters long"
            isValid = false
        }

        if (!isValid) {
            progressBar.visibility = View.GONE
        }

        return isValid
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    data class User(
        val name: String = "",
        val mobile: String = "",
        val size: String = "",
        val location: String = "",
        val rawEmail: String = "",
        val email: String = ""
    )
}
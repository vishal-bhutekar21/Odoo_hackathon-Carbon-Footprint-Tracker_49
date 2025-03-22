package com.chaitany.carbonview

import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

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
                createUser (email, password, name, mobile, size, location)
            }
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun createUser (email: String, password: String, name: String, mobile: String, size: String, location: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                // User created successfully, now store additional info in Realtime Database
                val userId = auth.currentUser ?.uid
                val user = User(name, mobile, size, location)
                if (userId != null) {
                    database.child("users").child(userId).setValue(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "User  registered successfully", Toast.LENGTH_SHORT).show()
                                // Optionally send OTP here
                                // val otp = generateOtp()
                                // sendOtpToUser (mobile, otp)
                                // openOtpActivity(mobile, otp, name, email, size, location, password)
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

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private fun togglePasswordVisibility(passwordField: TextInputEditText, showPassword: Boolean) {
        passwordField.transformationMethod = if (showPassword) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordField.setSelection(passwordField.text?.length ?: 0) // Keeps cursor at the end
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

        // Reset previous errors
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

        // Validate Name
        if (name.isEmpty()) {
            nameField.error = "Name cannot be empty"
            isValid = false
        }

        // Validate Mobile Number
        if (mobile.isEmpty()) {
            mobileField.error = "Mobile number cannot be empty"
            isValid = false
        } else if (mobile.length != 10 || !mobile.matches(Regex("^[0-9]{10}$"))) {
            mobileField.error = "Enter a valid 10-digit mobile number"
            isValid = false
        }

        // Validate Email
        if (email.isEmpty()) {
            emailField.error = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Enter a valid email address"
            isValid = false
        }

        // Validate Size
        if (size.isEmpty()) {
            findViewById<TextInputLayout>(R.id.sizeInputLayout).error = "Size cannot be empty"
            isValid = false
        }

        // Validate Location
        if (location.isEmpty()) {
            locationField.error = "Location cannot be empty"
            isValid = false
        }

        // Validate Password
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

    data class User(
        val name: String = "",
        val mobile: String = "",
        val size: String = "",
        val location: String = ""
    )
}
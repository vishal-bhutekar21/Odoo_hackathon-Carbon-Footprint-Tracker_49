package com.chaitany.carbonview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Login : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signUpButton: MaterialButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        emailEditText = findViewById(R.id.emailaddress)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLoginNow)
        signUpButton = findViewById(R.id.btnSignUp)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Retrieve saved login data
        val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("email", "")
        val savedPassword = sharedPreferences.getString("password", "")

        // Auto-fill login fields
        emailEditText.setText(savedEmail)
        passwordEditText.setText(savedPassword)

        // Login button click listener
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateLoginFields(emailEditText, passwordEditText)) {
                // Call login function
                loginUser(email, password)
            }
        }

        // Sign up button click listener
        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
            finish()
        }

        val cbShowPassword = findViewById<CheckBox>(R.id.cbShowPassword)
        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(passwordEditText, isChecked)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Successful login
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                    // Fetch user name from Firebase and store in SharedPreferences
                    val sanitizedEmail = sanitizeEmail(email)
                    val database = FirebaseDatabase.getInstance().getReference("Users").child(sanitizedEmail)
                    database.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val name = snapshot.child("name").getValue(String::class.java) ?: "Karan Bankar"
                            val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putBoolean("isLogged", true)
                                putString("email", email)
                                putString("password", password)
                                putString("name", name) // Store the name
                                apply()
                            }

                            // Proceed to next activity (Dashboard)
                            startActivity(Intent(this@Login, Dashboard::class.java))
                            finish()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@Login, "Failed to fetch user data: ${error.message}", Toast.LENGTH_LONG).show()
                            // Still proceed to Dashboard, but without name
                            val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putBoolean("isLogged", true)
                                putString("email", email)
                                putString("password", password)
                                putString("name", "Unknown User") // Fallback name
                                apply()
                            }
                            startActivity(Intent(this@Login, Dashboard::class.java))
                            finish()
                        }
                    })
                } else {
                    // Provide specific error messages
                    val errorMessage = task.exception?.message ?: "Login failed. Please try again."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun togglePasswordVisibility(passwordField: TextInputEditText, showPassword: Boolean) {
        passwordField.transformationMethod = if (showPassword) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordField.setSelection(passwordField.text?.length ?: 0) // Keeps cursor at the end
    }

    private fun validateLoginFields(
        emailEditText: TextInputEditText,
        passwordEditText: TextInputEditText
    ): Boolean {
        var isValid = true

        // Clear previous errors
        emailEditText.error = null
        passwordEditText.error = null

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate Email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Enter a valid email address"
            isValid = false
        }

        // Validate Password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
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
}
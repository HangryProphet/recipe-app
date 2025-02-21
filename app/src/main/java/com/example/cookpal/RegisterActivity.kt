package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var toggleConfirmPasswordIcon: ImageView
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView  // Add login link reference
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        togglePasswordIcon = findViewById(R.id.togglePasswordIcon)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        toggleConfirmPasswordIcon = findViewById(R.id.toggleConfirmPasswordIcon)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink) // Initialize login link

        // Handle click on "Sign In" text
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            ) // Smooth transition
            finish() // Finish RegisterActivity so user can't go back to it
        }

        // Toggle Password Visibility
        togglePasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePassword(passwordEditText, togglePasswordIcon, isPasswordVisible)
        }

        // Toggle Confirm Password Visibility
        toggleConfirmPasswordIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePassword(
                confirmPasswordEditText,
                toggleConfirmPasswordIcon,
                isConfirmPasswordVisible
            )
        }

        // Register Button Click
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(username, email, password)
            }
        }
    }

    // Toggle Password Method
    private fun togglePassword(editText: EditText, icon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            icon.setImageResource(R.drawable.ic_eye_open) // Change to open eye icon
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            icon.setImageResource(R.drawable.ic_eye_closed) // Change to closed eye icon
        }
        editText.setSelection(editText.text.length) // Keep cursor at end
    }

    // Firebase Register Function
    private fun registerUser(username: String, email: String, password: String) {
        if (username.length > 20) { // ✅ Limit username length
            usernameEditText.error = "Username must be 20 characters or less"
            return
        }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val userMap = hashMapOf(
                                "username" to username,
                                "email" to email,
                                "bio" to "No bio yet.",
                                "followers" to emptyList<String>(),
                                "following" to emptyList<String>()
                            )
                            db.collection("users").document(userId!!)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registration Successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Error saving user: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Registration Failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
    }


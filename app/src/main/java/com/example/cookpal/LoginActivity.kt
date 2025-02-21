package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // ✅ Check if user is already logged in (Redirect to MainActivity)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Prevent user from going back to login screen
            return
        }

        setContentView(R.layout.activity_login)

        // Find Views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        togglePasswordIcon = findViewById(R.id.togglePasswordIcon)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink)

        // Toggle password visibility when eye icon is clicked
        togglePasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_open) // Switch to "eye open"
            } else {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_closed) // Switch back to "eye closed"
            }
            passwordEditText.setSelection(passwordEditText.text.length) // Keep cursor at the end
        }

        // Login Button Click Listener (Authenticate with Firebase)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        // Register Link Click Listener (Redirect to RegisterActivity)
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot Password Click Listener
        forgotPasswordLink.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // Firebase Login Function
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java)) // ✅ Redirect to MainActivity
                    finish()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Show Dialog for Forgot Password
    private fun showForgotPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val input = EditText(this)
        input.hint = "Enter your email"
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // Firebase Forgot Password Function
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

package com.example.cookpal // Adjust to your package

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // The XML you provided

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        togglePasswordIcon = findViewById(R.id.togglePasswordIcon)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)

        // 1. Toggle password visibility when eye icon is clicked
        togglePasswordIcon.setOnClickListener {
            if (passwordEditText.transformationMethod is PasswordTransformationMethod) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_open) // Switch icon to "eye open"
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_closed) // Switch icon back to "eye closed"
            }
            // Move cursor to end
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // 2. Register link - navigate to RegisterActivity (or show a Toast for now)
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))

        }

        // 3. Login button - simple click (for now just show a Toast)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // TODO: Validate inputs or integrate Firebase Auth
            Toast.makeText(this, "Login clicked!\nEmail: $email\nPassword: $password", Toast.LENGTH_SHORT).show()
        }
    }
}

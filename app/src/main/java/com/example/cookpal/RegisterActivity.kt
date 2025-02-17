package com.example.cookpal // Your package here

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var backArrow: ImageView
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var toggleConfirmPasswordIcon: ImageView
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // The XML you provided

        // 1. Initialize all views
        backArrow = findViewById(R.id.backArrow)
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        togglePasswordIcon = findViewById(R.id.togglePasswordIcon)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        toggleConfirmPasswordIcon = findViewById(R.id.toggleConfirmPasswordIcon)
        registerButton = findViewById(R.id.registerButton)

        // 2. Back arrow -> finish this activity (go back to Login)
        backArrow.setOnClickListener {
            finish()
        }

        // 3. Toggle password visibility for main password
        togglePasswordIcon.setOnClickListener {
            if (passwordEditText.transformationMethod is PasswordTransformationMethod) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_open) // or your open-eye icon
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_closed) // revert
            }
            // Move cursor to end
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // 4. Toggle password visibility for confirm password
        toggleConfirmPasswordIcon.setOnClickListener {
            if (confirmPasswordEditText.transformationMethod is PasswordTransformationMethod) {
                // Show confirm password
                confirmPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleConfirmPasswordIcon.setImageResource(R.drawable.ic_eye_open)
            } else {
                // Hide confirm password
                confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleConfirmPasswordIcon.setImageResource(R.drawable.ic_eye_closed)
            }
            // Move cursor to end
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
        }

        // 5. Register button -> (Currently) just show a Toast
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // TODO: Additional validation or Firebase calls, e.g. compare password vs confirm password

            Toast.makeText(this, "Register clicked!\nUsername: $username\nEmail: $email", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivitySignupBinding

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.createAccountButton.setOnClickListener {
            if (validateForm()) {
                createSellerAccount()
            }
        }

        binding.signInLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        with(binding) {
            if (firstNameEditText.text.isNullOrEmpty()) {
                firstNameEditText.error = "First name is required"
                isValid = false
            }

            if (lastNameEditText.text.isNullOrEmpty()) {
                lastNameEditText.error = "Last name is required"
                isValid = false
            }

            if (emailEditText.text.isNullOrEmpty()) {
                emailEditText.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.text.toString()).matches()) {
                emailEditText.error = "Invalid email format"
                isValid = false
            }

            if (phoneEditText.text.isNullOrEmpty()) {
                phoneEditText.error = "Phone number is required"
                isValid = false
            }

            if (passwordEditText.text.isNullOrEmpty()) {
                passwordEditText.error = "Password is required"
                isValid = false
            } else if (passwordEditText.text.toString().length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (confirmPasswordEditText.text.toString() != passwordEditText.text.toString()) {
                confirmPasswordEditText.error = "Passwords don't match"
                isValid = false
            }

            if (!termsCheckbox.isChecked) {
                Toast.makeText(this@SignUpActivity, "Please agree to the terms", Toast.LENGTH_SHORT).show()
                isValid = false
            }
        }

        return isValid
    }

    private fun createSellerAccount() {
        val userData = mapOf(
            "firstName" to binding.firstNameEditText.text.toString(),
            "lastName" to binding.lastNameEditText.text.toString(),
            "email" to binding.emailEditText.text.toString(),
            "phone" to binding.phoneEditText.text.toString()
        )

        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, Maps::class.java))
        finish()
    }
}
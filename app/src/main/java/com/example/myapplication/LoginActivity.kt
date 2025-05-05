package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener { handleLogin() }
        binding.createAccountButton.setOnClickListener { handleCreateAccount() }
        binding.googleLoginButton.setOnClickListener { handleGoogleLogin() }
        binding.facebookLoginButton.setOnClickListener { handleFacebookLogin() }
        binding.forgotPassword.setOnClickListener { handleForgotPassword() }
        binding.signUpText.setOnClickListener { handleSignUp() }
        binding.rememberMe.setOnCheckedChangeListener { _, isChecked ->
            handleRememberMe(isChecked)
        }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun handleLogin() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (validateLoginForm(email, password)) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Maps::class.java))
            finish()
        }
    }

    private fun handleCreateAccount() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun handleGoogleLogin() {
        Toast.makeText(this, "Google login clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleFacebookLogin() {
        Toast.makeText(this, "Facebook login clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleForgotPassword() {
        startActivity(Intent(this, ForgotPasswordActivity::class.java))
    }

    private fun handleSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun handleRememberMe(isChecked: Boolean) {
        val sharedPref = getSharedPreferences("login_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("remember_me", isChecked)
            apply()
        }
    }
}
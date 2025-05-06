package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener { handleLogin() }
        binding.createAccountButton.setOnClickListener { handleCreateAccount() }
        binding.forgotPassword.setOnClickListener { handleForgotPassword() }
        binding.signUpText.setOnClickListener { handleSignUp() }
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
        } else if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun handleLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (validateLoginForm(email, password)) {
            showLoading(true)

            // First check if email exists in Firestore
            firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "No account found with this email",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // If email exists in Firestore, try to authenticate
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userDoc = documents.documents[0]
                                    val accountType = userDoc.getString("accountType") ?: "user"

                                    val intent = Intent(this, Maps::class.java).apply {
                                        putExtra("accountType", accountType)
                                        putExtra("email", email)
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    showLoading(false)
                                    Toast.makeText(
                                        this,
                                        "Invalid password",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Error checking user data: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private fun handleCreateAccount() {
        startActivity(Intent(this, SignUpActivity::class.java))
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
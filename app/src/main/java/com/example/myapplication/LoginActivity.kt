package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        checkAccountType(email)
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Authentication failed: ${authTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
    private fun handleRegularUserLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    checkAccountType(email)
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${authTask.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun checkAccountType(email: String) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "No user data found",
                        Toast.LENGTH_LONG
                    ).show()
                    auth.signOut()
                } else {
                    val userDoc = documents.documents[0]
                    val accountType = userDoc.getString("accountType") ?: "user"

                    if (accountType == "moderator") {
                        // Redirect to ModeratorActivity
                        val modIntent = Intent(this, ModeratorActivity::class.java).apply {
                            putExtra("accountType", accountType)
                            putExtra("email", email)
                        }
                        startActivity(modIntent)
                        finish()
                    } else {
                        // Regular user goes to Maps
                        val intent = Intent(this, Maps::class.java).apply {
                            putExtra("accountType", accountType)
                            putExtra("email", email)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Error checking account type: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
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
}
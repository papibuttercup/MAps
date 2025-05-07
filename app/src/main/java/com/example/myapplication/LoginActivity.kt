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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize Firebase Auth
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        // Check if user is already logged in
        checkCurrentUser()

        setupClickListeners()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: ${currentUser.email}")
            checkAccountType(currentUser.email ?: "")
        }
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
        val rememberMe = binding.rememberMe.isChecked

        Log.d(TAG, "Attempting login for email: $email")

        if (validateLoginForm(email, password)) {
            showLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Log.d(TAG, "Firebase Auth successful")
                        checkAccountType(email)
                    } else {
                        showLoading(false)
                        Log.e(TAG, "Firebase Auth failed", authTask.exception)
                        Toast.makeText(
                            this,
                            "Authentication failed: ${authTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun checkAccountType(email: String) {
        Log.d(TAG, "Checking account type for: $email")
        // First check if it's a seller account
        firestore.collection("sellers")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { sellerDocuments ->
                Log.d(TAG, "Seller check result - Empty: ${sellerDocuments.isEmpty}")
                if (!sellerDocuments.isEmpty) {
                    val sellerDoc = sellerDocuments.documents[0]
                    val verificationStatus = sellerDoc.getString("verificationStatus")
                    Log.d(TAG, "Seller verification status: $verificationStatus")
                    
                    when (verificationStatus) {
                        "pending" -> {
                            showLoading(false)
                            Toast.makeText(
                                this,
                                "Your seller account is pending verification. Please wait for moderator approval.",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                        "approved" -> {
                            Log.d(TAG, "Seller approved, proceeding to Maps")
                            // Seller account is verified, proceed to Maps
                            val intent = Intent(this, Maps::class.java).apply {
                                putExtra("accountType", "seller")
                                putExtra("email", email)
                            }
                            startActivity(intent)
                            finish()
                        }
                        "rejected" -> {
                            showLoading(false)
                            Toast.makeText(
                                this,
                                "Your seller account has been rejected. Please contact support for more information.",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                        else -> {
                            Log.d(TAG, "No seller verification status found, checking regular user")
                            checkRegularUser(email)
                        }
                    }
                } else {
                    Log.d(TAG, "No seller account found, checking regular user")
                    // Not a seller account, check regular user
                    checkRegularUser(email)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error checking seller account", exception)
                Toast.makeText(
                    this,
                    "Error checking account type: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
            }
    }

    private fun checkRegularUser(email: String) {
        Log.d(TAG, "Checking regular user account for: $email")
        firestore.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                Log.d(TAG, "Regular user check result - Empty: ${documents.isEmpty}")
                if (documents.isEmpty) {
                    Log.e(TAG, "No user data found in Firestore")
                    Toast.makeText(
                        this,
                        "No user data found",
                        Toast.LENGTH_LONG
                    ).show()
                    auth.signOut()
                } else {
                    val userDoc = documents.documents[0]
                    val accountType = userDoc.getString("accountType") ?: "user"
                    Log.d(TAG, "User account type: $accountType")

                    when (accountType) {
                        "moderator" -> {
                            Log.d(TAG, "Redirecting to ModeratorActivity")
                            val modIntent = Intent(this, ModeratorActivity::class.java)
                            startActivity(modIntent)
                            finish()
                        }
                        else -> {
                            Log.d(TAG, "Redirecting to Maps as regular user")
                            val intent = Intent(this, Maps::class.java).apply {
                                putExtra("accountType", accountType)
                                putExtra("email", email)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error checking regular user account", exception)
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
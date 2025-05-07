package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateSellerAccountActivity extends AppCompatActivity {
    private EditText firstNameEditText, lastNameEditText, shopNameEditText, shopLocationEditText,
            emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckbox;
    private Button createAccountButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_seller_account);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        firstNameEditText = findViewById(R.id.editTextFirstName);
        lastNameEditText = findViewById(R.id.editTextLastName);
        shopNameEditText = findViewById(R.id.editTextShopName);
        shopLocationEditText = findViewById(R.id.editTextShopLocation);
        emailEditText = findViewById(R.id.editTextEmail);
        phoneEditText = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        termsCheckbox = findViewById(R.id.checkBoxTerms);
        createAccountButton = findViewById(R.id.buttonCreateAccount);
    }

    private void setupClickListeners() {
        createAccountButton.setOnClickListener(v -> {
            if (validateForm()) {
                createSellerAccount();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(firstNameEditText.getText())) {
            firstNameEditText.setError("First name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastNameEditText.getText())) {
            lastNameEditText.setError("Last name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(shopNameEditText.getText())) {
            shopNameEditText.setError("Shop name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(shopLocationEditText.getText())) {
            shopLocationEditText.setError("Shop location is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(emailEditText.getText())) {
            emailEditText.setError("Email is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(phoneEditText.getText())) {
            phoneEditText.setError("Phone number is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(passwordEditText.getText())) {
            passwordEditText.setError("Password is required");
            isValid = false;
        } else if (passwordEditText.getText().length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
            confirmPasswordEditText.setError("Passwords don't match");
            isValid = false;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void createSellerAccount() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveSellerToFirestore(user.getUid());
                        }
                    } else {
                        Toast.makeText(CreateSellerAccountActivity.this,
                                "Account creation failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSellerToFirestore(String userId) {
        Map<String, Object> sellerData = new HashMap<>();
        sellerData.put("firstName", firstNameEditText.getText().toString());
        sellerData.put("lastName", lastNameEditText.getText().toString());
        sellerData.put("shopName", shopNameEditText.getText().toString());
        sellerData.put("shopLocation", shopLocationEditText.getText().toString());
        sellerData.put("email", emailEditText.getText().toString());
        sellerData.put("phone", phoneEditText.getText().toString());
        sellerData.put("accountType", "seller");
        sellerData.put("verificationStatus", "pending");
        sellerData.put("createdAt", System.currentTimeMillis());

        db.collection("sellers").document(userId)
                .set(sellerData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateSellerAccountActivity.this,
                            "Seller account created successfully! Waiting for verification.",
                            Toast.LENGTH_LONG).show();
                    // Sign out the user since they need to wait for verification
                    mAuth.signOut();
                    // Return to login screen
                    startActivity(new Intent(CreateSellerAccountActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateSellerAccountActivity.this,
                            "Error saving seller data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
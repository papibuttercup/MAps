package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountInfoActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView userEmailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If no user is logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Set up views
        userEmailTextView = findViewById(R.id.userEmail);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Display user email
        userEmailTextView.setText(currentUser.getEmail());

        // Set up logout button
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                mAuth.signOut();
                Toast.makeText(AccountInfoActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AccountInfoActivity.this, LoginActivity.class));
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
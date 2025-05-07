package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountInfoActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView userEmailTextView;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get current user
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If no user is logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Set up views
        userEmailTextView = findViewById(R.id.userEmail);
        Button logoutButton = findViewById(R.id.logoutButton);
        TextView setPasswordText = findViewById(R.id.setPasswordText);

        // Display user email
        userEmailTextView.setText(currentUser.getEmail());

        // Set up logout button
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        // Set up password change
        setPasswordText.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null) // Set to null initially
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String currentPassword = currentPasswordInput.getText().toString().trim();
                String newPassword = newPasswordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                if (TextUtils.isEmpty(currentPassword)) {
                    currentPasswordInput.setError("Current password is required");
                    return;
                }

                if (TextUtils.isEmpty(newPassword)) {
                    newPasswordInput.setError("New password is required");
                    return;
                }

                if (newPassword.length() < 6) {
                    newPasswordInput.setError("Password must be at least 6 characters");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    confirmPasswordInput.setError("Passwords don't match");
                    return;
                }

                // Re-authenticate user before changing password
                AuthCredential credential = EmailAuthProvider.getCredential(
                    currentUser.getEmail(), currentPassword);

                currentUser.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Update password
                            currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(AccountInfoActivity.this,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(AccountInfoActivity.this,
                                            "Failed to update password: " + updateTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    }
                                });
                        } else {
                            Toast.makeText(AccountInfoActivity.this,
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
            });
        });

        dialog.show();
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
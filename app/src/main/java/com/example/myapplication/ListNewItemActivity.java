package com.example.myapplication;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ListNewItemActivity extends AppCompatActivity {
    private EditText etCategory, etName, etDescription, etPrice, etImageUrl, etStock;
    private Button btnAddProduct;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_new_item);

        etCategory = findViewById(R.id.etCategory);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etImageUrl = findViewById(R.id.etImageUrl);
        etStock = findViewById(R.id.etStock);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        progressDialog = new ProgressDialog(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProductToFirestore();
            }
        });
    }

    private void addProductToFirestore() {
        String category = etCategory.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(name) || TextUtils.isEmpty(description) ||
                TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(imageUrl) || TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or stock", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Adding product...");
        progressDialog.show();

        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String sellerName = ""; // Optionally fetch seller name from Firestore if needed

        Map<String, Object> product = new HashMap<>();
        product.put("category", category);
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("imageUrl", imageUrl);
        product.put("stock", stock);
        product.put("isAvailable", true);
        product.put("sellerId", sellerId);
        product.put("sellerName", sellerName);
        product.put("createdAt", System.currentTimeMillis());

        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to add product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
} 
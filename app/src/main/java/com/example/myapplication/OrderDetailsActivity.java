package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class OrderDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Initialize Toolbar and set back button
        Toolbar toolbar = findViewById(R.id.toolbarOrders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        TextView txtBuyerName = findViewById(R.id.txtBuyerName);
        ImageView imgProduct = findViewById(R.id.imgProduct);
        TextView txtProductName = findViewById(R.id.txtProductName);
        TextView txtProductPrice = findViewById(R.id.txtProductPrice);
        TextView txtProductQuantity = findViewById(R.id.txtProductQuantity);
        TextView txtPaymentMethod = findViewById(R.id.txtPaymentMethod);
        TextView txtPickup = findViewById(R.id.txtPickup);

        // Set sample data
        txtBuyerName.setText("Peter Parker");
        txtProductName.setText("Black Shirt");
        txtProductPrice.setText("Php 150");
        txtProductQuantity.setText("1X");
        txtPaymentMethod.setText("Cash on Delivery");
        txtPickup.setText("Pick-up");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrderDetailsActivity extends AppCompatActivity {

    private ImageView btnBackOrders;
    private TextView txtBuyerName;
    private ImageView imgProduct;
    private TextView txtProductName;
    private TextView txtProductPrice;
    private TextView txtProductQuantity;
    private TextView txtPaymentMethod;
    private TextView txtPickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details); // Use the new layout file

        txtBuyerName = findViewById(R.id.txtBuyerName);
        imgProduct = findViewById(R.id.imgProduct);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        txtProductQuantity = findViewById(R.id.txtProductQuantity);
        txtPaymentMethod = findViewById(R.id.txtPaymentMethod);
        txtPickup = findViewById(R.id.txtPickup);

        btnBackOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Retrieve order data (if you passed any via Intent)
        // String orderId = getIntent().getStringExtra("orderId");
        // Fetch order details based on orderId from your data source

        // For now, set some static data to match the image
        txtBuyerName.setText("Peter Parker");
        // You'll need to load the actual image based on the product
        txtProductName.setText("Black Shirt");
        txtProductPrice.setText("Php 150");
        txtProductQuantity.setText("1X");
        txtPaymentMethod.setText("Cash on Delivery");
        txtPickup.setText("Pick-up");
    }
}
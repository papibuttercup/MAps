package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class SellerMainActivity extends AppCompatActivity {

    private LinearLayout rowListedItems;
    private LinearLayout rowTotalSales;
    private LinearLayout rowTotalViews;
    private LinearLayout rowOrders;
    private LinearLayout rowReviews;
    private LinearLayout faqButton;
    private LinearLayout privacyButton;
    private LinearLayout settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellermain); // Assuming your XML layout file is named seller_main.xml

        // Initialize the views
        rowListedItems = findViewById(R.id.row_listed_items);
        rowTotalSales = findViewById(R.id.row_total_sales);
        rowTotalViews = findViewById(R.id.row_total_views);
        rowOrders = findViewById(R.id.row_orders);
        rowReviews = findViewById(R.id.row_reviews);
        faqButton = findViewById(R.id.faqButton);
        privacyButton = findViewById(R.id.privacyButton);
        settingsButton = findViewById(R.id.settingsButton);

        // Set click listeners for each row and button
        rowListedItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the Listed Items activity
                Intent intent = new Intent(SellerMainActivity.this, ListedItemsActivity.class);
                startActivity(intent);
            }
        });

        rowTotalSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the Total Sales activity
                Intent intent = new Intent(SellerMainActivity.this, TotalSalesActivity.class);
                startActivity(intent);
            }
        });

        rowTotalViews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the Total Views activity
                Intent intent = new Intent(SellerMainActivity.this, TotalViewsActivity.class);
                startActivity(intent);
            }
        });

        rowOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the Orders activity
                Intent intent = new Intent(SellerMainActivity.this, OrderDetailsActivity.class);
                startActivity(intent);
            }
        });

        rowReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the Reviews activity
                Intent intent = new Intent(SellerMainActivity.this, ReviewsActivity.class);
                startActivity(intent);
            }
        });

    }
}
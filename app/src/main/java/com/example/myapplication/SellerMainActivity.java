package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SellerMainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellermain);

        mAuth = FirebaseAuth.getInstance();

        // Initialize and setup all menu rows
        setupRow(R.id.row_listed_items, "Listed Items", ListedItemsActivity.class);
        setupRow(R.id.row_total_sales, "Total Sales", TotalSalesActivity.class);
        setupRow(R.id.row_total_views, "Total Views", TotalViewsActivity.class);
        setupRow(R.id.row_orders, "Orders", OrderDetailsActivity.class);
        setupRow(R.id.row_reviews, "Reviews", ReviewsActivity.class);

        // Setup bottom navigation buttons
        findViewById(R.id.faqButton).setOnClickListener(v ->
                startActivity(new Intent(this, FAQActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingSellerAct.class)));

        // Logout button
        ImageView btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> logoutUser());
    }

    private void setupRow(int rowId, String title, Class<?> targetActivity) {
        findViewById(rowId).setOnClickListener(v ->
                startActivity(new Intent(this, targetActivity)));

        // If you need to set the title text programmatically:
        TextView titleView = findViewById(rowId).findViewById(R.id.menuText);
        titleView.setText(title);
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_seller_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
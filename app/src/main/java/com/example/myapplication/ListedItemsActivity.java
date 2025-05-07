package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ListedItemsActivity extends AppCompatActivity {

    private ImageView btnBackListedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listed_items); // Use the new layout file

        btnBackListedItems = findViewById(R.id.btnBackListedItems);
        btnBackListedItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle back button click (e.g., finish the activity)
                finish();
            }
        });

        // You'll also need to find and set listeners for the
        // "List New Item" button and potentially handle displaying
        // the actual list of items (likely using a RecyclerView and an Adapter).
    }
}
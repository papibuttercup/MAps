package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class FAQActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        View closeBtn = findViewById(R.id.btnCloseFAQ);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> finish());
        }
    }
}

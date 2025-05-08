package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class SellerMainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellermain);

        mAuth = FirebaseAuth.getInstance();

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // Disable ViewPager2 swipe
        viewPager.setUserInputEnabled(false);

        // Setup ViewPager2 with fragments
        SellerPagerAdapter pagerAdapter = new SellerPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Dashboard");
                        break;
                    case 1:
                        tab.setText("Location");
                        break;
                }
            }
        ).attach();

        // Setup bottom navigation buttons
        findViewById(R.id.faqButton).setOnClickListener(v ->
                startActivity(new Intent(this, FAQActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingSellerAct.class)));

        // Logout button
        findViewById(R.id.btnClose).setOnClickListener(v -> logoutUser());
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
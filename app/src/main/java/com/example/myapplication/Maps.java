package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.FirebaseApp; // Import FirebaseApp
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser ; // Import FirebaseUser
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

public class Maps extends AppCompatActivity {
    private MapView mapView;
    private CardView searchCard;
    private ImageView menuIcon;
    private EditText searchInput;
    private boolean isSearchExpanded = true;
    private FirebaseAuth mAuth; // Declare FirebaseAuth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this); // Initialize Firebase
        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        Mapbox.getInstance(this);

        // Inflate layout
        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);

        // Map style setup
        String key = BuildConfig.MAPTILER_API_KEY;
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + key;

        // Initialize views
        mapView = findViewById(R.id.mapView);
        searchCard = findViewById(R.id.searchCard);
        menuIcon = findViewById(R.id.menuIcon);
        searchInput = findViewById(R.id.searchInput);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            mapboxMap.setStyle(new Style.Builder().fromUri(styleUrl));
            mapboxMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(16.3835, 120.5924))
                    .zoom(15.0)
                    .build());
        });

        // Set click listener
        menuIcon.setOnClickListener(v -> toggleSearchBar());
    }

    private void toggleSearchBar() {
        searchCard.post(() -> {
            View searchContent = searchCard.findViewById(R.id.searchContent);
            EditText input = searchCard.findViewById(R.id.searchInput);
            ImageView searchIcon = searchCard.findViewById(R.id.searchIcon);

            if (isSearchExpanded) {
                animateCollapse(input, searchIcon);
            } else {
                animateExpand(input, searchIcon);
            }

            isSearchExpanded = !isSearchExpanded;
        });
    }

    private void animateCollapse(EditText input, ImageView icon) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(input, "alpha", 1f, 0f);
        ObjectAnimator shrinkX = ObjectAnimator.ofFloat(input, "scaleX", 1f, 0.8f);
        ObjectAnimator slideLeft = ObjectAnimator.ofFloat(input, "translationX", 0f, -50f);
        ObjectAnimator iconFadeOut = ObjectAnimator.ofFloat(icon, "alpha", 1f, 0f);

        AnimatorSet collapseSet = new AnimatorSet();
        collapseSet.playTogether(fadeOut, shrinkX, slideLeft, iconFadeOut);
        collapseSet.setDuration(300);
        collapseSet.setInterpolator(new AccelerateDecelerateInterpolator());

        collapseSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                input.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                input.setTranslationX(0f); // reset position
                hideKeyboard();
            }
        });

        collapseSet.start();
    }

    private void animateExpand(EditText input, ImageView icon) {
        input.setVisibility(View.VISIBLE);
        icon.setVisibility(View.VISIBLE);

        input.setScaleX(0f);
        input.setAlpha(0f);
        icon.setAlpha(0f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(input, "alpha", 0f, 1f);
        ObjectAnimator growX = ObjectAnimator.ofFloat(input, "scaleX", 0f, 1f);
        ObjectAnimator iconFadeIn = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);

        AnimatorSet expandSet = new AnimatorSet();
        expandSet.playTogether(fadeIn, growX, iconFadeIn);
        expandSet.setDuration(250);
        expandSet.setInterpolator(new OvershootInterpolator());

        expandSet.start();
        showKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // Example method to sign in a user
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser  user = mAuth.getCurrentUser ();
                        // Update UI with user information
                    } else {
                        // If sign in fails, display a message to the user.
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
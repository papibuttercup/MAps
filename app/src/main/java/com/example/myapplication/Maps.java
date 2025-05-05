package com.example.myapplication;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class Maps extends AppCompatActivity {
    private MapView mapView;
    private CardView searchCard;
    private ImageView menuIcon;
    private boolean isSearchExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the API Key from BuildConfig
        String key = BuildConfig.MAPTILER_API_KEY;

        // Map style
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + key;

        // Initialize Mapbox
        Mapbox.getInstance(this);

        // Inflate layout
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.activity_main, null);
        setContentView(rootView);

        // Initialize MapView
        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                mapboxMap.setStyle(new Style.Builder().fromUri(styleUrl));
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(16.3835, 120.5924))
                        .zoom(15.0)
                        .build());
            }
        });

        // Initialize search bar components
        searchCard = rootView.findViewById(R.id.searchCard);
        menuIcon = rootView.findViewById(R.id.menuIcon);

        // Set click listener for menu icon to toggle search bar
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSearchBar();

            }

        });
    }

    private void toggleSearchBar() {
        final View searchContent = searchCard.findViewById(R.id.searchContent);

        int startWidth = searchCard.getWidth();
        int targetWidth;

        if (isSearchExpanded) {
            // Collapse state
            targetWidth = menuIcon.getWidth() + (int) (searchCard.getCardElevation() * 2) + 48; // buffer for padding

            // Animate hiding content
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(searchContent, "alpha", 1f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(searchContent, "scaleX", 1f, 0f);

            // Animate width
            ValueAnimator widthAnimator = ValueAnimator.ofInt(startWidth, targetWidth);
            widthAnimator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                searchCard.getLayoutParams().width = val;
                searchCard.requestLayout();
            });

            AnimatorSet set = new AnimatorSet();
            set.playTogether(fadeOut, scaleX, widthAnimator);
            set.setDuration(300);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        } else {
            // Expand state
            searchCard.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            targetWidth = getResources().getDisplayMetrics().widthPixels - (2 * 16); // 16dp margin both sides

            // Animate showing content
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(searchContent, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(searchContent, "scaleX", 0f, 1f);

            // Animate width
            ValueAnimator widthAnimator = ValueAnimator.ofInt(startWidth, targetWidth);
            widthAnimator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                searchCard.getLayoutParams().width = val;
                searchCard.requestLayout();
            });

            AnimatorSet set = new AnimatorSet();
            set.playTogether(fadeIn, scaleX, widthAnimator);
            set.setDuration(300);
            set.setInterpolator(new OvershootInterpolator());
            set.start();
        }

        isSearchExpanded = !isSearchExpanded;
    }



    // ... (keep all the existing MapView lifecycle methods)
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
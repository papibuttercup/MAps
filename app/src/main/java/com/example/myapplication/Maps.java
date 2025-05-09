package com.example.myapplication;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Maps extends AppCompatActivity implements OnMapReadyCallback {
    private static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String MARKERS_PREF = "saved_markers";
    private static final String MARKERS_KEY = "markers_list";
    private AutocompleteAdapter autocompleteAdapter;
    private AutoCompleteTextView searchInput;
    private OkHttpClient httpClient;
    private GeocodingService geocodingService;
    private MapView mapView;
    private MapLibreMap maplibreMap;
    private CardView searchCard;
    private boolean isSearchExpanded = true;

    private LocationComponent locationComponent;
    private FloatingActionButton gpsFab;
    private boolean isTracking = false;
    private LatLng lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);

        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);

        mapView = findViewById(R.id.mapView);
        searchCard = findViewById(R.id.searchCard);
        ImageView menuIcon = findViewById(R.id.menuIcon);
        searchInput = findViewById(R.id.searchInput);
        gpsFab = findViewById(R.id.gpsFab);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        gpsFab.setOnClickListener(v -> toggleGpsTracking());
        menuIcon.setOnClickListener(v -> toggleSearchBar());

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        GeocodingService geocodingService = new MapTilerGeocodingService(this);
        autocompleteAdapter = new AutocompleteAdapter(this, httpClient);
        searchInput.setAdapter(autocompleteAdapter);
        searchInput.setThreshold(1);

// Add this item click listener
        searchInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            searchInput.setText(selectedItem);
            performSearch();
        });

        // Set up search input listener
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void performSearch() {
        String searchText = searchInput.getText().toString().trim();
        if (!searchText.isEmpty()) {
            searchLocation(searchText);
            hideKeyboard();
        } else {
            showToast("Please enter a location");
        }
    }

    // Add this inner class for the autocomplete adapter
    private class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> suggestions;
        private final OkHttpClient httpClient;

        public AutocompleteAdapter(Context context, OkHttpClient httpClient) {
            super(context, android.R.layout.simple_dropdown_item_1line);
            this.suggestions = new ArrayList<>();
            this.httpClient = httpClient;
        }

        @Override
        public int getCount() {
            return suggestions.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return position < suggestions.size() ? suggestions.get(position) : null;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint != null) {
                        List<String> newSuggestions = fetchSuggestions(constraint.toString());
                        results.values = newSuggestions;
                        results.count = newSuggestions.size();
                    }
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        suggestions.clear();
                        suggestions.addAll((List<String>) results.values);
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }

        private List<String> fetchSuggestions(String query) {
            List<String> newSuggestions = new ArrayList<>();
            String apiKey = BuildConfig.MAPTILER_API_KEY;
            String url = "https://api.maptiler.com/geocoding/" + query + ".json?key=" + apiKey + "&limit=5&autocomplete=true";

            Request request = new Request.Builder().url(url).build();

            try {
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                    JsonArray features = jsonObject.getAsJsonArray("features");

                    if (features != null) {
                        for (int i = 0; i < features.size(); i++) {
                            JsonObject feature = features.get(i).getAsJsonObject();
                            String placeName = feature.get("place_name").getAsString();
                            newSuggestions.add(placeName);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("Autocomplete", "Error fetching suggestions", e);
            }
            return newSuggestions;
        }
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.maplibreMap = mapLibreMap;

        String mapTilerApiKey = BuildConfig.MAPTILER_API_KEY;
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + mapTilerApiKey;

        mapLibreMap.setStyle(new Style.Builder().fromUri(styleUrl), this::setupMapStyle);
    }

    private void setupMapStyle(@NonNull Style style) {
        maplibreMap.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(16.3835, 120.5924))
                .zoom(15.0)
                .build());

        if (checkLocationPermission()) {
            enableLocationComponent(style);
        }

        // Set marker click listener to show the delete option
        maplibreMap.setOnMarkerClickListener(marker -> {
            showDeleteMarkerDialog(marker);
            return true;
        });

        maplibreMap.addOnMapLongClickListener(point -> {
            addMarker(point.getLatitude(), point.getLongitude(), "Custom Marker");
            return true;
        });

        loadMarkers(); // Load saved markers when map is ready
    }

    private void saveMarkers() {
        List<MarkerData> markers = new ArrayList<>();
        for (Marker marker : maplibreMap.getMarkers()) {
            markers.add(new MarkerData(
                    marker.getPosition().getLatitude(),
                    marker.getPosition().getLongitude(),
                    marker.getTitle()
            ));
        }

        String json = new Gson().toJson(markers);
        getSharedPreferences(MARKERS_PREF, MODE_PRIVATE)
                .edit()
                .putString(MARKERS_KEY, json)
                .apply();
    }

    private void loadMarkers() {
        String json = getSharedPreferences(MARKERS_PREF, MODE_PRIVATE)
                .getString(MARKERS_KEY, null);

        if (json != null) {
            Type type = new TypeToken<List<MarkerData>>(){}.getType();
            List<MarkerData> markers = new Gson().fromJson(json, type);

            if (markers != null) {
                for (MarkerData markerData : markers) {
                    maplibreMap.addMarker(new MarkerOptions()
                            .position(new LatLng(markerData.getLatitude(), markerData.getLongitude()))
                            .setTitle(markerData.getTitle()));
                }
            }
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (maplibreMap != null) {
                maplibreMap.getStyle(this::enableLocationComponent);
            }
        }
    }

    private void enableLocationComponent(@NonNull Style style) {
        try {
            LocationComponentActivationOptions options =
                    LocationComponentActivationOptions.builder(this, style)
                            .useDefaultLocationEngine(true)
                            .build();

            locationComponent = maplibreMap.getLocationComponent();
            locationComponent.activateLocationComponent(options);

            if (checkLocationPermission()) {
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.COMPASS);
            }
        } catch (Exception e) {
            Log.e("LocationComponent", "Error enabling location component", e);
        }
    }

    private void toggleGpsTracking() {
        if (locationComponent == null || !locationComponent.isLocationComponentEnabled()) {
            if (checkLocationPermission() && maplibreMap != null) {
                maplibreMap.getStyle(this::enableLocationComponent);
            }
            return;
        }

        isTracking = !isTracking;

        if (isTracking) {
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            gpsFab.setImageResource(R.drawable.ic_gps_fixed);
            showToast("GPS Tracking Enabled");

            Location lastLocation = locationComponent.getLastKnownLocation();
            if (lastLocation != null) {
                addMarker(lastLocation.getLatitude(), lastLocation.getLongitude(), "Current Location");
            }
        } else {
            locationComponent.setCameraMode(CameraMode.NONE);
            locationComponent.setRenderMode(RenderMode.NORMAL);
            gpsFab.setImageResource(R.drawable.ic_gps_not_fixed);
            showToast("GPS Tracking Disabled");
        }
    }

    private void addMarker(double lat, double lng, String title) {
        LatLng position = new LatLng(lat, lng);
        maplibreMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title));

        maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        showToast("Marker added: " + title);
        saveMarkers(); // Save after adding
    }

    private void showDeleteMarkerDialog(final Marker marker) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Marker")
                .setMessage("Do you want to delete this marker?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    marker.remove();
                    saveMarkers(); // Save after deletion
                    showToast("Marker deleted");
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void toggleSearchBar() {
        searchCard.post(() -> {
            View searchContent = searchCard.findViewById(R.id.searchContent);
            AutoCompleteTextView input = searchCard.findViewById(R.id.searchInput); // Changed to AutoCompleteTextView
            ImageView searchIcon = searchCard.findViewById(R.id.searchIcon);

            if (isSearchExpanded) {
                animateCollapse(input, searchIcon);
            } else {
                animateExpand(input, searchIcon);
            }

            isSearchExpanded = !isSearchExpanded;
        });
    }

    private void animateCollapse(AutoCompleteTextView input, ImageView icon) { // Changed parameter type
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

    private void animateExpand(AutoCompleteTextView input, ImageView icon) { // Changed parameter type
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void searchLocation(String address) {
        String apiKey = BuildConfig.MAPTILER_API_KEY;
        String encodedAddress;
        try {
            encodedAddress = URLEncoder.encode(address, "UTF-8");
        } catch (Exception e) {
            showToast("Error processing address");
            return;
        }

        String url = "https://api.maptiler.com/geocoding/" + encodedAddress + ".json?key=" + apiKey + "&limit=1";

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("Error searching for location"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> showToast("Location not found"));
                    return;
                }

                String responseBody = response.body().string();
                JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                JsonArray features = jsonObject.getAsJsonArray("features");

                if (features.size() > 0) {
                    JsonObject feature = features.get(0).getAsJsonObject();
                    JsonArray center = feature.getAsJsonArray("center");
                    double longitude = center.get(0).getAsDouble();
                    double latitude = center.get(1).getAsDouble();
                    String placeName = feature.get("place_name").getAsString();

                    runOnUiThread(() -> {
                        addMarker(latitude, longitude, placeName);
                        maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(latitude, longitude), 15));
                    });
                } else {
                    runOnUiThread(() -> showToast("Location not found"));
                }
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
        saveMarkers(); // Save markers when app closes
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Inner class for marker data persistence
    private static class MarkerData {
        private double latitude;
        private double longitude;
        private String title;

        public MarkerData(double latitude, double longitude, String title) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.title = title;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getTitle() { return title; }
    }
}
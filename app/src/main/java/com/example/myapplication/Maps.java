package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.Objects;
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

    private boolean isSearchExpanded = true;

    private LocationComponent locationComponent;
    private FloatingActionButton gpsFab;
    private boolean isTracking = false;
    private LatLng lastKnownLocation;
    private View menuIcon;
    private ImageView searchIcon;
    private boolean isVerifiedSeller = false;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        checkSellerVerification();
        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);

        mapView = findViewById(R.id.mapView);
        menuIcon = findViewById(R.id.menuIcon);
        searchInput = findViewById(R.id.searchInput);
        gpsFab = findViewById(R.id.gpsFab);
        searchIcon = findViewById(R.id.searchIcon);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        gpsFab.setOnClickListener(v -> toggleGpsTracking());
        menuIcon.setOnClickListener(this::showMenuDropdown);
        searchIcon.setOnClickListener(v -> performSearch());
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
    private void checkSellerVerification() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            isVerifiedSeller = false;
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String accountType = documentSnapshot.getString("accountType");
                        if ("seller".equals(accountType)) {
                            checkVerificationStatus(currentUser.getUid());
                        } else {
                            isVerifiedSeller = false;
                        }
                    } else {
                        isVerifiedSeller = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Maps", "Error checking account type", e);
                    isVerifiedSeller = false;
                });
    }

    private void checkVerificationStatus(String userId) {
        db.collection("sellers")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("verificationStatus");
                        isVerifiedSeller = "approved".equals(status);
                        if (isVerifiedSeller) {
                            showToast("Verified seller access granted");
                        }
                    } else {
                        isVerifiedSeller = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Maps", "Error checking verification status", e);
                    isVerifiedSeller = false;
                });
    }
    private void showMenuDropdown(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.custom_dropdown_menu, null);

        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true); // Focusable so it will close when clicking outside

        // Calculate proper positioning
        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);

        // Get screen dimensions
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        // Measure the popup view first to get its width
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        int xPos = anchorLocation[0];

        // Position below menu icon with 10px margin
        int yPos = anchorLocation[1] + anchor.getHeight() + 32;

        // Ensure dropdown doesn't go off screen bottom
        if (yPos + popupHeight > screenHeight) {
            // If dropdown would go off bottom of screen, show above instead
            yPos = anchorLocation[1] - popupHeight - 10;
        }

        // Show popup
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

        // Handle menu item clicks
        popupView.findViewById(R.id.menu_account).setOnClickListener(v -> {
            // Start the AccountInfoActivity
            android.content.Intent intent = new android.content.Intent(this, AccountInfoActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_categories).setOnClickListener(v -> {
            // Start the CategoriesActivity
            Intent intent = new Intent(this, CategoriesActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            // Start the SettingsActivity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_help).setOnClickListener(v -> {
            Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_seller).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateSellerAccountActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });
    }

    private void performSearch() {
        String searchText = searchInput.getText().toString().trim();
        if (!searchText.isEmpty()) {
            searchLocation(searchText);
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
        // Set marker click listener to show the delete option (only for sellers)
        // Set marker click listener (only verified sellers can delete)
        maplibreMap.setOnMarkerClickListener(marker -> {
            if (isVerifiedSeller) {
                showDeleteMarkerDialog(marker);
            }
            return true;
        });

        // Only allow long press to add markers if user is a verified seller
        if (isVerifiedSeller) {
            maplibreMap.addOnMapLongClickListener(point -> {
                addMarker(point.getLatitude(), point.getLongitude(), "Custom Marker");
                return true;
            });
        }

        loadMarkers();
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

        } else {
            locationComponent.setCameraMode(CameraMode.NONE);
            locationComponent.setRenderMode(RenderMode.NORMAL);
            gpsFab.setImageResource(R.drawable.ic_gps_not_fixed);
            showToast("GPS Tracking Disabled");
        }
    }

    private void addMarker(double lat, double lng, String title) {
        if (!isVerifiedSeller) {
            showToast("Only verified sellers can add markers");
            return;

        }

        LatLng position = new LatLng(lat, lng);
        maplibreMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title));

        maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        showToast("Marker added: " + title);
        saveMarkers();
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
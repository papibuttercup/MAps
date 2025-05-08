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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

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
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.layers.PropertyFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.widget.RatingBar;
import android.widget.Button;
import android.widget.ImageButton;

import java.util.concurrent.TimeUnit;

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
    private MapLibreMap map;

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

    private BottomSheetDialog collapsedSheetDialog;

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

        searchInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            searchInput.setText(selectedItem);
            performSearch();
        });

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

        db.collection("sellers")
                .document(currentUser.getUid())
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
                true);

        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        int xPos = anchorLocation[0];

        int yPos = anchorLocation[1] + anchor.getHeight() + 32;

        if (yPos + popupHeight > screenHeight) {
            yPos = anchorLocation[1] - popupHeight - 10;
        }

        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

        popupView.findViewById(R.id.menu_account).setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountInfoActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_categories).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoriesActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_settings).setOnClickListener(v -> {
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

        loadAllSellerMarkers();

        maplibreMap.setOnMarkerClickListener(marker -> {
            String shopName = marker.getTitle();
            String locationName = "Sample Location";
            showCollapsedBottomSheet(shopName, locationName);
            return true;
        });
    }

    private void loadAllSellerMarkers() {
        db.collection("seller_markers")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Double lat = document.getDouble("latitude");
                    Double lng = document.getDouble("longitude");
                    String title = document.getString("title");
                    
                    if (lat != null && lng != null && title != null) {
                        MarkerOptions markerOptions = new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(title);
                        
                        maplibreMap.addMarker(markerOptions);
                    }
                }
            })
            .addOnFailureListener(e -> {
                String errorMessage = "Error loading markers: " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("Maps", errorMessage, e);
            });
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
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void showCollapsedBottomSheet(String shopName, String locationName) {
        try {
            if (collapsedSheetDialog != null && collapsedSheetDialog.isShowing()) {
                collapsedSheetDialog.dismiss();
            }

            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_collapsed, null);
            collapsedSheetDialog = new BottomSheetDialog(this);
            collapsedSheetDialog.setContentView(bottomSheetView);

            TextView shopNameView = bottomSheetView.findViewById(R.id.shopName);
            TextView locationNameView = bottomSheetView.findViewById(R.id.locationName);
            Button expandButton = bottomSheetView.findViewById(R.id.expandButton);

            if (shopNameView == null || locationNameView == null || expandButton == null) {
                Log.e("Maps", "Failed to find views in bottom sheet");
                showToast("Error loading shop details");
                return;
            }

            shopNameView.setText(shopName);
            locationNameView.setText(locationName);

            expandButton.setOnClickListener(v -> {
                collapsedSheetDialog.dismiss();
                showOrderItemSheet(shopName, locationName);
            });

            collapsedSheetDialog.show();
        } catch (Exception e) {
            Log.e("Maps", "Error showing bottom sheet", e);
            showToast("Error loading shop details");
        }
    }

    private void showOrderItemSheet(String shopName, String locationName) {
        getSellerIdForShop(shopName, new SellerIdCallback() {
            @Override
            public void onSuccess(String sellerId) {
                showProductListBottomSheet(sellerId);
            }

            @Override
            public void onFailure(String error) {
                showToast("Error: " + error);
            }
        });
    }

    private void showProductListBottomSheet(String sellerId) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_product_list, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView recyclerView = bottomSheetView.findViewById(R.id.productRecyclerView);
        TextView emptyView = bottomSheetView.findViewById(R.id.emptyView);
        ImageButton closeButton = bottomSheetView.findViewById(R.id.closeProductList);

        if (recyclerView == null || emptyView == null || closeButton == null) {
            Log.e("Maps", "Failed to find views in bottom sheet");
            showToast("Error loading products");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        List<Product> products = new ArrayList<>();
        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Product product = document.toObject(Product.class);
                    if (product != null && product.getStock() > 0) {
                        product.setId(document.getId());
                        products.add(product);
                    }
                }

                if (products.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    ProductAdapter adapter = new ProductAdapter(products, product -> {
                        placeOrder(product);
                        bottomSheetDialog.dismiss();
                    });
                    recyclerView.setAdapter(adapter);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Maps", "Error fetching products", e);
                showToast("Error loading products");
            });

        bottomSheetDialog.show();
    }

    private void placeOrder(Product product) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showToast("Please login to place an order");
            return;
        }

        Map<String, Object> order = new HashMap<>();
        order.put("productId", product.getId());
        order.put("productName", product.getName());
        order.put("price", product.getPrice());
        order.put("sellerId", product.getSellerId());
        order.put("customerId", currentUser.getUid());
        order.put("status", "pending");
        order.put("timestamp", FieldValue.serverTimestamp());

        db.collection("orders")
            .add(order)
            .addOnSuccessListener(documentReference -> {
                showToast("Order placed successfully");
                db.collection("products").document(product.getId())
                    .update("stock", FieldValue.increment(-1))
                    .addOnFailureListener(e -> Log.e("Maps", "Error updating stock", e));
            })
            .addOnFailureListener(e -> {
                Log.e("Maps", "Error placing order", e);
                showToast("Error placing order");
            });
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final List<Product> products;
        private final OnOrderClickListener listener;

        ProductAdapter(List<Product> products, OnOrderClickListener listener) {
            this.products = products;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.productName.setText(product.getName());
            holder.productPrice.setText(String.format("$%.2f", product.getPrice()));
            holder.productStock.setText(String.format("Stock: %d", product.getStock()));

            RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .centerCrop();

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.placeholder_image);
            }

            holder.orderButton.setOnClickListener(v -> {
                if (product.getStock() > 0) {
                    listener.onOrder(product);
                } else {
                    showToast("Product out of stock");
                }
            });
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName, productPrice, productStock;
            Button orderButton;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.productImage);
                productName = itemView.findViewById(R.id.productName);
                productPrice = itemView.findViewById(R.id.productPrice);
                productStock = itemView.findViewById(R.id.productStock);
                orderButton = itemView.findViewById(R.id.orderButton);
            }
        }
    }
    private interface OnOrderClickListener {
        void onOrder(Product product);
    }

    private void updateAverageRating(String sellerId, TextView shopRatingView) {
        db.collection("sellers").document(sellerId)
            .collection("ratings").get()
            .addOnSuccessListener(querySnapshot -> {
                double sum = 0;
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot) {
                    Number rating = (Number) doc.get("rating");
                    if (rating != null) {
                        sum += rating.doubleValue();
                        count++;
                    }
                }
                double avg = count > 0 ? sum / count : 0;
                db.collection("sellers").document(sellerId)
                    .update("averageRating", avg)
                    .addOnSuccessListener(aVoid -> {
                        if (shopRatingView != null) shopRatingView.setText("★ " + String.format("%.1f", avg));
                    });
            });
    }

    // Add this interface at the top of your Maps class (with other member variables)
    private interface SellerIdCallback {
        void onSuccess(String sellerId);
        void onFailure(String error);
    }

    // Replace your existing getSellerIdForShop() method with this:
    private void getSellerIdForShop(String shopName, SellerIdCallback callback) {
        if (shopName == null || shopName.isEmpty()) {
            callback.onFailure("Invalid shop name");
            return;
        }

        // First find the marker in seller_markers collection
        db.collection("seller_markers")
                .whereEqualTo("title", shopName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String sellerId = queryDocumentSnapshots.getDocuments().get(0).getString("sellerId");
                        if (sellerId != null) {
                            // Now verify this is an approved seller
                            db.collection("sellers")
                                    .document(sellerId)
                                    .get()
                                    .addOnSuccessListener(sellerDoc -> {
                                        if (sellerDoc.exists() && "approved".equals(sellerDoc.getString("verificationStatus"))) {
                                            callback.onSuccess(sellerId);
                                        } else {
                                            callback.onFailure("Shop is not verified");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Maps", "Error finding seller", e);
                                        callback.onFailure("Error finding shop");
                                    });
                        } else {
                            callback.onFailure("Shop not found");
                        }
                    } else {
                        callback.onFailure("Shop not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Maps", "Error finding marker", e);
                    callback.onFailure("Error finding shop");
                });
    }

    private void setupMapTiler() {
        mapView.getMapAsync(mapLibreMap -> {
            this.map = mapLibreMap;
            
            // Set the style URL with your MapTiler API key
            String styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=" + getString(R.string.maptiler_api_key);
            map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
                // Style is loaded
                map.getStyle().addSource(new GeoJsonSource("markers"));
                
                // Add marker layer
                map.getStyle().addLayer(new SymbolLayer("markers", "markers")
                    .withProperties(
                        PropertyFactory.iconImage("marker"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                    ));
                
                // Load markers
                loadAllSellerMarkers();
            });
        });
    }
}
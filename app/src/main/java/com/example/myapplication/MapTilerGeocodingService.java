// MapTilerGeocodingService.java
package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.maplibre.android.geometry.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapTilerGeocodingService implements GeocodingService {
    private static final String TAG = "MapTilerGeocoding";
    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    public MapTilerGeocodingService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    @Override
    public List<String> getAutocompleteSuggestions(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>();
        }

        String apiKey = BuildConfig.MAPTILER_API_KEY;
        String url = "https://api.maptiler.com/geocoding/" + query + ".json?key=" + apiKey + "&limit=5&autocomplete=true";

        Request request = new Request.Builder().url(url).build();
        List<String> suggestions = new ArrayList<>();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                JsonArray features = jsonObject.getAsJsonArray("features");

                for (int i = 0; i < features.size(); i++) {
                    JsonObject feature = features.get(i).getAsJsonObject();
                    String placeName = feature.get("place_name").getAsString();
                    suggestions.add(placeName);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching autocomplete suggestions", e);
        }

        return suggestions;
    }

    @Override
    public LatLng getLocationFromAddress(String address) {
        // Implement if needed for direct geocoding
        return null;
    }
}
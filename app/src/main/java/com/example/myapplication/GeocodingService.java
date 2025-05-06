// GeocodingService.java
package com.example.myapplication;

import org.maplibre.android.geometry.LatLng;

import java.util.List;

public interface GeocodingService {
    List<String> getAutocompleteSuggestions(String query);
    LatLng getLocationFromAddress(String address);
}
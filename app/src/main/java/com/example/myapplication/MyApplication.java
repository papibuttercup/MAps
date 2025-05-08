package com.example.myapplication;

import android.app.Application;
import org.maplibre.android.MapLibre;
import org.maplibre.android.WellKnownTileServer;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize MapLibre with MapLibre tile server
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre);
    }
} 
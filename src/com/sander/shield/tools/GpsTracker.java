package com.sander.shield.tools;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class GpsTracker {
    private LocationManager locationManager;
    private GpsCallback callback;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private LocationListener listener;

    public interface GpsCallback {
        void onLocation(String json);
        void onError(String error);
    }

    public void startTracking(Context ctx, GpsCallback cb) {
        this.callback = cb;
        this.locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            cb.onError("GPS Service not available");
            return;
        }

        isRunning = true;

        // Bikin listener yang beneran nangkep update dari satelit
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isRunning) {
                    String json = "{\"event\":\"gps_fix\",\"lat\":" + location.getLatitude() + 
                                  ",\"lon\":" + location.getLongitude() + 
                                  ",\"acc\":" + location.getAccuracy() + "}\n";
                    callback.onLocation(json);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            // Minta update beneran (setiap 5 detik atau tiap 10 meter)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, listener, Looper.getMainLooper());
            
            // Coba juga network provider (buat indoor)
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, listener, Looper.getMainLooper());
            } catch (Exception e) {}

            // Kasih feedback awal
            callback.onError("Mencari sinyal GPS (Minta update dari satelit)...");

        } catch (SecurityException e) {
            callback.onError("Izin GPS Ditolak! Pastikan 'Precise Location' aktif.");
            isRunning = false;
        }
    }

    public void stopTracking() {
        isRunning = false;
        if (locationManager != null && listener != null) {
            locationManager.removeUpdates(listener);
        }
        handler.removeCallbacksAndMessages(null);
    }
    
    public boolean isRunning() { return isRunning; }
}

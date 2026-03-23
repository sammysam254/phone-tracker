package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class LocationTracker implements LocationListener {
    
    private static final String TAG = "LocationTracker";
    private static final long LOCATION_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(2); // Every 2 minutes
    private static final float MIN_DISTANCE_CHANGE = 10; // 10 meters
    
    private Context context;
    private SupabaseClient supabaseClient;
    private LocationManager locationManager;
    private String deviceId;
    private Handler handler;
    private boolean isTracking = false;
    
    public LocationTracker(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void startTracking() {
        // Check if consent is granted and parent_id exists
        SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        
        if (!consentGranted || parentId == null) {
            Log.w(TAG, "Cannot start location tracking - consent not granted or no parent_id");
            return;
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted - cannot track location");
            return;
        }
        
        try {
            // Request location updates from both GPS and Network providers
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    MIN_DISTANCE_CHANGE,
                    this
                );
                Log.d(TAG, "GPS location tracking enabled");
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    MIN_DISTANCE_CHANGE,
                    this
                );
                Log.d(TAG, "Network location tracking enabled");
            }
            
            isTracking = true;
            
            // Get last known location immediately
            Location lastKnownLocation = getLastKnownLocation();
            if (lastKnownLocation != null) {
                onLocationChanged(lastKnownLocation);
                Log.d(TAG, "Initial location logged");
            }
            
            Log.i(TAG, "✅ Location tracking started successfully");
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception starting location tracking: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start location tracking: " + e.getMessage());
        }
    }
    
    public void stopTracking() {
        if (locationManager != null && isTracking) {
            try {
                locationManager.removeUpdates(this);
                isTracking = false;
                Log.i(TAG, "Location tracking stopped");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception stopping location tracking", e);
            }
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
    
    private Location getLastKnownLocation() {
        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (gpsLocation != null && networkLocation != null) {
                // Return the more recent location
                return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
            } else if (gpsLocation != null) {
                return gpsLocation;
            } else {
                return networkLocation;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting last known location", e);
            return null;
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        
        try {
            JSONObject locationData = new JSONObject();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("accuracy", location.getAccuracy());
            locationData.put("altitude", location.getAltitude());
            locationData.put("speed", location.getSpeed());
            locationData.put("bearing", location.getBearing());
            locationData.put("provider", location.getProvider());
            locationData.put("timestamp", location.getTime());
            
            // Add address information if available
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(context);
                java.util.List<android.location.Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);
                    locationData.put("address", address.getAddressLine(0));
                    locationData.put("city", address.getLocality());
                    locationData.put("country", address.getCountryName());
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not get address for location: " + e.getMessage());
            }
            
            // Log location to Supabase
            supabaseClient.logActivity(deviceId, "location", locationData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Location logged successfully");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log location: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing location update", e);
        }
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider " + provider + " status changed to: " + status);
    }
    
    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider " + provider + " enabled");
    }
    
    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider " + provider + " disabled");
    }
    
    public boolean isTracking() {
        return isTracking;
    }
}
package com.parentalcontrol.monitor;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import java.security.MessageDigest;
import java.util.UUID;

public class DeviceUtils {
    
    public static String getDeviceId(Context context) {
        try {
            // CRITICAL: Always check SharedPreferences FIRST to ensure consistency
            android.content.SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
            String storedId = prefs.getString("device_id", null);
            
            // If we already have a device ID, ALWAYS use it
            if (storedId != null && !storedId.isEmpty()) {
                return storedId;
            }
            
            // Generate new device ID only if none exists
            // Use Android ID as primary identifier
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String generatedId;
            
            if (androidId != null && !androidId.equals("9774d56d682e549c")) {
                generatedId = hashString(androidId);
            } else {
                // Fallback to random UUID
                String uuid = UUID.randomUUID().toString();
                generatedId = hashString(uuid);
            }
            
            // CRITICAL: Store the generated ID permanently
            prefs.edit().putString("device_id", generatedId).apply();
            
            return generatedId;
            
        } catch (Exception e) {
            // Final fallback - but try to retrieve stored ID first
            android.content.SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
            String storedId = prefs.getString("device_id", null);
            if (storedId != null) {
                return storedId;
            }
            
            // Last resort: generate and store
            String fallbackId = hashString(UUID.randomUUID().toString());
            prefs.edit().putString("device_id", fallbackId).apply();
            return fallbackId;
        }
    }
    
    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16); // Return first 16 characters
        } catch (Exception e) {
            return input.substring(0, Math.min(16, input.length()));
        }
    }
    
    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }
    
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }
    
    public static String getAndroidVersion() {
        return android.os.Build.VERSION.RELEASE;
    }
}
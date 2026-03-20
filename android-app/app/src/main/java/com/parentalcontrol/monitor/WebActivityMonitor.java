package com.parentalcontrol.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WebActivityMonitor {
    
    private static final String TAG = "WebActivityMonitor";
    private static final long MONITORING_INTERVAL = TimeUnit.SECONDS.toMillis(10); // Check every 10 seconds
    
    private Context context;
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable monitoringRunnable;
    private String deviceId;
    private Set<String> processedUrls;
    private long lastCheckTime;
    
    public WebActivityMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.processedUrls = new HashSet<>();
        this.lastCheckTime = System.currentTimeMillis();
    }
    
    public void startMonitoring() {
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                checkBrowserHistory();
                handler.postDelayed(this, MONITORING_INTERVAL);
            }
        };
        
        handler.post(monitoringRunnable);
        Log.i(TAG, "Web activity monitoring started");
    }
    
    public void stopMonitoring() {
        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "Web activity monitoring stopped");
    }
    
    private void checkBrowserHistory() {
        // Check if consent is granted
        SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return;
        }
        
        try {
            // Check Chrome browser history
            checkChromeHistory();
            
            // Check default browser history
            checkDefaultBrowserHistory();
            
            // Update last check time
            lastCheckTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking browser history", e);
        }
    }
    
    private void checkChromeHistory() {
        try {
            Uri uri = Uri.parse("content://com.android.chrome.browser/bookmarks");
            Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{"url", "title", "date"},
                "date > ?",
                new String[]{String.valueOf(lastCheckTime)},
                "date DESC"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String url = cursor.getString(0);
                    String title = cursor.getString(1);
                    long date = cursor.getLong(2);
                    
                    if (url != null && !processedUrls.contains(url + date)) {
                        logWebActivity(url, title, date, "Chrome");
                        processedUrls.add(url + date);
                    }
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Chrome history not accessible: " + e.getMessage());
        }
    }
    
    private void checkDefaultBrowserHistory() {
        try {
            // Note: Browser.BOOKMARKS_URI is deprecated and may not work on newer Android versions
            // This is kept for compatibility with older devices
            Uri bookmarksUri = Uri.parse("content://browser/bookmarks");
            
            Cursor cursor = context.getContentResolver().query(
                bookmarksUri,
                new String[]{"url", "title", "date"},
                "date > ?",
                new String[]{String.valueOf(lastCheckTime)},
                "date DESC"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String url = cursor.getString(0);
                    String title = cursor.getString(1);
                    long date = cursor.getLong(2);
                    
                    if (url != null && !processedUrls.contains(url + date)) {
                        logWebActivity(url, title, date, "Default Browser");
                        processedUrls.add(url + date);
                    }
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Default browser history not accessible: " + e.getMessage());
        }
    }
    
    private void logWebActivity(String url, String title, long date, String browser) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("url", url);
            activityData.put("title", title != null ? title : "");
            activityData.put("visitTime", date);
            activityData.put("browser", browser);
            activityData.put("timestamp", System.currentTimeMillis());
            
            // Extract domain for categorization
            try {
                Uri uri = Uri.parse(url);
                String domain = uri.getHost();
                activityData.put("domain", domain != null ? domain : "");
            } catch (Exception e) {
                activityData.put("domain", "");
            }
            
            supabaseClient.logActivity(deviceId, "web_activity", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Web activity logged: " + url);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log web activity: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging web activity", e);
        }
    }
}
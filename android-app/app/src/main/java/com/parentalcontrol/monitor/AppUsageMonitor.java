package com.parentalcontrol.monitor;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AppUsageMonitor {
    
    private static final String TAG = "AppUsageMonitor";
    private static final long MONITORING_INTERVAL = TimeUnit.MINUTES.toMillis(5); // Check every 5 minutes
    
    private Context context;
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable monitoringRunnable;
    private String deviceId;
    private UsageStatsManager usageStatsManager;
    
    public AppUsageMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }
    
    public void startMonitoring() {
        // Check if consent is granted and parent_id exists
        android.content.SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        
        if (!consentGranted || parentId == null) {
            Log.w(TAG, "Cannot start app usage monitoring - consent not granted or no parent_id");
            return;
        }
        
        if (usageStatsManager == null) {
            Log.w(TAG, "UsageStatsManager not available - cannot monitor app usage");
            return;
        }
        
        try {
            monitoringRunnable = new Runnable() {
                @Override
                public void run() {
                    collectAppUsageStats();
                    handler.postDelayed(this, MONITORING_INTERVAL);
                }
            };
            
            handler.post(monitoringRunnable);
            Log.i(TAG, "✅ App usage monitoring started successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start app usage monitoring: " + e.getMessage());
        }
    }
    
    public void stopMonitoring() {
        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "App usage monitoring stopped");
    }
    
    private void collectAppUsageStats() {
        try {
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR_OF_DAY, -1); // Last hour
            long startTime = calendar.getTimeInMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, 
                startTime, 
                endTime
            );
            
            if (usageStatsList != null && !usageStatsList.isEmpty()) {
                for (UsageStats usageStats : usageStatsList) {
                    if (usageStats.getTotalTimeInForeground() > 0) {
                        logAppUsage(usageStats);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting app usage stats", e);
        }
    }
    
    private void logAppUsage(UsageStats usageStats) {
        try {
            String packageName = usageStats.getPackageName();
            String appName = getAppName(packageName);
            
            JSONObject activityData = new JSONObject();
            activityData.put("packageName", packageName);
            activityData.put("appName", appName);
            activityData.put("totalTimeInForeground", usageStats.getTotalTimeInForeground());
            activityData.put("firstTimeStamp", usageStats.getFirstTimeStamp());
            activityData.put("lastTimeStamp", usageStats.getLastTimeStamp());
            activityData.put("lastTimeUsed", usageStats.getLastTimeUsed());
            
            supabaseClient.logActivity(deviceId, "app_usage", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "App usage logged for: " + appName);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log app usage: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging app usage", e);
        }
    }
    
    private String getAppName(String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName; // Return package name if app name not found
        }
    }
}
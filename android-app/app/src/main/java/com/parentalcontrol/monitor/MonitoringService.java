package com.parentalcontrol.monitor;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class MonitoringService extends Service {
    
    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "ParentalControlChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private CallLogMonitor callLogMonitor;
    private SmsMonitor smsMonitor;
    private AppUsageMonitor appUsageMonitor;
    private CameraMonitor cameraMonitor;
    private MicrophoneMonitor microphoneMonitor;
    private WebActivityMonitor webActivityMonitor;
    private LocationTracker locationTracker;
    private RemoteControlService remoteControlService;
    
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Check consent and parent_id before starting monitoring
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        boolean devicePaired = prefs.getBoolean("device_paired", false);
        String parentId = prefs.getString("parent_id", null);
        
        Log.i(TAG, "MonitoringService.onCreate() - Checking prerequisites:");
        Log.i(TAG, "  - Device paired: " + devicePaired);
        Log.i(TAG, "  - Consent granted: " + consentGranted);
        Log.i(TAG, "  - Parent ID: " + (parentId != null ? "Present" : "Missing"));
        
        if (!devicePaired) {
            Log.w(TAG, "Device not paired, stopping service");
            stopSelf();
            return;
        }
        
        if (parentId == null || parentId.isEmpty()) {
            Log.w(TAG, "No parent_id found, stopping service");
            stopSelf();
            return;
        }
        
        if (!consentGranted) {
            Log.w(TAG, "Consent not granted, stopping service");
            stopSelf();
            return;
        }
        
        Log.i(TAG, "All prerequisites met, initializing monitors");
        initializeMonitors();
        Log.i(TAG, "Monitoring service created successfully");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "🚀 MonitoringService.onStartCommand() called");
        
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Verify prerequisites again
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean devicePaired = prefs.getBoolean("device_paired", false);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        String deviceId = prefs.getString("device_id", null);
        
        Log.i(TAG, "📊 Service Prerequisites Check:");
        Log.i(TAG, "  - Device Paired: " + devicePaired);
        Log.i(TAG, "  - Consent Granted: " + consentGranted);
        Log.i(TAG, "  - Parent ID: " + (parentId != null ? parentId : "NULL"));
        Log.i(TAG, "  - Device ID: " + (deviceId != null ? deviceId : "NULL"));
        
        if (!devicePaired || !consentGranted || parentId == null || deviceId == null) {
            Log.e(TAG, "❌ Prerequisites not met - stopping service");
            Log.e(TAG, "   Required: device_paired=true, consent_granted=true, parent_id!=null, device_id!=null");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        // Start monitoring components
        startAllMonitors();
        
        Log.i(TAG, "✅ Monitoring service started successfully");
        return START_STICKY; // Restart if killed by system
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Restart service when task is removed
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startForegroundService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop monitoring components
        stopAllMonitors();
        
        Log.i(TAG, "Monitoring service destroyed");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    private void initializeMonitors() {
        try {
            callLogMonitor = new CallLogMonitor(this);
            smsMonitor = new SmsMonitor(this);
            appUsageMonitor = new AppUsageMonitor(this);
            cameraMonitor = new CameraMonitor(this);
            microphoneMonitor = new MicrophoneMonitor(this);
            webActivityMonitor = new WebActivityMonitor(this);
            locationTracker = new LocationTracker(this);
            
            Log.i(TAG, "All monitors initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing monitors", e);
        }
    }
    
    private void startAllMonitors() {
        try {
            // Verify prerequisites before starting any monitors
            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
            boolean devicePaired = prefs.getBoolean("device_paired", false);
            boolean consentGranted = prefs.getBoolean("consent_granted", false);
            String parentId = prefs.getString("parent_id", null);
            
            if (!devicePaired || !consentGranted || parentId == null) {
                Log.e(TAG, "Cannot start monitors - prerequisites not met");
                stopSelf();
                return;
            }
            
            Log.i(TAG, "🚀 Starting all monitoring services with parent_id: " + parentId);
            
            // Start Call Log Monitor
            if (callLogMonitor != null) {
                try {
                    callLogMonitor.startMonitoring();
                    Log.d(TAG, "✅ Call log monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start call log monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ Call log monitor is null");
            }
            
            // Start SMS Monitor
            if (smsMonitor != null) {
                try {
                    smsMonitor.startMonitoring();
                    Log.d(TAG, "✅ SMS monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start SMS monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ SMS monitor is null");
            }
            
            // Start App Usage Monitor
            if (appUsageMonitor != null) {
                try {
                    appUsageMonitor.startMonitoring();
                    Log.d(TAG, "✅ App usage monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start app usage monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ App usage monitor is null");
            }
            
            // Start Camera Monitor
            if (cameraMonitor != null) {
                try {
                    cameraMonitor.startMonitoring();
                    Log.d(TAG, "✅ Camera monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start camera monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ Camera monitor is null");
            }
            
            // Start Microphone Monitor
            if (microphoneMonitor != null) {
                try {
                    microphoneMonitor.startMonitoring();
                    Log.d(TAG, "✅ Microphone monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start microphone monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ Microphone monitor is null");
            }
            
            // Start Web Activity Monitor
            if (webActivityMonitor != null) {
                try {
                    webActivityMonitor.startMonitoring();
                    Log.d(TAG, "✅ Web activity monitor started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start web activity monitor: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ Web activity monitor is null");
            }
            
            // Start Location Tracker
            if (locationTracker != null) {
                try {
                    locationTracker.startTracking();
                    Log.d(TAG, "✅ Location tracker started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start location tracker: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ Location tracker is null");
            }
            
            // Start Remote Control Service as foreground service
            try {
                Intent remoteControlIntent = new Intent(this, RemoteControlService.class);
                startForegroundService(remoteControlIntent);
                Log.d(TAG, "✅ Remote control service started successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start remote control service: " + e.getMessage());
            }
            
            // Check Notification Listener status
            try {
                String enabledListeners = android.provider.Settings.Secure.getString(
                    getContentResolver(), "enabled_notification_listeners");
                if (enabledListeners != null && enabledListeners.contains(getPackageName())) {
                    Log.d(TAG, "✅ Notification listener is enabled and active");
                } else {
                    Log.w(TAG, "⚠️ Notification listener not enabled - notifications won't be monitored");
                    Log.w(TAG, "   Please enable notification access in Settings > Apps > Special Access > Notification Access");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking notification listener: " + e.getMessage());
            }
            
            // Check Accessibility Service status
            try {
                String enabledServices = android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (enabledServices != null && enabledServices.contains(getPackageName())) {
                    Log.d(TAG, "✅ Accessibility service is enabled and active");
                } else {
                    Log.w(TAG, "⚠️ Accessibility service not enabled - keyboard monitoring won't work");
                    Log.w(TAG, "   Please enable accessibility service in Settings > Accessibility");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking accessibility service: " + e.getMessage());
            }
            
            // Log final status
            Log.i(TAG, "🎯 All monitoring services initialization completed");
            Log.i(TAG, "📊 Active monitors: Call Log, SMS, App Usage, Camera, Microphone, Web Activity, Location, Remote Control");
            Log.i(TAG, "🔗 Parent ID: " + parentId);
            Log.i(TAG, "📱 Device ID: " + DeviceUtils.getDeviceId(this));
            
            // Test database connection by logging a test activity
            testDatabaseConnection();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Critical error starting monitors", e);
        }
    }
    
    private void testDatabaseConnection() {
        try {
            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
            String parentId = prefs.getString("parent_id", null);
            String deviceId = DeviceUtils.getDeviceId(this);
            
            if (parentId != null && deviceId != null) {
                Log.i(TAG, "🧪 Testing database connection...");
                
                SupabaseClient testClient = new SupabaseClient(this);
                org.json.JSONObject testData = new org.json.JSONObject();
                testData.put("test", "monitoring_service_started");
                testData.put("timestamp", System.currentTimeMillis());
                testData.put("service_version", "2.0.2");
                
                testClient.logActivity(deviceId, "app_usage", testData, new SupabaseClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Log.i(TAG, "✅ Database connection test SUCCESSFUL");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Database connection test FAILED: " + error);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Database connection test error: " + e.getMessage());
        }
    }
    
    private void stopAllMonitors() {
        try {
            if (callLogMonitor != null) {
                callLogMonitor.stopMonitoring();
                callLogMonitor = null;
            }
            
            if (smsMonitor != null) {
                smsMonitor.stopMonitoring();
                smsMonitor = null;
            }
            
            if (appUsageMonitor != null) {
                appUsageMonitor.stopMonitoring();
                appUsageMonitor = null;
            }
            
            if (cameraMonitor != null) {
                cameraMonitor.stopMonitoring();
                cameraMonitor = null;
            }
            
            if (microphoneMonitor != null) {
                microphoneMonitor.stopMonitoring();
                microphoneMonitor = null;
            }
            
            if (webActivityMonitor != null) {
                webActivityMonitor.stopMonitoring();
                webActivityMonitor = null;
            }
            
            if (locationTracker != null) {
                locationTracker.stopTracking();
                locationTracker = null;
            }
            
            // Stop remote control service
            Intent remoteControlIntent = new Intent(this, RemoteControlService.class);
            stopService(remoteControlIntent);
            
            Log.i(TAG, "All monitors stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping monitors", e);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Parental Control Monitoring",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitoring device activities for parental control");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Parental Control Active")
            .setContentText("Monitoring device activities with consent")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
}
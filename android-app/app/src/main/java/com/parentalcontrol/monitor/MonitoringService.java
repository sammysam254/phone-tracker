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
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start monitoring components
        startAllMonitors();
        
        Log.i(TAG, "Monitoring service started");
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
            if (callLogMonitor != null) {
                callLogMonitor.startMonitoring();
                Log.d(TAG, "Call log monitor started");
            }
            
            if (smsMonitor != null) {
                smsMonitor.startMonitoring();
                Log.d(TAG, "SMS monitor started");
            }
            
            if (appUsageMonitor != null) {
                appUsageMonitor.startMonitoring();
                Log.d(TAG, "App usage monitor started");
            }
            
            if (cameraMonitor != null) {
                cameraMonitor.startMonitoring();
                Log.d(TAG, "Camera monitor started");
            }
            
            if (microphoneMonitor != null) {
                microphoneMonitor.startMonitoring();
                Log.d(TAG, "Microphone monitor started");
            }
            
            if (webActivityMonitor != null) {
                webActivityMonitor.startMonitoring();
                Log.d(TAG, "Web activity monitor started");
            }
            
            if (locationTracker != null) {
                locationTracker.startTracking();
                Log.d(TAG, "Location tracker started");
            }
            
            // Start remote control service as foreground service
            Intent remoteControlIntent = new Intent(this, RemoteControlService.class);
            startForegroundService(remoteControlIntent);
            Log.d(TAG, "Remote control service started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting monitors", e);
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
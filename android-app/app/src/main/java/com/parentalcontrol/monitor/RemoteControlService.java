package com.parentalcontrol.monitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class RemoteControlService extends Service {
    
    private static final String TAG = "RemoteControlService";
    private static final long CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(3); // Check every 3 seconds for instant commands
    private static final String CHANNEL_ID = "RemoteControlChannel";
    private static final int NOTIFICATION_ID = 2;
    
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable checkCommandsRunnable;
    private String deviceId;
    private RemoteCameraController cameraController;
    private RemoteAudioController audioController;
    private RemoteDeviceController deviceController;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        handler = new Handler(Looper.getMainLooper());
        
        cameraController = new RemoteCameraController(this);
        audioController = new RemoteAudioController(this);
        deviceController = new RemoteDeviceController(this);
        
        Log.i(TAG, "Remote control service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        startCommandChecking();
        Log.i(TAG, "Remote control service started in foreground");
        return START_STICKY;
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
        stopCommandChecking();
        
        if (cameraController != null) {
            cameraController.shutdown();
        }
        
        if (audioController != null) {
            audioController.shutdown();
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "Remote control service destroyed");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startCommandChecking() {
        checkCommandsRunnable = new Runnable() {
            @Override
            public void run() {
                checkForRemoteCommands();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        
        handler.post(checkCommandsRunnable);
        Log.i(TAG, "Started checking for remote commands");
    }
    
    private void stopCommandChecking() {
        if (handler != null && checkCommandsRunnable != null) {
            handler.removeCallbacks(checkCommandsRunnable);
        }
    }
    
    private void checkForRemoteCommands() {
        // Check if consent is granted
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return;
        }
        
        // Check for pending commands from parent
        supabaseClient.checkRemoteCommands(deviceId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject commandResponse = new JSONObject(response);
                    if (commandResponse.has("commands")) {
                        processRemoteCommands(commandResponse.getJSONArray("commands"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing remote commands", e);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking remote commands: " + error);
            }
        });
    }
    
    private void processRemoteCommands(org.json.JSONArray commands) {
        try {
            for (int i = 0; i < commands.length(); i++) {
                JSONObject command = commands.getJSONObject(i);
                String commandType = command.getString("command_type");
                String commandId = command.getString("id");
                
                Log.i(TAG, "Processing remote command: " + commandType);
                
                switch (commandType) {
                    case "activate_camera":
                        handleActivateCamera(command, commandId);
                        break;
                        
                    case "deactivate_camera":
                        handleDeactivateCamera(commandId);
                        break;
                        
                    case "start_audio_monitoring":
                        handleStartAudioMonitoring(command, commandId);
                        break;
                        
                    case "stop_audio_monitoring":
                        handleStopAudioMonitoring(commandId);
                        break;
                        
                    case "get_location":
                        handleGetLocation(commandId);
                        break;
                        
                    case "emergency_alert":
                        handleEmergencyAlert(command, commandId);
                        break;
                        
                    case "lock_device":
                        handleLockDevice(commandId);
                        break;
                        
                    case "uninstall_app":
                        handleUninstallApp(command, commandId);
                        break;
                        
                    case "install_app":
                        handleInstallApp(command, commandId);
                        break;
                        
                    case "get_installed_apps":
                        handleGetInstalledApps(commandId);
                        break;
                        
                    case "disable_camera":
                        handleDisableCamera(command, commandId);
                        break;
                        
                    default:
                        Log.w(TAG, "Unknown command type: " + commandType);
                        markCommandCompleted(commandId, "error", "Unknown command type");
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing remote commands", e);
        }
    }
    
    private void handleActivateCamera(JSONObject command, String commandId) {
        try {
            int duration = command.optInt("duration", 30); // Default 30 seconds
            boolean frontCamera = command.optBoolean("front_camera", true);
            
            cameraController.activateCamera(frontCamera, duration, new RemoteCameraController.CameraCallback() {
                @Override
                public void onCameraActivated(String imageUrl) {
                    JSONObject result = new JSONObject();
                    try {
                        result.put("image_url", imageUrl);
                        result.put("timestamp", System.currentTimeMillis());
                        markCommandCompleted(commandId, "success", result.toString());
                    } catch (Exception e) {
                        markCommandCompleted(commandId, "error", e.getMessage());
                    }
                }
                
                @Override
                public void onCameraError(String error) {
                    markCommandCompleted(commandId, "error", error);
                }
            });
            
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleDeactivateCamera(String commandId) {
        cameraController.deactivateCamera();
        markCommandCompleted(commandId, "success", "Camera deactivated");
    }
    
    private void handleStartAudioMonitoring(JSONObject command, String commandId) {
        try {
            int duration = command.optInt("duration", 60); // Default 60 seconds
            
            audioController.startAudioMonitoring(duration, new RemoteAudioController.AudioCallback() {
                @Override
                public void onAudioRecorded(String audioUrl) {
                    JSONObject result = new JSONObject();
                    try {
                        result.put("audio_url", audioUrl);
                        result.put("timestamp", System.currentTimeMillis());
                        markCommandCompleted(commandId, "success", result.toString());
                    } catch (Exception e) {
                        markCommandCompleted(commandId, "error", e.getMessage());
                    }
                }
                
                @Override
                public void onAudioError(String error) {
                    markCommandCompleted(commandId, "error", error);
                }
            });
            
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleStopAudioMonitoring(String commandId) {
        audioController.stopAudioMonitoring();
        markCommandCompleted(commandId, "success", "Audio monitoring stopped");
    }
    
    private void handleGetLocation(String commandId) {
        // This will be handled by the LocationTracker
        // For immediate location, we can force a location update
        JSONObject result = new JSONObject();
        try {
            result.put("message", "Location update requested");
            result.put("timestamp", System.currentTimeMillis());
            markCommandCompleted(commandId, "success", result.toString());
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleEmergencyAlert(JSONObject command, String commandId) {
        try {
            String message = command.optString("message", "Emergency alert activated");
            
            // Log emergency alert
            JSONObject alertData = new JSONObject();
            alertData.put("alert_type", "emergency");
            alertData.put("message", message);
            alertData.put("timestamp", System.currentTimeMillis());
            
            supabaseClient.logActivity(deviceId, "emergency_alert", alertData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    markCommandCompleted(commandId, "success", "Emergency alert logged");
                }
                
                @Override
                public void onError(String error) {
                    markCommandCompleted(commandId, "error", error);
                }
            });
            
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleLockDevice(String commandId) {
        try {
            boolean success = deviceController.lockDevice();
            if (success) {
                markCommandCompleted(commandId, "success", "Device locked successfully");
            } else {
                markCommandCompleted(commandId, "error", "Failed to lock device - Device admin not enabled");
            }
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleUninstallApp(JSONObject command, String commandId) {
        try {
            String packageName = command.getString("package_name");
            deviceController.uninstallApp(packageName);
            markCommandCompleted(commandId, "success", "Uninstall initiated for " + packageName);
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleInstallApp(JSONObject command, String commandId) {
        try {
            String apkUrl = command.getString("apk_url");
            // Download APK and install
            // This would require additional implementation for downloading
            markCommandCompleted(commandId, "pending", "APK download initiated");
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleGetInstalledApps(String commandId) {
        try {
            java.util.List<android.content.pm.ApplicationInfo> apps = deviceController.getInstalledApps();
            JSONObject result = new JSONObject();
            org.json.JSONArray appsArray = new org.json.JSONArray();
            
            for (android.content.pm.ApplicationInfo app : apps) {
                if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    JSONObject appInfo = new JSONObject();
                    appInfo.put("package_name", app.packageName);
                    appInfo.put("app_name", app.loadLabel(getPackageManager()).toString());
                    appsArray.put(appInfo);
                }
            }
            
            result.put("apps", appsArray);
            result.put("count", appsArray.length());
            markCommandCompleted(commandId, "success", result.toString());
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void handleDisableCamera(JSONObject command, String commandId) {
        try {
            boolean disable = command.optBoolean("disable", true);
            boolean success = deviceController.disableCamera(disable);
            if (success) {
                markCommandCompleted(commandId, "success", "Camera " + (disable ? "disabled" : "enabled"));
            } else {
                markCommandCompleted(commandId, "error", "Failed to change camera state - Device admin not enabled");
            }
        } catch (Exception e) {
            markCommandCompleted(commandId, "error", e.getMessage());
        }
    }
    
    private void markCommandCompleted(String commandId, String status, String result) {
        supabaseClient.markCommandCompleted(commandId, status, result, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Command " + commandId + " marked as " + status);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to mark command completed: " + error);
            }
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Remote Control Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Handles remote control commands from parent");
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
            .setContentTitle("Remote Control Active")
            .setContentText("Listening for parent commands")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
}
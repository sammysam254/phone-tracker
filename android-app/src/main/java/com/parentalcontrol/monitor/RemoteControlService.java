package com.parentalcontrol.monitor;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class RemoteControlService extends Service {
    
    private static final String TAG = "RemoteControlService";
    private static final long CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(10); // Check every 10 seconds
    
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable checkCommandsRunnable;
    private String deviceId;
    private RemoteCameraController cameraController;
    private RemoteAudioController audioController;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        handler = new Handler(Looper.getMainLooper());
        
        cameraController = new RemoteCameraController(this);
        audioController = new RemoteAudioController(this);
        
        Log.i(TAG, "Remote control service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startCommandChecking();
        return START_STICKY;
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
}
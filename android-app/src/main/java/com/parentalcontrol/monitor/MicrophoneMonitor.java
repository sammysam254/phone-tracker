package com.parentalcontrol.monitor;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class MicrophoneMonitor {
    
    private static final String TAG = "MicrophoneMonitor";
    private static final long MONITORING_INTERVAL = TimeUnit.SECONDS.toMillis(30); // Check every 30 seconds
    
    private Context context;
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable monitoringRunnable;
    private String deviceId;
    private AudioManager audioManager;
    private boolean wasInUse = false;
    
    public MicrophoneMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    public void startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted");
            return;
        }
        
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                checkMicrophoneUsage();
                handler.postDelayed(this, MONITORING_INTERVAL);
            }
        };
        
        handler.post(monitoringRunnable);
        Log.i(TAG, "Microphone monitoring started");
    }
    
    public void stopMonitoring() {
        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "Microphone monitoring stopped");
    }
    
    private void checkMicrophoneUsage() {
        try {
            boolean isInUse = isMicrophoneInUse();
            
            // Log when microphone usage state changes
            if (isInUse && !wasInUse) {
                logMicrophoneEvent("microphone_started");
                wasInUse = true;
            } else if (!isInUse && wasInUse) {
                logMicrophoneEvent("microphone_stopped");
                wasInUse = false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking microphone usage", e);
        }
    }
    
    private boolean isMicrophoneInUse() {
        try {
            // Check AppOps for microphone usage
            return checkAppOpsForMicrophone() || checkAudioManagerState();
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking microphone availability", e);
        }
        
        return false;
    }
    
    private boolean checkAppOpsForMicrophone() {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps != null) {
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_RECORD_AUDIO, 
                    android.os.Process.myUid(), context.getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking AppOps for microphone", e);
        }
        return false;
    }
    
    private boolean checkAudioManagerState() {
        try {
            if (audioManager != null) {
                // Check if audio is being recorded
                return audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION ||
                       audioManager.getMode() == AudioManager.MODE_IN_CALL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking audio manager state", e);
        }
        return false;
    }
    
    private void logMicrophoneEvent(String eventType) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("event", eventType);
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("audioMode", audioManager != null ? audioManager.getMode() : -1);
            activityData.put("deviceModel", DeviceUtils.getDeviceModel());
            
            supabaseClient.logActivity(deviceId, "mic", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Microphone event logged: " + eventType);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log microphone event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging microphone event", e);
        }
    }
}
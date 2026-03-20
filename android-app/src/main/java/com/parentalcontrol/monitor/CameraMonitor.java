package com.parentalcontrol.monitor;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class CameraMonitor {
    
    private static final String TAG = "CameraMonitor";
    private static final long MONITORING_INTERVAL = TimeUnit.SECONDS.toMillis(30); // Check every 30 seconds
    
    private Context context;
    private SupabaseClient supabaseClient;
    private Handler handler;
    private Runnable monitoringRunnable;
    private String deviceId;
    private CameraManager cameraManager;
    private boolean wasInUse = false;
    
    public CameraMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }
    
    public void startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted");
            return;
        }
        
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                checkCameraUsage();
                handler.postDelayed(this, MONITORING_INTERVAL);
            }
        };
        
        handler.post(monitoringRunnable);
        Log.i(TAG, "Camera monitoring started");
    }
    
    public void stopMonitoring() {
        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "Camera monitoring stopped");
    }
    
    private void checkCameraUsage() {
        try {
            boolean isInUse = isCameraInUse();
            
            // Log when camera usage state changes
            if (isInUse && !wasInUse) {
                logCameraEvent("camera_started");
                wasInUse = true;
            } else if (!isInUse && wasInUse) {
                logCameraEvent("camera_stopped");
                wasInUse = false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera usage", e);
        }
    }
    
    private boolean isCameraInUse() {
        try {
            if (cameraManager == null) {
                return false;
            }
            
            // Check if any camera is currently in use
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                // This is a simplified check - in practice, you might need to use
                // CameraManager.AvailabilityCallback for more accurate detection
                try {
                    // If we can't open the camera, it might be in use
                    // Note: This is not a perfect method and may have false positives
                    return checkAppOpsForCamera();
                } catch (CameraAccessException e) {
                    // Camera might be in use by another app
                    return true;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera availability", e);
        }
        
        return false;
    }
    
    private boolean checkAppOpsForCamera() {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps != null) {
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_CAMERA, 
                    android.os.Process.myUid(), context.getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking AppOps for camera", e);
        }
        return false;
    }
    
    private void logCameraEvent(String eventType) {
        try {
            JSONObject activityData = new JSONObject();
            activityData.put("event", eventType);
            activityData.put("timestamp", System.currentTimeMillis());
            activityData.put("deviceModel", DeviceUtils.getDeviceModel());
            
            supabaseClient.logActivity(deviceId, "camera", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Camera event logged: " + eventType);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log camera event: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging camera event", e);
        }
    }
}
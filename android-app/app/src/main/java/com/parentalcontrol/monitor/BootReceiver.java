package com.parentalcontrol.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            
            Log.i(TAG, "Device boot completed or app updated");
            
            // Check if consent was previously granted
            SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
            boolean consentGranted = prefs.getBoolean("consent_granted", false);
            
            if (consentGranted) {
                // Auto-start monitoring service if consent was granted
                Intent serviceIntent = new Intent(context, MonitoringService.class);
                context.startForegroundService(serviceIntent);
                
                // Also start remote control service
                Intent remoteControlIntent = new Intent(context, RemoteControlService.class);
                context.startForegroundService(remoteControlIntent);
                
                Log.i(TAG, "Monitoring and remote control services auto-started after boot");
            } else {
                Log.i(TAG, "Consent not granted, services not started");
            }
        }
    }
}
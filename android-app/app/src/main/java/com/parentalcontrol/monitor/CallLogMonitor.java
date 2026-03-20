package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

public class CallLogMonitor {
    
    private static final String TAG = "CallLogMonitor";
    private Context context;
    private SupabaseClient supabaseClient;
    private CallLogObserver callLogObserver;
    private String deviceId;
    
    public CallLogMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
    }
    
    public void startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Call log permission not granted");
            return;
        }
        
        callLogObserver = new CallLogObserver(new Handler(Looper.getMainLooper()));
        context.getContentResolver().registerContentObserver(
            CallLog.Calls.CONTENT_URI, 
            true, 
            callLogObserver
        );
        
        Log.i(TAG, "Call log monitoring started");
    }
    
    public void stopMonitoring() {
        if (callLogObserver != null) {
            context.getContentResolver().unregisterContentObserver(callLogObserver);
            callLogObserver = null;
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "Call log monitoring stopped");
    }
    
    private class CallLogObserver extends ContentObserver {
        
        public CallLogObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            
            // Get the most recent call log entry
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                },
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 1"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                try {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    
                    // Create activity data
                    JSONObject activityData = new JSONObject();
                    activityData.put("number", number != null ? number : "Unknown");
                    activityData.put("type", getCallTypeString(type));
                    activityData.put("date", date);
                    activityData.put("duration", duration);
                    
                    // Log to Supabase
                    supabaseClient.logActivity(deviceId, "call", activityData, new SupabaseClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d(TAG, "Call activity logged successfully");
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to log call activity: " + error);
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing call log entry", e);
                } finally {
                    cursor.close();
                }
            }
        }
    }
    
    private String getCallTypeString(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "missed";
            default:
                return "unknown";
        }
    }
}
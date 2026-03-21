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
import android.provider.ContactsContract;
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
                    CallLog.Calls.CACHED_NAME,
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
                    // Get phone number - handle null/empty cases
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String cachedName = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    
                    // Handle different number scenarios
                    String displayNumber = number;
                    String contactName = cachedName;
                    
                    if (number == null || number.isEmpty() || number.equals("-1") || number.equals("-2")) {
                        // Private/blocked/unknown number
                        displayNumber = "Private Number";
                        contactName = "Hidden";
                    } else if (number.equals("-3")) {
                        displayNumber = "Payphone";
                        contactName = "Public Phone";
                    } else {
                        // Valid number - try to get contact name if not cached
                        if (contactName == null || contactName.isEmpty()) {
                            contactName = getContactName(number);
                        }
                    }
                    
                    // Create activity data - ALWAYS include both number and name
                    JSONObject activityData = new JSONObject();
                    activityData.put("number", displayNumber); // Always show the phone number
                    activityData.put("contact_name", contactName != null && !contactName.isEmpty() ? contactName : "Unknown"); // Show contact name or "Unknown"
                    activityData.put("display_text", contactName != null && !contactName.isEmpty() ? 
                        contactName + " (" + displayNumber + ")" : displayNumber); // Combined display: "John Doe (+1234567890)"
                    activityData.put("type", getCallTypeString(type));
                    activityData.put("date", date);
                    activityData.put("duration", duration);
                    
                    Log.d(TAG, "Call logged - Number: " + displayNumber + ", Contact: " + contactName + ", Type: " + getCallTypeString(type));
                    
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
    
    private String getContactName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        
        // Check if we have READ_CONTACTS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null,
                null
            );
            
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                        return name;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error looking up contact name", e);
        }
        
        return null;
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
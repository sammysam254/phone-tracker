package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

public class SmsMonitor {
    
    private static final String TAG = "SmsMonitor";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    
    private Context context;
    private SupabaseClient supabaseClient;
    private SmsReceiver smsReceiver;
    private SmsContentObserver smsContentObserver;
    private String deviceId;
    private long lastSmsTimestamp = 0;
    
    public SmsMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
    }
    
    public void startMonitoring() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permissions not granted");
            return;
        }
        
        // Register broadcast receiver for incoming SMS
        smsReceiver = new SmsReceiver();
        IntentFilter filter = new IntentFilter(SMS_RECEIVED);
        filter.setPriority(1000);
        context.registerReceiver(smsReceiver, filter);
        
        // Register content observer for all SMS (sent and received)
        smsContentObserver = new SmsContentObserver(new Handler(Looper.getMainLooper()));
        context.getContentResolver().registerContentObserver(
            Uri.parse("content://sms/"),
            true,
            smsContentObserver
        );
        
        Log.i(TAG, "SMS monitoring started (broadcast + content observer)");
    }
    
    public void stopMonitoring() {
        if (smsReceiver != null) {
            try {
                context.unregisterReceiver(smsReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
            smsReceiver = null;
        }
        
        if (smsContentObserver != null) {
            context.getContentResolver().unregisterContentObserver(smsContentObserver);
            smsContentObserver = null;
        }
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "SMS monitoring stopped");
    }
    
    // Broadcast receiver for incoming SMS (real-time)
    private class SmsReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SMS_RECEIVED.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    try {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        String format = bundle.getString("format");
                        
                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                                
                                if (smsMessage != null) {
                                    String sender = smsMessage.getOriginatingAddress();
                                    String messageBody = smsMessage.getMessageBody();
                                    long timestamp = smsMessage.getTimestampMillis();
                                    
                                    logSmsMessage(sender, messageBody, timestamp, "received");
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS broadcast", e);
                    }
                }
            }
        }
    }
    
    // Content observer for all SMS changes (sent and received)
    private class SmsContentObserver extends ContentObserver {
        
        public SmsContentObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            
            try {
                // Query the most recent SMS
                Cursor cursor = context.getContentResolver().query(
                    Uri.parse("content://sms/"),
                    new String[]{"_id", "address", "body", "date", "type"},
                    null,
                    null,
                    "date DESC LIMIT 1"
                );
                
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                        
                        // Avoid duplicate logging
                        if (date > lastSmsTimestamp) {
                            lastSmsTimestamp = date;
                            
                            String messageType;
                            switch (type) {
                                case 1: // Inbox (received)
                                    messageType = "received";
                                    break;
                                case 2: // Sent
                                    messageType = "sent";
                                    break;
                                case 3: // Draft
                                    messageType = "draft";
                                    break;
                                case 4: // Outbox
                                    messageType = "outbox";
                                    break;
                                case 5: // Failed
                                    messageType = "failed";
                                    break;
                                case 6: // Queued
                                    messageType = "queued";
                                    break;
                                default:
                                    messageType = "unknown";
                            }
                            
                            logSmsMessage(address, body, date, messageType);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing SMS content change", e);
            }
        }
    }
    
    private void logSmsMessage(String address, String body, long timestamp, String type) {
        try {
            // Create activity data
            JSONObject activityData = new JSONObject();
            activityData.put("address", address != null ? address : "Unknown");
            activityData.put("message", body != null ? body : "");
            activityData.put("timestamp", timestamp);
            activityData.put("type", type);
            
            Log.d(TAG, "Logging SMS - Type: " + type + ", Address: " + address + ", Length: " + (body != null ? body.length() : 0));
            
            // Log to Supabase
            supabaseClient.logActivity(deviceId, "sms", activityData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "SMS activity logged successfully");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log SMS activity: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging SMS message", e);
        }
    }
}
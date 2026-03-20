package com.parentalcontrol.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import org.json.JSONObject;

public class SmsMonitor {
    
    private static final String TAG = "SmsMonitor";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    
    private Context context;
    private SupabaseClient supabaseClient;
    private SmsReceiver smsReceiver;
    private String deviceId;
    
    public SmsMonitor(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
    }
    
    public void startMonitoring() {
        smsReceiver = new SmsReceiver();
        IntentFilter filter = new IntentFilter(SMS_RECEIVED);
        filter.setPriority(1000);
        context.registerReceiver(smsReceiver, filter);
        
        Log.i(TAG, "SMS monitoring started");
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
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
        
        Log.i(TAG, "SMS monitoring stopped");
    }
    
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
                                    
                                    // Create activity data
                                    JSONObject activityData = new JSONObject();
                                    activityData.put("sender", sender != null ? sender : "Unknown");
                                    activityData.put("message", messageBody != null ? messageBody : "");
                                    activityData.put("timestamp", timestamp);
                                    activityData.put("type", "received");
                                    
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
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS", e);
                    }
                }
            }
        }
    }
}
package com.parentalcontrol.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import org.json.JSONObject;

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if consent is granted
        SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return; // Don't process if consent not granted
        }
        
        if (SMS_RECEIVED.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    String format = bundle.getString("format");
                    
                    if (pdus != null) {
                        SupabaseClient supabaseClient = new SupabaseClient(context);
                        String deviceId = DeviceUtils.getDeviceId(context);
                        
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
                        
                        supabaseClient.shutdown();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing SMS", e);
                }
            }
        }
    }
}
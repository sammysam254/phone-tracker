package com.parentalcontrol.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONObject;

public class CallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "CallReceiver";
    private static CallAudioRecorder audioRecorder;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if consent is granted
        SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!consentGranted) {
            return;
        }
        
        String action = intent.getAction();
        
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            handlePhoneStateChange(context, state, phoneNumber);
        } else if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            handleOutgoingCall(context, phoneNumber);
        }
    }
    
    private void handlePhoneStateChange(Context context, String state, String phoneNumber) {
        try {
            SupabaseClient supabaseClient = new SupabaseClient(context);
            String deviceId = DeviceUtils.getDeviceId(context);
            
            JSONObject callData = new JSONObject();
            callData.put("phone_number", phoneNumber != null ? phoneNumber : "Unknown");
            callData.put("call_state", state);
            callData.put("timestamp", System.currentTimeMillis());
            callData.put("call_type", "incoming");
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                callData.put("event", "call_ringing");
                Log.i(TAG, "Incoming call ringing: " + phoneNumber);
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                callData.put("event", "call_answered");
                Log.i(TAG, "Call answered: " + phoneNumber);
                
                // Always start call recording when call is answered
                startCallRecording(context, phoneNumber);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                callData.put("event", "call_ended");
                Log.i(TAG, "Call ended: " + phoneNumber);
                
                // Always stop call recording when call ends
                stopCallRecording(context);
            }
            
            // Log call event
            supabaseClient.logActivity(deviceId, "call", callData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Call event logged: " + state);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log call event: " + error);
                }
            });
            
            supabaseClient.shutdown();
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling phone state change", e);
        }
    }
    
    private void handleOutgoingCall(Context context, String phoneNumber) {
        try {
            SupabaseClient supabaseClient = new SupabaseClient(context);
            String deviceId = DeviceUtils.getDeviceId(context);
            
            JSONObject callData = new JSONObject();
            callData.put("phone_number", phoneNumber != null ? phoneNumber : "Unknown");
            callData.put("call_state", "outgoing");
            callData.put("event", "call_initiated");
            callData.put("timestamp", System.currentTimeMillis());
            callData.put("call_type", "outgoing");
            
            // Log outgoing call
            supabaseClient.logActivity(deviceId, "call", callData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Outgoing call logged: " + phoneNumber);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to log outgoing call: " + error);
                }
            });
            
            supabaseClient.shutdown();
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling outgoing call", e);
        }
    }
    
    private void startCallRecording(Context context, String phoneNumber) {
        try {
            if (audioRecorder == null) {
                audioRecorder = new CallAudioRecorder(context);
            }
            
            audioRecorder.startRecording(phoneNumber);
            Log.i(TAG, "Call recording started for: " + phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting call recording", e);
        }
    }
    
    private void stopCallRecording(Context context) {
        try {
            if (audioRecorder != null) {
                audioRecorder.stopRecording();
                audioRecorder = null;
                Log.i(TAG, "Call recording stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping call recording", e);
        }
    }
}
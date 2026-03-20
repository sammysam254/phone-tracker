package com.parentalcontrol.monitor;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseClient {
    
    private static final String TAG = "SupabaseClient";
    private static final String SUPABASE_URL = "https://gejzprqznycnbfzeaxza.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdlanpwcnF6bnljbmJmemVheHphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5OTM2MTQsImV4cCI6MjA4OTU2OTYxNH0.zl9tfulUKL3aDbz4NjQOgOTk5JdMd8_Pf1YvHHN0SOQ";
    
    private ExecutorService executor;
    private Context context;
    
    public SupabaseClient(Context context) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(3);
    }
    
    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public void logActivity(String deviceId, String activityType, JSONObject data, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/activities");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);
                
                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("device_id", deviceId);
                requestBody.put("activity_type", activityType);
                requestBody.put("activity_data", data);
                requestBody.put("timestamp", java.time.Instant.now().toString());
                
                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes());
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    if (callback != null) {
                        callback.onSuccess("Activity logged successfully");
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode + ": " + response.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging activity", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void registerDeviceWithCode(JSONObject deviceData, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/device_pairing");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);
                
                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(deviceData.toString().getBytes());
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    if (callback != null) {
                        callback.onSuccess("Device registered with pairing code");
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode + ": " + response.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error registering device with code", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void checkPairingStatus(String deviceId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/devices?device_id=eq." + deviceId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onSuccess(response.toString());
                    }
                } else {
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode);
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking pairing status", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void updateDeviceConsent(String deviceId, boolean consentGranted, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/devices?device_id=eq." + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);
                
                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("consent_granted", consentGranted);
                if (consentGranted) {
                    requestBody.put("consent_timestamp", java.time.Instant.now().toString());
                }
                
                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes());
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    if (callback != null) {
                        callback.onSuccess("Consent updated successfully");
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode + ": " + response.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating consent", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void checkDeviceAuthorization(String deviceId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/devices?device_id=eq." + deviceId + "&select=consent_granted");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onSuccess(response.toString());
                    }
                } else {
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode);
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking authorization", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void checkRemoteCommands(String deviceId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/remote_commands?device_id=eq." + deviceId + "&status=eq.pending&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        JSONObject result = new JSONObject();
                        result.put("commands", new org.json.JSONArray(response.toString()));
                        callback.onSuccess(result.toString());
                    }
                } else {
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode);
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking remote commands", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void markCommandCompleted(String commandId, String status, String result, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/remote_commands?id=eq." + commandId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);
                
                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("status", status);
                requestBody.put("result", result);
                requestBody.put("completed_at", java.time.Instant.now().toString());
                
                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes());
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    if (callback != null) {
                        callback.onSuccess("Command marked as completed");
                    }
                } else {
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode);
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error marking command completed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void uploadImage(String filename, byte[] imageBytes, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/storage/v1/object/monitoring-images/" + filename);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setDoOutput(true);
                
                // Send image data
                OutputStream os = conn.getOutputStream();
                os.write(imageBytes);
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/monitoring-images/" + filename;
                    if (callback != null) {
                        callback.onSuccess(imageUrl);
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode + ": " + response.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading image", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void uploadAudio(String filename, byte[] audioBytes, ApiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/storage/v1/object/monitoring-audio/" + filename);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set headers
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "audio/3gpp");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setDoOutput(true);
                
                // Send audio data
                OutputStream os = conn.getOutputStream();
                os.write(audioBytes);
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    String audioUrl = SUPABASE_URL + "/storage/v1/object/public/monitoring-audio/" + filename;
                    if (callback != null) {
                        callback.onSuccess(audioUrl);
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("HTTP " + responseCode + ": " + response.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading audio", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
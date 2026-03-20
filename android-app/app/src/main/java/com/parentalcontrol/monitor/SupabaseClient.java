package com.parentalcontrol.monitor;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
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
    
    // ============================================================================
    // DEPRECATED METHODS - QR Code Pairing Only
    // ============================================================================
    // These methods are deprecated as of v1.2.1. The app now uses QR code pairing exclusively.
    // Kept for reference only - DO NOT USE in new code.
    // ============================================================================
    
    /*
    @Deprecated
    public void registerDeviceWithCode(JSONObject deviceData, ApiCallback callback) {
        // DEPRECATED: Use pairDeviceWithQR() instead
        // This method was used for the old pairing code system
        executor.execute(() -> {
            try {
                final String deviceId = deviceData.getString("device_id");
                
                // Add connection timeout and retry logic
                int maxRetries = 3;
                int retryCount = 0;
                Exception lastException = null;
                
                while (retryCount < maxRetries) {
                    try {
                        // First, check if device already exists and update it instead of creating new
                        URL checkUrl = new URL(SUPABASE_URL + "/rest/v1/device_pairing?device_id=eq." + deviceId);
                        HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
                        
                        // Set connection timeouts
                        checkConn.setConnectTimeout(10000); // 10 seconds
                        checkConn.setReadTimeout(15000); // 15 seconds
                        
                        // Set headers for check
                        checkConn.setRequestMethod("GET");
                        checkConn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                        checkConn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                        checkConn.setRequestProperty("Content-Type", "application/json");
                        
                        int checkResponseCode = checkConn.getResponseCode();
                        boolean deviceExists = false;
                        
                        if (checkResponseCode == 200) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(checkConn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            
                            JSONArray existingDevices = new JSONArray(response.toString());
                            deviceExists = existingDevices.length() > 0;
                        } else if (checkResponseCode == 404) {
                            // Table might not exist, continue with creation
                            deviceExists = false;
                        } else {
                            // Log the error but continue
                            Log.w(TAG, "Check request failed with code: " + checkResponseCode);
                        }
                        checkConn.disconnect();
                        
                        // Now either update existing device or create new one
                        URL url;
                        String method;
                        JSONObject requestData;
                        
                        if (deviceExists) {
                            // Update existing device with new pairing code
                            url = new URL(SUPABASE_URL + "/rest/v1/device_pairing?device_id=eq." + deviceId);
                            method = "PATCH";
                            
                            // Create update payload (only update necessary fields)
                            requestData = new JSONObject();
                            requestData.put("pairing_code", deviceData.getString("pairing_code"));
                            requestData.put("status", "waiting_for_parent");
                            requestData.put("expires_at", deviceData.getString("expires_at"));
                            requestData.put("updated_at", java.time.Instant.now().toString());
                        } else {
                            // Create new device record
                            url = new URL(SUPABASE_URL + "/rest/v1/device_pairing");
                            method = "POST";
                            requestData = new JSONObject(deviceData.toString()); // Create copy
                            requestData.put("created_at", java.time.Instant.now().toString());
                        }
                        
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        
                        // Set connection timeouts
                        conn.setConnectTimeout(10000); // 10 seconds
                        conn.setReadTimeout(15000); // 15 seconds
                        
                        // Set headers
                        conn.setRequestMethod(method);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Prefer", "return=representation");
                        conn.setDoOutput(true);
                        
                        // Send request
                        OutputStream os = conn.getOutputStream();
                        os.write(requestData.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        
                        int responseCode = conn.getResponseCode();
                        
                        if (responseCode >= 200 && responseCode < 300) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            
                            if (callback != null) {
                                String successMessage = deviceExists ? 
                                    "Device updated with new pairing code. Ready for re-pairing." : 
                                    "Device registered successfully. Waiting for parent connection.";
                                callback.onSuccess(successMessage);
                            }
                            return; // Success, exit retry loop
                        } else {
                            // Read error response
                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            
                            String errorMsg = "Registration failed (HTTP " + responseCode + "): " + response.toString();
                            Log.e(TAG, "Registration error: " + errorMsg);
                            
                            // If it's a client error (4xx), don't retry
                            if (responseCode >= 400 && responseCode < 500) {
                                if (callback != null) {
                                    callback.onError(errorMsg);
                                }
                                return;
                            } else {
                                // Server error (5xx), retry
                                lastException = new Exception(errorMsg);
                            }
                        }
                        
                        conn.disconnect();
                        
                    } catch (java.net.SocketTimeoutException e) {
                        Log.w(TAG, "Request timeout, retry " + (retryCount + 1) + "/" + maxRetries);
                        lastException = e;
                    } catch (java.net.UnknownHostException e) {
                        Log.e(TAG, "Network unavailable: " + e.getMessage());
                        if (callback != null) {
                            callback.onError("No internet connection. Please check your network and try again.");
                        }
                        return;
                    } catch (java.net.ConnectException e) {
                        Log.w(TAG, "Connection failed, retry " + (retryCount + 1) + "/" + maxRetries);
                        lastException = e;
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error during registration", e);
                        lastException = e;
                    }
                    
                    retryCount++;
                    
                    // Wait before retry (exponential backoff)
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000 * retryCount); // 1s, 2s, 3s delays
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                // All retries failed, use local fallback
                Log.i(TAG, "Supabase registration failed, using local fallback mode...");
                if (callback != null) {
                    // Generate success message for local mode
                    callback.onSuccess("Pairing code generated in offline mode. The code will be registered when connection is restored.");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error registering device with code", e);
                if (callback != null) {
                    // Even if registration fails, allow local pairing code generation
                    callback.onSuccess("Pairing code generated in offline mode. The code will be registered when connection is restored.");
                }
            }
        });
    }
    
    @Deprecated
    public void updatePairingCode(String deviceId, JSONObject updateData, ApiCallback callback) {
        // DEPRECATED: Use pairDeviceWithQR() instead
        // This method was used for the old pairing code system
        executor.execute(() -> {
            try {
                // Add connection timeout and retry logic
                int maxRetries = 3;
                int retryCount = 0;
                Exception lastException = null;
                
                while (retryCount < maxRetries) {
                    try {
                        // Only update the pairing code fields, don't create new entries
                        URL url = new URL(SUPABASE_URL + "/rest/v1/device_pairing?device_id=eq." + deviceId);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        
                        // Set connection timeouts
                        conn.setConnectTimeout(10000); // 10 seconds
                        conn.setReadTimeout(15000); // 15 seconds
                        
                        // Set headers for PATCH (update only)
                        conn.setRequestMethod("PATCH");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Prefer", "return=representation");
                        conn.setDoOutput(true);
                        
                        // Create minimal update payload (only pairing code related fields)
                        JSONObject requestData = new JSONObject();
                        requestData.put("pairing_code", updateData.getString("pairing_code"));
                        requestData.put("status", "waiting_for_parent"); // Reset status for new pairing
                        requestData.put("expires_at", updateData.getString("expires_at"));
                        requestData.put("updated_at", java.time.Instant.now().toString());
                        // Explicitly do NOT update device_name, device_brand, android_version, etc.
                        
                        // Send request
                        OutputStream os = conn.getOutputStream();
                        os.write(requestData.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        
                        int responseCode = conn.getResponseCode();
                        
                        if (responseCode >= 200 && responseCode < 300) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            
                            if (callback != null) {
                                callback.onSuccess("Pairing code updated successfully. Ready for re-pairing.");
                            }
                            return; // Success, exit retry loop
                        } else {
                            // Read error response
                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();
                            
                            String errorMsg = "Code update failed (HTTP " + responseCode + "): " + response.toString();
                            Log.e(TAG, "Code update error: " + errorMsg);
                            
                            // If it's a client error (4xx), don't retry
                            if (responseCode >= 400 && responseCode < 500) {
                                if (callback != null) {
                                    callback.onError(errorMsg);
                                }
                                return;
                            } else {
                                // Server error (5xx), retry
                                lastException = new Exception(errorMsg);
                            }
                        }
                        
                        conn.disconnect();
                        
                    } catch (java.net.SocketTimeoutException e) {
                        Log.w(TAG, "Code update timeout, retry " + (retryCount + 1) + "/" + maxRetries);
                        lastException = e;
                    } catch (java.net.UnknownHostException e) {
                        Log.e(TAG, "Network unavailable: " + e.getMessage());
                        if (callback != null) {
                            callback.onError("No internet connection. Please check your network and try again.");
                        }
                        return;
                    } catch (java.net.ConnectException e) {
                        Log.w(TAG, "Connection failed, retry " + (retryCount + 1) + "/" + maxRetries);
                        lastException = e;
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error during code update", e);
                        lastException = e;
                    }
                    
                    retryCount++;
                    
                    // Wait before retry (exponential backoff)
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000 * retryCount); // 1s, 2s, 3s delays
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                // All retries failed, use local fallback
                Log.i(TAG, "Supabase code update failed, using local fallback mode...");
                if (callback != null) {
                    // Generate success message for local mode
                    callback.onSuccess("Pairing code updated in offline mode. The code will be synced when connection is restored.");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating pairing code", e);
                if (callback != null) {
                    // Even if update fails, allow local pairing code generation
                    callback.onSuccess("Pairing code updated in offline mode. The code will be synced when connection is restored.");
                }
            }
        });
    }
    */
    
    public void checkPairingStatus(String deviceId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                // First check device_pairing table for pairing status
                URL url = new URL(SUPABASE_URL + "/rest/v1/device_pairing?device_id=eq." + deviceId + "&select=*");
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
                    
                    // Parse response to check if paired
                    try {
                        JSONArray pairingArray = new JSONArray(response.toString());
                        if (pairingArray.length() > 0) {
                            JSONObject pairingRecord = pairingArray.getJSONObject(0);
                            String status = pairingRecord.optString("status", "waiting_for_parent");
                            boolean isPaired = "paired".equals(status);
                            
                            JSONObject result = new JSONObject();
                            result.put("is_paired", isPaired);
                            result.put("status", status);
                            if (isPaired && pairingRecord.has("parent_id") && !pairingRecord.isNull("parent_id")) {
                                result.put("parent_name", "Parent"); // We'll get this from users table if needed
                            }
                            
                            if (callback != null) {
                                callback.onSuccess(result.toString());
                            }
                        } else {
                            // No pairing record found
                            JSONObject result = new JSONObject();
                            result.put("is_paired", false);
                            result.put("status", "not_found");
                            
                            if (callback != null) {
                                callback.onSuccess(result.toString());
                            }
                        }
                    } catch (JSONException e) {
                        if (callback != null) {
                            callback.onError("Failed to parse pairing status: " + e.getMessage());
                        }
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("Failed to check pairing status (HTTP " + responseCode + "): " + errorResponse.toString());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking pairing status", e);
                if (callback != null) {
                    callback.onError("Network error: " + e.getMessage());
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
    
    public void pairDeviceWithQR(JSONObject pairingData, ApiCallback callback) {
        executor.execute(() -> {
            try {
                final String deviceId = pairingData.getString("device_id");
                final String parentId = pairingData.getString("parent_id");
                final String pairingToken = pairingData.getString("pairing_token");
                
                // Verify the pairing token with the backend
                URL verifyUrl = new URL(SUPABASE_URL + "/rest/v1/rpc/verify_qr_pairing");
                HttpURLConnection verifyConn = (HttpURLConnection) verifyUrl.openConnection();
                
                verifyConn.setConnectTimeout(10000);
                verifyConn.setReadTimeout(15000);
                verifyConn.setRequestMethod("POST");
                verifyConn.setRequestProperty("Content-Type", "application/json");
                verifyConn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                verifyConn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                verifyConn.setRequestProperty("Prefer", "return=representation");
                verifyConn.setDoOutput(true);
                
                // Create verification payload
                JSONObject verifyPayload = new JSONObject();
                verifyPayload.put("p_parent_id", parentId);
                verifyPayload.put("p_pairing_token", pairingToken);
                verifyPayload.put("p_device_id", deviceId);
                verifyPayload.put("p_device_name", pairingData.getString("device_name"));
                verifyPayload.put("p_device_brand", pairingData.getString("device_brand"));
                verifyPayload.put("p_android_version", pairingData.getString("android_version"));
                
                OutputStream os = verifyConn.getOutputStream();
                os.write(verifyPayload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
                
                int responseCode = verifyConn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(verifyConn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    // Parse response to get parent name
                    JSONObject result = new JSONObject(response.toString());
                    
                    if (callback != null) {
                        callback.onSuccess(result.toString());
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        verifyConn.getErrorStream() != null ? verifyConn.getErrorStream() : verifyConn.getInputStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    br.close();
                    
                    if (callback != null) {
                        callback.onError("Pairing verification failed (HTTP " + responseCode + "): " + errorResponse.toString());
                    }
                }
                
                verifyConn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error pairing device with QR", e);
                if (callback != null) {
                    callback.onError("Network error: " + e.getMessage());
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
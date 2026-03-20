package com.parentalcontrol.monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.security.SecureRandom;

public class PairingActivity extends AppCompatActivity {
    
    private TextView pairingCodeText;
    private TextView instructionsText;
    private Button generateCodeButton;
    private Button checkPairingButton;
    private SupabaseClient supabaseClient;
    private String deviceId;
    private String currentPairingCode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        
        initViews();
        setupDevice();
        generatePairingCode();
        setupClickListeners();
    }
    
    private void initViews() {
        pairingCodeText = findViewById(R.id.pairingCodeText);
        instructionsText = findViewById(R.id.instructionsText);
        generateCodeButton = findViewById(R.id.generateCodeButton);
        checkPairingButton = findViewById(R.id.checkPairingButton);
    }
    
    private void setupDevice() {
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        
        String instructions = "Share this pairing code with your parent/guardian:\\n\\n" +
            "1. Parent opens the web dashboard\\n" +
            "2. Parent clicks 'Add Device'\\n" +
            "3. Parent enters this pairing code\\n" +
            "4. Click 'Check Pairing Status' below\\n\\n" +
            "Device ID: " + deviceId;
        
        instructionsText.setText(instructions);
    }
    
    private void generatePairingCode() {
        currentPairingCode = generateSecureCode();
        pairingCodeText.setText(currentPairingCode);
        
        // Register device with pairing code
        registerDeviceWithCode();
    }
    
    private String generateSecureCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        
        // Generate 6-digit code
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }
    
    private void registerDeviceWithCode() {
        try {
            JSONObject deviceData = new JSONObject();
            deviceData.put("device_id", deviceId);
            deviceData.put("pairing_code", currentPairingCode);
            deviceData.put("device_name", DeviceUtils.getDeviceModel());
            deviceData.put("device_brand", DeviceUtils.getDeviceBrand());
            deviceData.put("android_version", DeviceUtils.getAndroidVersion());
            deviceData.put("status", "waiting_for_parent");
            deviceData.put("expires_at", getExpirationTime());
            
            supabaseClient.registerDeviceWithCode(deviceData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        // Clear any previous pairing status
                        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("device_paired", false);
                        editor.remove("parent_name");
                        editor.apply();
                        
                        Toast.makeText(PairingActivity.this, "New pairing code generated. Ready for pairing.", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        String userFriendlyError = getUserFriendlyError(error);
                        
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PairingActivity.this);
                        builder.setTitle("❌ Registration Error")
                                .setMessage(userFriendlyError + "\n\nWould you like to try again?")
                                .setPositiveButton("Retry", (dialog, which) -> {
                                    // Generate new code and retry
                                    generatePairingCode();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
            });
            
        } catch (Exception e) {
            Toast.makeText(this, "Error generating pairing code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getExpirationTime() {
        // Set expiration to 24 hours from now
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(new java.util.Date(expirationTime));
    }
    
    private String getUserFriendlyError(String error) {
        if (error.contains("HTTP 400") || error.contains("pgrst204")) {
            return "Database connection issue. Please check your internet connection and try again.";
        } else if (error.contains("HTTP 401") || error.contains("unauthorized")) {
            return "Authentication error. Please restart the app and try again.";
        } else if (error.contains("HTTP 409") || error.contains("conflict") || error.contains("duplicate")) {
            return "Device already has a pairing code. Generating new code for re-pairing...";
        } else if (error.contains("Network error") || error.contains("timeout")) {
            return "Network connection problem. Please check your internet connection.";
        } else if (error.contains("JSON") || error.contains("parse")) {
            return "Data format error. Please try again.";
        } else {
            return "Registration failed: " + error + "\n\nPlease ensure you have a stable internet connection.";
        }
    }
    
    private void setupClickListeners() {
        generateCodeButton.setOnClickListener(v -> {
            // Show confirmation dialog for re-pairing
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("🔄 Generate New Pairing Code")
                    .setMessage("This will generate a new pairing code and allow re-pairing with a parent account.\n\nAny existing pairing will be reset. Continue?")
                    .setPositiveButton("Yes, Generate New Code", (dialog, which) -> {
                        generatePairingCode();
                        Toast.makeText(PairingActivity.this, "New pairing code generated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        
        checkPairingButton.setOnClickListener(v -> checkPairingStatus());
    }
    
    private void checkPairingStatus() {
        // Disable button during check to prevent multiple requests
        checkPairingButton.setEnabled(false);
        checkPairingButton.setText("Checking...");
        
        supabaseClient.checkPairingStatus(deviceId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject result = new JSONObject(response);
                    boolean isPaired = result.optBoolean("is_paired", false);
                    String parentName = result.optString("parent_name", "");
                    
                    runOnUiThread(() -> {
                        // Re-enable button
                        checkPairingButton.setEnabled(true);
                        checkPairingButton.setText("Check Pairing Status");
                        
                        if (isPaired) {
                            // Save pairing info
                            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("device_paired", true);
                            editor.putString("parent_name", parentName);
                            editor.apply();
                            
                            // Show success dialog
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PairingActivity.this);
                            builder.setTitle("✅ Pairing Successful!")
                                    .setMessage("Your device has been successfully paired with " + parentName + "'s account.\n\nYou will now proceed to the consent screen.")
                                    .setPositiveButton("Continue", (dialog, which) -> {
                                        Intent intent = new Intent(PairingActivity.this, ConsentActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            // Show waiting message with helpful tips
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PairingActivity.this);
                            builder.setTitle("⏳ Waiting for Parent")
                                    .setMessage("Your device is not paired yet. Please ensure:\n\n" +
                                            "• Your parent has opened the web dashboard\n" +
                                            "• They have entered the pairing code: " + currentPairingCode + "\n" +
                                            "• Both devices have internet connection\n\n" +
                                            "Try checking again in a few moments.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        checkPairingButton.setEnabled(true);
                        checkPairingButton.setText("Check Pairing Status");
                        Toast.makeText(PairingActivity.this, "Error checking pairing status. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    checkPairingButton.setEnabled(true);
                    checkPairingButton.setText("Check Pairing Status");
                    
                    // Show error dialog with retry option
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PairingActivity.this);
                    builder.setTitle("❌ Connection Error")
                            .setMessage("Unable to check pairing status:\n" + error + "\n\nPlease check your internet connection and try again.")
                            .setPositiveButton("Retry", (dialog, which) -> checkPairingStatus())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
}
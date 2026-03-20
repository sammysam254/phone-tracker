package com.parentalcontrol.monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConsentActivity extends AppCompatActivity {
    
    private TextView consentText;
    private Button acceptButton;
    private Button declineButton;
    private SupabaseClient supabaseClient;
    private String deviceId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);
        
        initViews();
        setupConsentText();
        setupClickListeners();
        
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
    }
    
    private void initViews() {
        consentText = findViewById(R.id.consentText);
        acceptButton = findViewById(R.id.acceptButton);
        declineButton = findViewById(R.id.declineButton);
    }
    
    private void setupConsentText() {
        String consent = "PARENTAL CONTROL CONSENT\\n\\n" +
            "This application will monitor the following activities on this device:\\n\\n" +
            "• Call logs (incoming/outgoing calls)\\n" +
            "• Text messages and chat applications\\n" +
            "• Camera usage and photo access\\n" +
            "• Microphone usage\\n" +
            "• App usage statistics\\n" +
            "• Location data (if enabled)\\n" +
            "• Screen time and device usage\\n\\n" +
            "DATA USAGE:\\n" +
            "• All data is encrypted and securely transmitted\\n" +
            "• Data is used solely for parental monitoring\\n" +
            "• No data is shared with third parties\\n" +
            "• You can revoke consent at any time\\n\\n" +
            "By accepting, you acknowledge that:\\n" +
            "• You are the legal guardian of this device user\\n" +
            "• You have informed the device user about this monitoring\\n" +
            "• You understand what data is being collected\\n" +
            "• You consent to this monitoring for parental purposes only\\n\\n" +
            "Device ID: " + deviceId;
            
        consentText.setText(consent);
    }
    
    private void setupClickListeners() {
        acceptButton.setOnClickListener(v -> {
            saveConsent(true);
            updateSupabaseConsent(true);
            Toast.makeText(this, "Consent granted", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        declineButton.setOnClickListener(v -> {
            saveConsent(false);
            updateSupabaseConsent(false);
            Toast.makeText(this, "Consent declined", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void saveConsent(boolean granted) {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("consent_granted", granted);
        editor.putLong("consent_timestamp", System.currentTimeMillis());
        editor.apply();
    }
    
    private void updateSupabaseConsent(boolean granted) {
        supabaseClient.updateDeviceConsent(deviceId, granted, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(ConsentActivity.this, "Consent status updated", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConsentActivity.this, "Failed to update consent: " + error, Toast.LENGTH_LONG).show();
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
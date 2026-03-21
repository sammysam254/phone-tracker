package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private TextView statusText;
    private TextView deviceIdText;
    private Button setupButton;
    private Button startButton;
    private String deviceId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupDeviceInfo();
        checkSetupStatus();
        setupClickListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkSetupStatus();
        updateMonitoringStatus();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        deviceIdText = findViewById(R.id.deviceIdText);
        setupButton = findViewById(R.id.setupButton);
        startButton = findViewById(R.id.startButton);
    }
    
    private void setupDeviceInfo() {
        deviceId = DeviceUtils.getDeviceId(this);
        deviceIdText.setText(deviceId);
    }
    
    private void setupClickListeners() {
        setupButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PermissionSetupActivity.class);
            startActivity(intent);
        });
        
        startButton.setOnClickListener(v -> {
            if (isFullySetup()) {
                if (MonitoringService.isServiceRunning(this)) {
                    stopMonitoringService();
                } else {
                    startMonitoringService();
                }
            } else {
                Toast.makeText(this, "Please complete setup first", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void checkSetupStatus() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean devicePaired = prefs.getBoolean("device_paired", false);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        
        if (!devicePaired) {
            statusText.setText("⚠️ Device not paired with parent. Please complete setup.");
            setupButton.setEnabled(true);
            setupButton.setText("Start Setup");
            startButton.setEnabled(false);
        } else if (!consentGranted) {
            statusText.setText("⚠️ Consent required. Please review and accept consent.");
            setupButton.setEnabled(true);
            setupButton.setText("Review Consent");
            startButton.setEnabled(false);
        } else if (!hasAllPermissions()) {
            statusText.setText("⚠️ Additional permissions required for full monitoring.");
            setupButton.setEnabled(true);
            setupButton.setText("Grant Permissions");
            startButton.setEnabled(false);
        } else {
            statusText.setText("✅ Setup complete. Ready for monitoring.");
            setupButton.setEnabled(false);
            startButton.setEnabled(true);
        }
    }
    
    private boolean isFullySetup() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        return prefs.getBoolean("device_paired", false) &&
               prefs.getBoolean("consent_granted", false) &&
               hasAllPermissions();
    }
    
    private boolean hasAllPermissions() {
        String[] requiredPermissions = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        };
        
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }
    
    private void updateMonitoringStatus() {
        if (MonitoringService.isServiceRunning(this)) {
            startButton.setText("Stop Monitoring");
            statusText.setText("🟢 Monitoring service is active");
        } else {
            startButton.setText("Start Monitoring");
        }
    }
    
    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        startForegroundService(serviceIntent);
        
        // Also ensure remote control service is running
        Intent remoteControlIntent = new Intent(this, RemoteControlService.class);
        startForegroundService(remoteControlIntent);
        
        Toast.makeText(this, "Comprehensive monitoring started", Toast.LENGTH_SHORT).show();
        updateMonitoringStatus();
    }
    
    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        stopService(serviceIntent);
        
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
        updateMonitoringStatus();
    }
}
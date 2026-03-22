package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1001;
    
    private TextView statusText;
    private TextView deviceIdText;
    private Button setupButton;
    private Button startButton;
    private String deviceId;
    private RemoteDeviceController deviceController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        deviceController = new RemoteDeviceController(this);
        
        initViews();
        setupDeviceInfo();
        checkSetupStatus();
        setupClickListeners();
        checkDeviceAdmin();
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
            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
            boolean devicePaired = prefs.getBoolean("device_paired", false);
            boolean consentGranted = prefs.getBoolean("consent_granted", false);
            
            Intent intent;
            if (!devicePaired) {
                // Not paired - go to permission setup first
                intent = new Intent(this, PermissionSetupActivity.class);
            } else if (!consentGranted) {
                // Paired but no consent - go to consent
                intent = new Intent(this, ConsentActivity.class);
            } else {
                // Already paired and consented - go to permission setup to review/grant more
                intent = new Intent(this, PermissionSetupActivity.class);
            }
            startActivity(intent);
        });
        
        // Add permission checker button
        Button checkPermissionsButton = findViewById(R.id.checkPermissionsButton);
        if (checkPermissionsButton != null) {
            checkPermissionsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, PermissionCheckerActivity.class);
                startActivity(intent);
            });
        }
        
        // Add device admin button
        Button enableDeviceAdminButton = findViewById(R.id.enableDeviceAdminButton);
        if (enableDeviceAdminButton != null) {
            enableDeviceAdminButton.setOnClickListener(v -> {
                if (deviceController.isDeviceAdminEnabled()) {
                    Toast.makeText(this, "✅ Device admin is already enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Enable Device Admin")
                        .setMessage("Device admin permission allows:\n\n• Remote device lock\n• Enhanced security features\n• Parental control functionality\n\nThis is required for full monitoring capabilities.")
                        .setPositiveButton("Enable", (dialog, which) -> {
                            boolean requested = deviceController.requestDeviceAdmin();
                            if (requested) {
                                Toast.makeText(this, "Please enable device admin on the next screen", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });
        }
        
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
        
        // Terms and Privacy links
        TextView termsLink = findViewById(R.id.termsLink);
        TextView privacyLink = findViewById(R.id.privacyLink);
        
        if (termsLink != null) {
            termsLink.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://your-domain.com/terms.html"));
                startActivity(browserIntent);
            });
        }
        
        if (privacyLink != null) {
            privacyLink.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://your-domain.com/privacy.html"));
                startActivity(browserIntent);
            });
        }
    }
    
    private void checkSetupStatus() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean devicePaired = prefs.getBoolean("device_paired", false);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        boolean deviceAdminEnabled = deviceController.isDeviceAdminEnabled();
        
        StringBuilder statusBuilder = new StringBuilder();
        
        if (!devicePaired) {
            statusBuilder.append("⚠️ Device not paired with parent. Tap 'Start Setup' to pair.");
            setupButton.setEnabled(true);
            setupButton.setText("Start Setup & Pair");
            startButton.setEnabled(false);
        } else if (!consentGranted) {
            statusBuilder.append("⚠️ Consent required. Please review and accept consent.");
            setupButton.setEnabled(true);
            setupButton.setText("Review Consent");
            startButton.setEnabled(false);
        } else if (!hasAllPermissions()) {
            statusBuilder.append("⚠️ Some permissions missing. Grant them for full monitoring.\n\n✅ You can still start monitoring with limited features.");
            setupButton.setEnabled(true);
            setupButton.setText("Grant More Permissions");
            startButton.setEnabled(true); // Allow starting even without all permissions
        } else {
            statusBuilder.append("✅ Setup complete. All permissions granted. Ready for full monitoring.");
            setupButton.setEnabled(true);
            setupButton.setText("Review Permissions");
            startButton.setEnabled(true);
        }
        
        // Add device admin status
        if (!deviceAdminEnabled) {
            statusBuilder.append("\n\n⚠️ Device Admin not enabled. Tap 'Enable Device Admin' button below for remote lock features.");
        } else {
            statusBuilder.append("\n\n✅ Device Admin enabled.");
        }
        
        statusText.setText(statusBuilder.toString());
    }
    
    private boolean isFullySetup() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        // Only require pairing and consent, not all permissions
        return prefs.getBoolean("device_paired", false) &&
               prefs.getBoolean("consent_granted", false);
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
    
    private void checkDeviceAdmin() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean deviceAdminPrompted = prefs.getBoolean("device_admin_prompted", false);
        
        // Only prompt once per app installation
        if (!deviceAdminPrompted && !deviceController.isDeviceAdminEnabled()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Device Admin Required")
                .setMessage("Device admin permission is required for remote lock and security features.\n\nThis is essential for parental control functionality.\n\nEnable it now?")
                .setPositiveButton("Enable Now", (dialog, which) -> {
                    boolean requested = deviceController.requestDeviceAdmin();
                    if (requested) {
                        Toast.makeText(this, "Please enable device admin on the next screen", Toast.LENGTH_LONG).show();
                    }
                    prefs.edit().putBoolean("device_admin_prompted", true).apply();
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    prefs.edit().putBoolean("device_admin_prompted", true).apply();
                    Toast.makeText(this, "You can enable device admin later from Settings", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "✅ Device admin enabled successfully!", Toast.LENGTH_LONG).show();
                Log.i("MainActivity", "Device admin enabled successfully");
            } else {
                Toast.makeText(this, "⚠️ Device admin was not enabled. Some features may not work.", Toast.LENGTH_LONG).show();
                Log.w("MainActivity", "Device admin not enabled by user");
            }
            checkSetupStatus();
        }
    }
}
package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
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
    private SupabaseClient supabaseClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        deviceController = new RemoteDeviceController(this);
        supabaseClient = new SupabaseClient(this);
        
        // Clear old pairing data for fresh start (v1.7.0)
        clearOldPairingData();
        
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
                // Not paired - go to login to bind device
                intent = new Intent(this, LoginActivity.class);
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
        
        // Add re-pair device button
        Button rePairDeviceButton = findViewById(R.id.rePairDeviceButton);
        if (rePairDeviceButton != null) {
            rePairDeviceButton.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Unbind Device")
                    .setMessage("This will:\n\n• Clear all local binding data\n• Clear all server data (activities, device records)\n• Stop monitoring service\n• Allow you to login with a different account\n\nAre you sure you want to continue?")
                    .setPositiveButton("Yes, Unbind", (dialog, which) -> {
                        rePairDevice();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        // Add diagnostic button
        Button diagnosticButton = findViewById(R.id.diagnosticButton);
        if (diagnosticButton != null) {
            diagnosticButton.setOnClickListener(v -> runDiagnostics());
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
            statusBuilder.append("⚠️ Device not bound to parent account. Tap 'Login' to bind.");
            setupButton.setEnabled(true);
            setupButton.setText("Login & Bind Device");
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
    
    /**
     * Re-pair device - clears all local and server data, then starts fresh pairing
     */
    private void rePairDevice() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        String parentId = prefs.getString("parent_id", null);
        
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Clearing pairing data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Stop monitoring service first
        stopMonitoringService();
        Intent remoteControlIntent = new Intent(this, RemoteControlService.class);
        stopService(remoteControlIntent);
        
        // If we have a parent_id, clear server data first
        if (parentId != null && !parentId.isEmpty()) {
            supabaseClient.clearDevicePairing(deviceId, parentId, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        Log.i("MainActivity", "Server data cleared: " + response);
                        clearLocalPairingData();
                        progressDialog.dismiss();
                        
                        Toast.makeText(MainActivity.this, "✅ Device unbound! Login again to bind...", Toast.LENGTH_LONG).show();
                        
                        // Navigate to login for fresh binding
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.w("MainActivity", "Server clear failed, clearing local data anyway: " + error);
                        clearLocalPairingData();
                        progressDialog.dismiss();
                        
                        Toast.makeText(MainActivity.this, "⚠️ Server clear failed, but local data cleared. Login again to bind...", Toast.LENGTH_LONG).show();
                        
                        // Navigate to login anyway
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
            });
        } else {
            // No parent_id, just clear local data
            clearLocalPairingData();
            progressDialog.dismiss();
            
            Toast.makeText(this, "✅ Local data cleared! Login again to bind...", Toast.LENGTH_SHORT).show();
            
            // Navigate to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    
    /**
     * Clear all local pairing data from SharedPreferences
     */
    private void clearLocalPairingData() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Clear all pairing and setup data
        editor.remove("device_paired");
        editor.remove("device_registered");
        editor.remove("consent_granted");
        editor.remove("parent_id");
        editor.remove("parent_name");
        editor.remove("device_admin_prompted");
        
        // Keep device_id - it should persist
        editor.apply();
        
        Log.i("MainActivity", "Local pairing data cleared");
    }
    
    /**
     * Clear old pairing data to force fresh pairing and permission setup
     * This ensures users re-scan QR code and grant all permissions again
     */
    private void clearOldPairingData() {
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        
        // Check if this is a fresh install or update that needs clearing
        int lastVersionCode = prefs.getInt("last_version_code", 0);
        int currentVersionCode = 21; // v1.8.3
        
        if (lastVersionCode < currentVersionCode) {
            Log.i("MainActivity", "Clearing old pairing data for v1.7.0 update");
            
            // Clear all pairing and setup data
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("device_paired");
            editor.remove("device_registered");
            editor.remove("consent_granted");
            editor.remove("parent_id");
            editor.remove("parent_name");
            
            // Keep device_id - it should persist
            // Update version code
            editor.putInt("last_version_code", currentVersionCode);
            editor.apply();
            
            Log.i("MainActivity", "Old pairing data cleared - user must re-pair and grant permissions");
        }
    }
    
    private void runDiagnostics() {
        Log.i("MainActivity", "🔧 Running comprehensive diagnostics...");
        
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("🔧 MONITORING DIAGNOSTICS\n\n");
        
        // Check SharedPreferences
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean devicePaired = prefs.getBoolean("device_paired", false);
        boolean consentGranted = prefs.getBoolean("consent_granted", false);
        String parentId = prefs.getString("parent_id", null);
        String deviceId = prefs.getString("device_id", null);
        
        diagnostics.append("📊 ACCOUNT BINDING STATUS:\n");
        diagnostics.append("• Device Paired: ").append(devicePaired ? "✅" : "❌").append("\n");
        diagnostics.append("• Consent Granted: ").append(consentGranted ? "✅" : "❌").append("\n");
        diagnostics.append("• Parent ID: ").append(parentId != null ? "✅ Present" : "❌ Missing").append("\n");
        diagnostics.append("• Device ID: ").append(deviceId != null ? "✅ Present" : "❌ Missing").append("\n\n");
        
        // Check Service Status
        boolean serviceRunning = MonitoringService.isServiceRunning(this);
        diagnostics.append("🚀 SERVICE STATUS:\n");
        diagnostics.append("• Monitoring Service: ").append(serviceRunning ? "✅ Running" : "❌ Stopped").append("\n\n");
        
        // Check Permissions
        diagnostics.append("🔒 PERMISSIONS:\n");
        diagnostics.append("• SMS: ").append(hasPermission(android.Manifest.permission.READ_SMS) ? "✅" : "❌").append("\n");
        diagnostics.append("• Phone: ").append(hasPermission(android.Manifest.permission.READ_PHONE_STATE) ? "✅" : "❌").append("\n");
        diagnostics.append("• Location: ").append(hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ? "✅" : "❌").append("\n");
        diagnostics.append("• Camera: ").append(hasPermission(android.Manifest.permission.CAMERA) ? "✅" : "❌").append("\n");
        diagnostics.append("• Microphone: ").append(hasPermission(android.Manifest.permission.RECORD_AUDIO) ? "✅" : "❌").append("\n");
        diagnostics.append("• Usage Stats: ").append(hasUsageStatsPermission() ? "✅" : "❌").append("\n\n");
        
        // Check Special Permissions
        diagnostics.append("🔧 SPECIAL PERMISSIONS:\n");
        diagnostics.append("• Device Admin: ").append(deviceController.isDeviceAdminEnabled() ? "✅" : "❌").append("\n");
        diagnostics.append("• Notification Access: ").append(isNotificationAccessEnabled() ? "✅" : "❌").append("\n");
        diagnostics.append("• Accessibility: ").append(isAccessibilityEnabled() ? "✅" : "❌").append("\n\n");
        
        // Test Database Connection
        diagnostics.append("🗄️ DATABASE TEST:\n");
        if (parentId != null && deviceId != null) {
            diagnostics.append("• Testing connection...\n");
            testDatabaseConnection(diagnostics);
        } else {
            diagnostics.append("• ❌ Cannot test - missing parent_id or device_id\n");
        }
        
        // Show diagnostics in dialog
        new android.app.AlertDialog.Builder(this)
            .setTitle("🔧 Monitoring Diagnostics")
            .setMessage(diagnostics.toString())
            .setPositiveButton("Copy to Clipboard", (dialog, which) -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Diagnostics", diagnostics.toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
    
    private boolean hasPermission(String permission) {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, permission) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean hasUsageStatsPermission() {
        try {
            android.app.usage.UsageStatsManager usageStatsManager = 
                (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            java.util.List<android.app.usage.UsageStats> stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time);
            return stats != null && !stats.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isNotificationAccessEnabled() {
        try {
            String enabledListeners = android.provider.Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
            return enabledListeners != null && enabledListeners.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isAccessibilityEnabled() {
        try {
            String enabledServices = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null && enabledServices.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
    
    private void testDatabaseConnection(StringBuilder diagnostics) {
        try {
            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
            String parentId = prefs.getString("parent_id", null);
            String deviceId = prefs.getString("device_id", null);
            
            SupabaseClient testClient = new SupabaseClient(this);
            org.json.JSONObject testData = new org.json.JSONObject();
            testData.put("test", "diagnostic_test");
            testData.put("timestamp", System.currentTimeMillis());
            
            testClient.logActivity(deviceId, "app_usage", testData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.i("MainActivity", "✅ Database test successful");
                }
                
                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "❌ Database test failed: " + error);
                }
            });
            
            diagnostics.append("• ✅ Test activity logged\n");
        } catch (Exception e) {
            diagnostics.append("• ❌ Test failed: ").append(e.getMessage()).append("\n");
        }
    }
}

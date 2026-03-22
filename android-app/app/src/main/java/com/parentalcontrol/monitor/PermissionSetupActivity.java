package com.parentalcontrol.monitor;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionSetupActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int USAGE_STATS_REQUEST_CODE = 101;
    private static final int NOTIFICATION_ACCESS_REQUEST_CODE = 102;
    private static final int ACCESSIBILITY_REQUEST_CODE = 103;
    private static final int OVERLAY_REQUEST_CODE = 104;
    private static final int STORAGE_REQUEST_CODE = 105;
    
    private LinearLayout permissionContainer;
    private Button continueButton;
    private Button skipButton;
    
    private String[] requiredPermissions = {
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);
        
        initViews();
        setupPermissionList();
        setupContinueButton();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }
    
    private void initViews() {
        permissionContainer = findViewById(R.id.permissionContainer);
        continueButton = findViewById(R.id.continueButton);
        skipButton = findViewById(R.id.skipButton);
    }
    
    private void setupPermissionList() {
        permissionContainer.removeAllViews();
        
        // Standard permissions
        for (String permission : requiredPermissions) {
            addPermissionItem(getPermissionName(permission), 
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED,
                () -> requestStandardPermissions());
        }
        
        // Special permissions
        addPermissionItem("Storage Access", 
            hasStoragePermission(), 
            this::requestStoragePermission);
            
        addPermissionItem("Usage Stats Access", 
            hasUsageStatsPermission(), 
            this::requestUsageStatsPermission);
            
        addPermissionItem("Notification Access", 
            hasNotificationAccess(), 
            this::requestNotificationAccess);
            
        addPermissionItem("Accessibility Service", 
            hasAccessibilityPermission(), 
            this::requestAccessibilityPermission);
            
        addPermissionItem("Screen Overlay", 
            hasOverlayPermission(), 
            this::requestOverlayPermission);
    }
    
    private void addPermissionItem(String name, boolean granted, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(16, 12, 16, 12);
        
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView statusView = new TextView(this);
        statusView.setText(granted ? "✅ Granted" : "❌ Required");
        statusView.setTextColor(granted ? 0xFF28a745 : 0xFFdc3545);
        
        Button actionButton = new Button(this);
        actionButton.setText(granted ? "Granted" : "Grant");
        actionButton.setEnabled(!granted);
        actionButton.setOnClickListener(v -> action.run());
        
        item.addView(nameView);
        item.addView(statusView);
        item.addView(actionButton);
        
        permissionContainer.addView(item);
    }
    
    private void setupContinueButton() {
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PairingActivity.class);
            startActivity(intent);
            finish();
        });
        
        // Add skip button functionality
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> {
                // Show warning dialog
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Skip Permissions?")
                    .setMessage("You can proceed to pairing without granting all permissions now.\n\n" +
                            "⚠️ Warning: Some monitoring features will not work until you grant the required permissions.\n\n" +
                            "You can grant permissions later from the main screen.")
                    .setPositiveButton("Skip & Continue", (dialog, which) -> {
                        Intent intent = new Intent(this, PairingActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Stay Here", null)
                    .show();
            });
        }
    }
    
    private void updatePermissionStatus() {
        setupPermissionList();
        
        // Always enable continue button - allow proceeding without all permissions
        continueButton.setEnabled(true);
        
        if (allPermissionsGranted()) {
            continueButton.setText("Continue to Pairing");
            if (skipButton != null) {
                skipButton.setVisibility(android.view.View.GONE);
            }
        } else {
            continueButton.setText("Continue Anyway");
            if (skipButton != null) {
                skipButton.setVisibility(android.view.View.VISIBLE);
                skipButton.setText("Skip All Permissions");
            }
        }
    }
    
    private boolean allPermissionsGranted() {
        // Check standard permissions
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // Check special permissions
        return hasStoragePermission() &&
               hasUsageStatsPermission() && 
               hasNotificationAccess() && 
               hasAccessibilityPermission() && 
               hasOverlayPermission();
    }
    
    private void requestStandardPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
    }
    
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
            android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, USAGE_STATS_REQUEST_CODE);
    }
    
    private boolean hasNotificationAccess() {
        ComponentName cn = new ComponentName(this, ParentalNotificationListenerService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }
    
    private void requestNotificationAccess() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivityForResult(intent, NOTIFICATION_ACCESS_REQUEST_CODE);
    }
    
    private boolean hasAccessibilityPermission() {
        ComponentName cn = new ComponentName(this, AccessibilityMonitorService.class);
        String flat = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return flat != null && flat.contains(cn.flattenToString());
    }
    
    private void requestAccessibilityPermission() {
        // Show detailed instructions before opening settings
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enable Accessibility Service")
                .setMessage("To monitor device activities, please:\n\n" +
                        "1. Find 'Parental Control Monitor' in the list\n" +
                        "2. Tap on it to open settings\n" +
                        "3. Toggle the switch to 'ON'\n" +
                        "4. Confirm by tapping 'OK' in the dialog\n" +
                        "5. Return to this app\n\n" +
                        "This permission is required for monitoring keyboard inputs and app activities.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
        }
    }
    
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses MANAGE_EXTERNAL_STORAGE
            return android.os.Environment.isExternalStorageManager();
        } else {
            // Android 10 and below use READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Storage Permission Required")
                    .setMessage("This app needs access to device storage to monitor media files and app data.\n\n" +
                            "Please:\n" +
                            "1. Find this app in the list\n" +
                            "2. Toggle 'Allow access to manage all files' to ON\n" +
                            "3. Return to this app\n\n" +
                            "This permission is required for comprehensive monitoring.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, STORAGE_REQUEST_CODE);
                        } catch (Exception e) {
                            // Fallback to general storage settings
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivityForResult(intent, STORAGE_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Android 10 and below - Request READ_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                STORAGE_REQUEST_CODE);
        }
    }
    
    private String getPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.READ_CALL_LOG:
                return "Call Log Access";
            case Manifest.permission.READ_SMS:
                return "SMS Read Access";
            case Manifest.permission.RECEIVE_SMS:
                return "SMS Receive Access";
            case Manifest.permission.RECORD_AUDIO:
                return "Microphone Access";
            case Manifest.permission.CAMERA:
                return "Camera Access";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Location Access";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Coarse Location";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Storage Access";
            case Manifest.permission.READ_CONTACTS:
                return "Contacts Access";
            case Manifest.permission.READ_PHONE_STATE:
                return "Phone State Access";
            default:
                return permission;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == STORAGE_REQUEST_CODE) {
            updatePermissionStatus();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updatePermissionStatus();
    }
}
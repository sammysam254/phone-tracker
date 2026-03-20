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
    
    private LinearLayout permissionContainer;
    private Button continueButton;
    
    private String[] requiredPermissions = {
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
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
            if (allPermissionsGranted()) {
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please grant all permissions to continue", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void updatePermissionStatus() {
        setupPermissionList();
        continueButton.setEnabled(allPermissionsGranted());
        continueButton.setText(allPermissionsGranted() ? "Continue to Pairing" : "Grant All Permissions");
    }
    
    private boolean allPermissionsGranted() {
        // Check standard permissions
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // Check special permissions
        return hasUsageStatsPermission() && 
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
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE);
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
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatus();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updatePermissionStatus();
    }
}
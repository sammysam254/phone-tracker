package com.parentalcontrol.monitor;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionCheckerActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private LinearLayout permissionsContainer;
    private Button grantAllButton;
    private Button finishButton;
    
    private static class PermissionInfo {
        String permission;
        String title;
        String description;
        boolean isSpecial; // Requires special handling (like accessibility)
        
        PermissionInfo(String permission, String title, String description, boolean isSpecial) {
            this.permission = permission;
            this.title = title;
            this.description = description;
            this.isSpecial = isSpecial;
        }
    }
    
    private PermissionInfo[] permissions = {
        new PermissionInfo(Manifest.permission.READ_CALL_LOG, "Call Logs", "Monitor incoming and outgoing calls", false),
        new PermissionInfo(Manifest.permission.READ_SMS, "Read SMS", "Read text messages", false),
        new PermissionInfo(Manifest.permission.RECEIVE_SMS, "Receive SMS", "Monitor incoming messages", false),
        new PermissionInfo(Manifest.permission.RECORD_AUDIO, "Microphone", "Record audio for remote monitoring", false),
        new PermissionInfo(Manifest.permission.CAMERA, "Camera", "Capture photos remotely", false),
        new PermissionInfo(Manifest.permission.ACCESS_FINE_LOCATION, "Location", "Track device location", false),
        new PermissionInfo(Manifest.permission.READ_CONTACTS, "Contacts", "Show contact names in call logs", false),
        new PermissionInfo("ACCESSIBILITY", "Accessibility Service", "Monitor keyboard input and screen interactions", true),
        new PermissionInfo("NOTIFICATION_LISTENER", "Notification Access", "Monitor app notifications", true),
        new PermissionInfo("USAGE_STATS", "Usage Access", "Track app usage statistics", true)
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_checker);
        
        permissionsContainer = findViewById(R.id.permissionsContainer);
        grantAllButton = findViewById(R.id.grantAllButton);
        finishButton = findViewById(R.id.finishButton);
        
        grantAllButton.setOnClickListener(v -> requestMissingPermissions());
        finishButton.setOnClickListener(v -> finish());
        
        refreshPermissionsList();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionsList();
    }
    
    private void refreshPermissionsList() {
        permissionsContainer.removeAllViews();
        
        int grantedCount = 0;
        int totalCount = permissions.length;
        
        for (PermissionInfo permInfo : permissions) {
            boolean isGranted = checkPermissionStatus(permInfo);
            if (isGranted) grantedCount++;
            
            View permissionView = createPermissionView(permInfo, isGranted);
            permissionsContainer.addView(permissionView);
        }
        
        // Update header
        TextView headerText = findViewById(R.id.headerText);
        headerText.setText(String.format("Permissions Status: %d/%d Granted", grantedCount, totalCount));
        
        // Update buttons
        if (grantedCount == totalCount) {
            grantAllButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            grantAllButton.setVisibility(View.VISIBLE);
            finishButton.setVisibility(View.GONE);
        }
    }
    
    private boolean checkPermissionStatus(PermissionInfo permInfo) {
        if (permInfo.isSpecial) {
            switch (permInfo.permission) {
                case "ACCESSIBILITY":
                    return isAccessibilityServiceEnabled();
                case "NOTIFICATION_LISTENER":
                    return isNotificationListenerEnabled();
                case "USAGE_STATS":
                    return hasUsageStatsPermission();
            }
        } else {
            return ContextCompat.checkSelfPermission(this, permInfo.permission) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }
    
    private View createPermissionView(PermissionInfo permInfo, boolean isGranted) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_permission, permissionsContainer, false);
        
        TextView titleText = view.findViewById(R.id.permissionTitle);
        TextView descText = view.findViewById(R.id.permissionDescription);
        TextView statusText = view.findViewById(R.id.permissionStatus);
        Button actionButton = view.findViewById(R.id.permissionActionButton);
        
        titleText.setText(permInfo.title);
        descText.setText(permInfo.description);
        
        if (isGranted) {
            statusText.setText("✅ Granted");
            statusText.setTextColor(getColor(android.R.color.holo_green_dark));
            actionButton.setVisibility(View.GONE);
        } else {
            statusText.setText("❌ Not Granted");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setText("Grant");
            
            actionButton.setOnClickListener(v -> {
                if (permInfo.isSpecial) {
                    openSpecialPermissionSettings(permInfo.permission);
                } else {
                    requestSinglePermission(permInfo.permission);
                }
            });
        }
        
        return view;
    }
    
    private void requestMissingPermissions() {
        // Request regular permissions
        java.util.List<String> missingPermissions = new java.util.ArrayList<>();
        
        for (PermissionInfo permInfo : permissions) {
            if (!permInfo.isSpecial && !checkPermissionStatus(permInfo)) {
                missingPermissions.add(permInfo.permission);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toArray(new String[0]),
                REQUEST_CODE_PERMISSIONS
            );
        }
        
        // Guide user to special permissions
        for (PermissionInfo permInfo : permissions) {
            if (permInfo.isSpecial && !checkPermissionStatus(permInfo)) {
                Toast.makeText(this, "Please grant " + permInfo.title + " manually", Toast.LENGTH_LONG).show();
                openSpecialPermissionSettings(permInfo.permission);
                break; // Open one at a time
            }
        }
    }
    
    private void requestSinglePermission(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
    }
    
    private void openSpecialPermissionSettings(String permissionType) {
        Intent intent;
        switch (permissionType) {
            case "ACCESSIBILITY":
                intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                Toast.makeText(this, "Enable 'Parental Control Monitor' in Accessibility", Toast.LENGTH_LONG).show();
                break;
                
            case "NOTIFICATION_LISTENER":
                intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                Toast.makeText(this, "Enable 'Parental Control Monitor' in Notification Access", Toast.LENGTH_LONG).show();
                break;
                
            case "USAGE_STATS":
                intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                Toast.makeText(this, "Enable 'Parental Control Monitor' in Usage Access", Toast.LENGTH_LONG).show();
                break;
                
            default:
                return;
        }
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AccessibilityMonitorService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
            getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(service);
    }
    
    private boolean isNotificationListenerEnabled() {
        String listeners = Settings.Secure.getString(
            getContentResolver(),
            "enabled_notification_listeners"
        );
        return listeners != null && listeners.contains(getPackageName());
    }
    
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            refreshPermissionsList();
        }
    }
}

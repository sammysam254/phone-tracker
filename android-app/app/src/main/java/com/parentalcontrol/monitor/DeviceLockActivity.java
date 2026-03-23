package com.parentalcontrol.monitor;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceLockActivity extends Activity {
    
    private UnlockCodeManager unlockCodeManager;
    private TextView lockMessageText;
    private EditText unlockCodeInput;
    private Button unlockButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make this activity completely lock the device
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SECURE
            // Removed FLAG_NOT_TOUCHABLE to allow input
        );
        
        // Prevent user from leaving this activity
        setTaskDescription(new android.app.ActivityManager.TaskDescription("Device Locked"));
        
        // Set as launcher activity to prevent home button
        try {
            android.content.ComponentName componentName = new android.content.ComponentName(this, DeviceLockActivity.class);
            android.content.pm.PackageManager packageManager = getPackageManager();
            packageManager.setComponentEnabledSetting(componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            android.util.Log.w("DeviceLockActivity", "Could not set as launcher: " + e.getMessage());
        }
        
        setContentView(R.layout.activity_device_lock);
        
        unlockCodeManager = new UnlockCodeManager(this);
        
        // Check if device is actually locked
        if (!unlockCodeManager.isDeviceLocked()) {
            finish();
            return;
        }
        
        initViews();
        setupListeners();
        displayLockInfo();
        
        // Disable hardware buttons and system UI
        disableSystemUI();
        
        android.util.Log.i("DeviceLockActivity", "🔒 Device lock screen activated - complete lockdown mode");
    }
    
    private void initViews() {
        lockMessageText = findViewById(R.id.lockMessageText);
        unlockCodeInput = findViewById(R.id.unlockCodeInput);
        unlockButton = findViewById(R.id.unlockButton);
    }
    
    private void setupListeners() {
        unlockButton.setOnClickListener(v -> attemptUnlock());
        
        // Ensure EditText can receive focus and show keyboard
        unlockCodeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Force show keyboard
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(unlockCodeInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        
        // Auto-focus the input field
        unlockCodeInput.requestFocus();
        
        // Force show keyboard immediately
        unlockCodeInput.post(() -> {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(unlockCodeInput, android.view.inputmethod.InputMethodManager.SHOW_FORCED);
            }
        });
    }
    
    private void displayLockInfo() {
        String message = unlockCodeManager.getLockMessage();
        if (message == null || message.isEmpty()) {
            message = "🔒 DEVICE LOCKED\n\nThis device has been locked by parental controls.\n\nEnter the unlock code provided by your parent or request remote unlock.";
        }
        lockMessageText.setText(message);
    }
    
    private void attemptUnlock() {
        String enteredCode = unlockCodeInput.getText().toString().trim();
        
        if (enteredCode.isEmpty()) {
            Toast.makeText(this, "Please enter unlock code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (unlockCodeManager.verifyUnlockCode(enteredCode)) {
            // Correct code - unlock device
            unlockCodeManager.unlockDevice();
            Toast.makeText(this, "✅ Device unlocked successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // Wrong code - show error
            Toast.makeText(this, "❌ Invalid unlock code. Try again.", Toast.LENGTH_LONG).show();
            unlockCodeInput.setText("");
            
            // Vibrate to indicate wrong code
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(500);
                }
            } catch (Exception e) {
                // Ignore vibration errors
            }
        }
    }
    
    private void disableSystemUI() {
        // Hide system UI for complete lockdown
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        
        // Additional window flags for complete lockdown
        try {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            
            // Prevent screenshots
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } catch (Exception e) {
            android.util.Log.w("DeviceLockActivity", "Could not set additional security flags: " + e.getMessage());
        }
    }
    
    @Override
    public void onBackPressed() {
        // Completely prevent back button from working
        Toast.makeText(this, "🔒 Device is locked. Enter unlock code to continue.", Toast.LENGTH_SHORT).show();
        android.util.Log.d("DeviceLockActivity", "Back button blocked - device is locked");
    }
    
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        // Block all hardware keys except volume (for emergency)
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_HOME:
            case android.view.KeyEvent.KEYCODE_RECENT_APPS:
            case android.view.KeyEvent.KEYCODE_MENU:
            case android.view.KeyEvent.KEYCODE_SEARCH:
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_APP_SWITCH:
                Toast.makeText(this, "🔒 Device is locked", Toast.LENGTH_SHORT).show();
                android.util.Log.d("DeviceLockActivity", "Hardware key blocked: " + keyCode);
                return true; // Block the key
            case android.view.KeyEvent.KEYCODE_VOLUME_UP:
            case android.view.KeyEvent.KEYCODE_VOLUME_DOWN:
                // Allow volume buttons for emergency
                return super.onKeyDown(keyCode, event);
            case android.view.KeyEvent.KEYCODE_POWER:
                // Block power button to prevent screen off
                Toast.makeText(this, "🔒 Power button disabled while locked", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        // Also block key up events for critical keys
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_HOME:
            case android.view.KeyEvent.KEYCODE_RECENT_APPS:
            case android.view.KeyEvent.KEYCODE_MENU:
            case android.view.KeyEvent.KEYCODE_SEARCH:
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_APP_SWITCH:
            case android.view.KeyEvent.KEYCODE_POWER:
                return true; // Block the key
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // If device is still locked, immediately restart this activity
        if (unlockCodeManager.isDeviceLocked()) {
            android.util.Log.d("DeviceLockActivity", "Activity paused while locked - restarting immediately");
            Intent intent = new Intent(this, DeviceLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                           Intent.FLAG_ACTIVITY_NO_ANIMATION |
                           Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure we're always on top and system UI is hidden
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        disableSystemUI();
        
        // Check if device was unlocked remotely
        if (!unlockCodeManager.isDeviceLocked()) {
            Toast.makeText(this, "✅ Device unlocked remotely by parent", Toast.LENGTH_SHORT).show();
            android.util.Log.i("DeviceLockActivity", "Device unlocked remotely - finishing lock screen");
            finish();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Prevent the activity from being stopped if device is locked
        if (unlockCodeManager.isDeviceLocked()) {
            android.util.Log.d("DeviceLockActivity", "Activity stopped while locked - restarting");
            Intent intent = new Intent(this, DeviceLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP |
                           Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If device is still locked, restart the activity
        if (unlockCodeManager != null && unlockCodeManager.isDeviceLocked()) {
            android.util.Log.d("DeviceLockActivity", "Activity destroyed while locked - restarting");
            Intent intent = new Intent(this, DeviceLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disableSystemUI();
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && unlockCodeManager != null && unlockCodeManager.isDeviceLocked()) {
            disableSystemUI();
        }
    }
}

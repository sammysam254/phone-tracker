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
        
        // Make this activity full screen and show over lock screen
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        
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
    }
    
    private void initViews() {
        lockMessageText = findViewById(R.id.lockMessageText);
        unlockCodeInput = findViewById(R.id.unlockCodeInput);
        unlockButton = findViewById(R.id.unlockButton);
    }
    
    private void setupListeners() {
        unlockButton.setOnClickListener(v -> attemptUnlock());
    }
    
    private void displayLockInfo() {
        String message = unlockCodeManager.getLockMessage();
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
            Toast.makeText(this, "Device unlocked", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // Wrong code
            Toast.makeText(this, "Invalid unlock code", Toast.LENGTH_SHORT).show();
            unlockCodeInput.setText("");
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing lock screen
        // Do nothing
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // If device is still locked, restart this activity
        if (unlockCodeManager.isDeviceLocked()) {
            Intent intent = new Intent(this, DeviceLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}

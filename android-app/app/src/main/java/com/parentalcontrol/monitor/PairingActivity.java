package com.parentalcontrol.monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PairingActivity extends AppCompatActivity {
    
    private TextView instructionsText;
    private Button scanQRButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        
        initViews();
        setupDevice();
        setupClickListeners();
    }
    
    private void initViews() {
        instructionsText = findViewById(R.id.instructionsText);
        scanQRButton = findViewById(R.id.scanQRButton);
    }
    
    private void setupDevice() {
        String instructions = "📱 Connect to Parent's Account\n\n" +
            "To start monitoring, you need to pair this device with your parent's account.\n\n" +
            "Steps:\n" +
            "1. Your parent opens their monitoring app\n" +
            "2. Parent generates a QR code for pairing\n" +
            "3. Tap 'Scan QR Code' below\n" +
            "4. Point camera at parent's QR code\n" +
            "5. Pairing completes automatically!";
        
        instructionsText.setText(instructions);
    }
    
    private void setupClickListeners() {
        scanQRButton.setOnClickListener(v -> {
            // Launch QR scanner
            Intent intent = new Intent(PairingActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if device is already paired
        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
        boolean isPaired = prefs.getBoolean("device_paired", false);
        
        if (isPaired) {
            // Already paired, go to consent
            Intent intent = new Intent(this, ConsentActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

package com.parentalcontrol.monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import org.json.JSONObject;
import java.util.List;

public class QRScannerActivity extends AppCompatActivity {
    
    private DecoratedBarcodeView barcodeView;
    private SupabaseClient supabaseClient;
    private String deviceId;
    private boolean isPairing = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        barcodeView = findViewById(R.id.barcode_scanner);
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        
        setupScanner();
    }
    
    private void setupScanner() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && !isPairing) {
                    isPairing = true;
                    barcodeView.pause();
                    handleQRCode(result.getText());
                }
            }
            
            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Optional: handle possible result points
            }
        });
    }
    
    private void handleQRCode(String qrData) {
        try {
            // Parse QR code data (format: {"parentId":"xxx","pairingToken":"xxx","timestamp":123456})
            JSONObject data = new JSONObject(qrData);
            String parentId = data.getString("parentId");
            String pairingToken = data.getString("pairingToken");
            long timestamp = data.getLong("timestamp");
            
            // Check if QR code is not expired (valid for 10 minutes)
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > 10 * 60 * 1000) {
                showError("QR code has expired. Please generate a new one from the parent app.");
                return;
            }
            
            // Pair device with parent
            pairWithParent(parentId, pairingToken);
            
        } catch (Exception e) {
            showError("Invalid QR code. Please scan the QR code from the parent app.");
        }
    }
    
    private void pairWithParent(String parentId, String pairingToken) {
        try {
            JSONObject pairingData = new JSONObject();
            pairingData.put("device_id", deviceId);
            pairingData.put("parent_id", parentId);
            pairingData.put("pairing_token", pairingToken);
            pairingData.put("device_name", DeviceUtils.getDeviceModel());
            pairingData.put("device_brand", DeviceUtils.getDeviceBrand());
            pairingData.put("android_version", DeviceUtils.getAndroidVersion());
            
            supabaseClient.pairDeviceWithQR(pairingData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject result = new JSONObject(response);
                            String parentName = result.optString("parent_name", "Parent");
                            
                            // Save pairing info
                            SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("device_paired", true);
                            editor.putBoolean("device_registered", true);
                            editor.putString("parent_id", parentId);
                            editor.putString("parent_name", parentName);
                            editor.apply();
                            
                            // Show success dialog
                            new AlertDialog.Builder(QRScannerActivity.this)
                                    .setTitle("✅ Pairing Successful!")
                                    .setMessage("Your device has been successfully paired with " + parentName + "'s account.\n\nYou will now proceed to the consent screen.")
                                    .setPositiveButton("Continue", (dialog, which) -> {
                                        Intent intent = new Intent(QRScannerActivity.this, ConsentActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                            
                        } catch (Exception e) {
                            showError("Pairing successful but error reading response: " + e.getMessage());
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showError("Pairing failed: " + error);
                    });
                }
            });
            
        } catch (Exception e) {
            showError("Error creating pairing request: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            isPairing = false;
            new AlertDialog.Builder(this)
                    .setTitle("❌ Pairing Error")
                    .setMessage(message)
                    .setPositiveButton("Try Again", (dialog, which) -> {
                        barcodeView.resume();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        finish();
                    })
                    .show();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
}

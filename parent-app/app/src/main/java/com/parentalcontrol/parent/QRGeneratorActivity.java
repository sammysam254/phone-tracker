package com.parentalcontrol.parent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONObject;
import java.security.SecureRandom;

public class QRGeneratorActivity extends AppCompatActivity {
    
    private ImageView qrCodeImage;
    private TextView instructionsText;
    private TextView expiryText;
    private Button regenerateButton;
    private Button backButton;
    
    private String parentId;
    private String pairingToken;
    private long tokenTimestamp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);
        
        initViews();
        loadParentId();
        generateQRCode();
        setupClickListeners();
    }
    
    private void initViews() {
        qrCodeImage = findViewById(R.id.qrCodeImage);
        instructionsText = findViewById(R.id.instructionsText);
        expiryText = findViewById(R.id.expiryText);
        regenerateButton = findViewById(R.id.regenerateButton);
        backButton = findViewById(R.id.backButton);
    }
    
    private void loadParentId() {
        SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        
        // Generate parent ID from email (in production, this should come from backend)
        if (!userEmail.isEmpty()) {
            parentId = "parent_" + userEmail.hashCode();
        } else {
            parentId = "parent_" + System.currentTimeMillis();
        }
    }
    
    private void generateQRCode() {
        try {
            // Generate secure pairing token
            pairingToken = generateSecureToken();
            tokenTimestamp = System.currentTimeMillis();
            
            // Create QR code data
            JSONObject qrData = new JSONObject();
            qrData.put("parentId", parentId);
            qrData.put("pairingToken", pairingToken);
            qrData.put("timestamp", tokenTimestamp);
            
            String qrContent = qrData.toString();
            
            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            qrCodeImage.setImageBitmap(bitmap);
            
            // Update instructions
            String instructions = "📱 Pair Child's Device\n\n" +
                "1. Open the child monitoring app on their device\n" +
                "2. Tap 'Scan QR Code'\n" +
                "3. Point the camera at this QR code\n" +
                "4. Pairing will complete automatically!";
            
            instructionsText.setText(instructions);
            
            // Update expiry text
            expiryText.setText("⏱️ This QR code expires in 10 minutes");
            
            // Store token for verification (in production, send to backend)
            SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("current_pairing_token", pairingToken);
            editor.putLong("token_timestamp", tokenTimestamp);
            editor.apply();
            
        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        
        // Convert to hex string
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        
        return token.toString();
    }
    
    private void setupClickListeners() {
        regenerateButton.setOnClickListener(v -> {
            generateQRCode();
            Toast.makeText(this, "New QR code generated", Toast.LENGTH_SHORT).show();
        });
        
        backButton.setOnClickListener(v -> finish());
    }
}

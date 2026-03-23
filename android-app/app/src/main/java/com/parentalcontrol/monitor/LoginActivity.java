package com.parentalcontrol.monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private SupabaseClient supabaseClient;
    private String deviceId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        supabaseClient = new SupabaseClient(this);
        deviceId = DeviceUtils.getDeviceId(this);
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        
        // Show device info
        TextView deviceIdText = findViewById(R.id.deviceIdText);
        TextView deviceInfoText = findViewById(R.id.deviceInfoText);
        
        if (deviceIdText != null) {
            deviceIdText.setText("Device ID: " + deviceId);
        }
        
        if (deviceInfoText != null) {
            String deviceInfo = DeviceUtils.getDeviceBrand() + " " + DeviceUtils.getDeviceModel();
            deviceInfoText.setText(deviceInfo);
        }
    }
    
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        
        // Enter key support
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }
    
    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        
        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }
        
        performLogin(email, password);
    }
    
    private void performLogin(String email, String password) {
        setLoading(true);
        statusText.setText("Logging in...");
        
        supabaseClient.loginParent(email, password, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject result = new JSONObject(response);
                    String parentId = result.getString("parent_id");
                    String parentEmail = result.getString("email");
                    
                    // Bind device to parent account
                    bindDeviceToParent(parentId, parentEmail);
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        statusText.setText("Login failed: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    statusText.setText("Login failed");
                    
                    String errorMessage = error;
                    if (error.contains("Invalid login credentials")) {
                        errorMessage = "Invalid email or password";
                    } else if (error.contains("Email not confirmed")) {
                        errorMessage = "Please verify your email first";
                    }
                    
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Login error: " + error);
                });
            }
        });
    }
    
    private void bindDeviceToParent(String parentId, String parentEmail) {
        runOnUiThread(() -> {
            statusText.setText("Binding device to your account...");
        });
        
        String deviceName = DeviceUtils.getDeviceBrand() + " " + DeviceUtils.getDeviceModel();
        String deviceBrand = DeviceUtils.getDeviceBrand();
        String androidVersion = android.os.Build.VERSION.RELEASE;
        
        supabaseClient.bindDeviceToParent(deviceId, parentId, deviceName, deviceBrand, androidVersion, 
            new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        // Save login state
                        SharedPreferences prefs = getSharedPreferences("ParentalControl", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("device_paired", true);
                        editor.putString("parent_id", parentId);
                        editor.putString("parent_email", parentEmail);
                        editor.putString("device_name", deviceName);
                        editor.apply();
                        
                        setLoading(false);
                        statusText.setText("✅ Device bound successfully!");
                        
                        Toast.makeText(LoginActivity.this, 
                            "✅ Device bound to " + parentEmail, 
                            Toast.LENGTH_LONG).show();
                        
                        // Navigate to consent activity
                        Intent intent = new Intent(LoginActivity.this, ConsentActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        statusText.setText("Device binding failed");
                        Toast.makeText(LoginActivity.this, 
                            "Failed to bind device: " + error, 
                            Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Device binding error: " + error);
                    });
                }
            });
    }
    
    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!loading);
            emailInput.setEnabled(!loading);
            passwordInput.setEnabled(!loading);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
}

package com.parentalcontrol.parent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    
    private TextView titleText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button actionButton;
    private Button switchModeButton;
    private boolean isRegisterMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        checkMode();
        setupClickListeners();
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        actionButton = findViewById(R.id.actionButton);
        switchModeButton = findViewById(R.id.switchModeButton);
    }
    
    private void checkMode() {
        isRegisterMode = getIntent().getBooleanExtra("register_mode", false);
        updateUI();
    }
    
    private void updateUI() {
        if (isRegisterMode) {
            titleText.setText("Create Parent Account");
            actionButton.setText("Register & Open Dashboard");
            switchModeButton.setText("Already have an account? Login");
        } else {
            titleText.setText("Parent Login");
            actionButton.setText("Login & Open Dashboard");
            switchModeButton.setText("Don't have an account? Register");
        }
    }
    
    private void setupClickListeners() {
        actionButton.setOnClickListener(v -> {
            if (isRegisterMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });
        
        switchModeButton.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            updateUI();
        });
    }
    
    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save credentials for auto-login
        saveCredentials(email, password);
        
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        openDashboard();
    }
    
    private void handleRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save credentials
        saveCredentials(email, password);
        
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        openDashboard();
    }
    
    private void saveCredentials(String email, String password) {
        SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.putString("user_password", password);
        editor.apply();
    }
    
    private void openDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
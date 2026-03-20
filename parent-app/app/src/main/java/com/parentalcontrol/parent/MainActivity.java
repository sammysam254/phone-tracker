package com.parentalcontrol.parent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private TextView welcomeText;
    private Button loginButton;
    private Button registerButton;
    private Button quickAccessButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupClickListeners();
        checkExistingLogin();
    }
    
    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        quickAccessButton = findViewById(R.id.quickAccessButton);
    }
    
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
        
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("register_mode", true);
            startActivity(intent);
        });
        
        quickAccessButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });
    }
    
    private void checkExistingLogin() {
        SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
        String savedEmail = prefs.getString("user_email", "");
        
        if (!savedEmail.isEmpty()) {
            welcomeText.setText("Welcome back, " + savedEmail + "!");
            quickAccessButton.setText("Open Dashboard");
        } else {
            welcomeText.setText("Welcome to Parental Control Dashboard");
            quickAccessButton.setText("Quick Access (No Login)");
        }
    }
}
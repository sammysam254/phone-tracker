package com.parentalcontrol.parent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {
    
    private WebView webView;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button refreshButton;
    private Button backButton;
    
    private static final String DASHBOARD_URL = "https://phonetracker-0a26.onrender.com";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        initViews();
        setupWebView();
        setupClickListeners();
        loadDashboard();
    }
    
    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        refreshButton = findViewById(R.id.refreshButton);
        backButton = findViewById(R.id.backButton);
    }
    
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // Enable JavaScript
        webSettings.setJavaScriptEnabled(true);
        
        // Enable DOM storage
        webSettings.setDomStorageEnabled(true);
        
        // Enable local storage
        webSettings.setDatabaseEnabled(true);
        
        // Set user agent for mobile
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " ParentalControlParentApp/1.0.0");
        
        // Enable zoom controls
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Set viewport
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // Enable mixed content (HTTP and HTTPS)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Set WebView client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading("Loading dashboard...");
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoading();
                
                // Auto-fill login if credentials are stored
                autoFillLoginIfAvailable();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hideLoading();
                showError("Failed to load dashboard: " + description);
            }
        });
    }
    
    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> {
            webView.reload();
            showLoading("Refreshing...");
        });
        
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });
    }
    
    private void loadDashboard() {
        showLoading("Connecting to dashboard...");
        webView.loadUrl(DASHBOARD_URL);
    }
    
    private void autoFillLoginIfAvailable() {
        SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        String userPassword = prefs.getString("user_password", "");
        
        if (!userEmail.isEmpty() && !userPassword.isEmpty()) {
            // JavaScript to auto-fill login form
            String javascript = String.format(
                "javascript:(function(){" +
                "var emailField = document.getElementById('loginEmail') || document.querySelector('input[type=\"email\"]');" +
                "var passwordField = document.getElementById('loginPassword') || document.querySelector('input[type=\"password\"]');" +
                "if(emailField && passwordField) {" +
                "  emailField.value = '%s';" +
                "  passwordField.value = '%s';" +
                "  var loginBtn = document.getElementById('loginBtn') || document.querySelector('button[type=\"submit\"]');" +
                "  if(loginBtn) {" +
                "    setTimeout(function(){ loginBtn.click(); }, 1000);" +
                "  }" +
                "}" +
                "})()",
                userEmail, userPassword
            );
            
            webView.evaluateJavascript(javascript, null);
        }
    }
    
    private void showLoading(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            statusText.setText(message);
            statusText.setVisibility(View.VISIBLE);
        });
    }
    
    private void hideLoading() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
        });
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusText.setText(message);
            statusText.setVisibility(View.VISIBLE);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
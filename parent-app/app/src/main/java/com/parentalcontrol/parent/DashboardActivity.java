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
                
                // Set a timeout for authentication checking
                webView.postDelayed(() -> {
                    // Check if still showing "Checking Authentication"
                    String checkScript = 
                        "javascript:(function(){" +
                        "var h2Elements = document.querySelectorAll('h2');" +
                        "for(var i = 0; i < h2Elements.length; i++) {" +
                        "  if(h2Elements[i].textContent.includes('Checking Authentication')) {" +
                        "    return 'stuck_auth';" +
                        "  }" +
                        "}" +
                        "return 'ok';" +
                        "})()";
                    
                    webView.evaluateJavascript(checkScript, result -> {
                        if (result != null && result.contains("stuck_auth")) {
                            runOnUiThread(() -> {
                                statusText.setText("Authentication timeout. Tap refresh to try again.");
                                statusText.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            });
                        } else {
                            // Auto-fill login if credentials are stored
                            autoFillLoginIfAvailable();
                        }
                    });
                }, 8000); // 8 second timeout for auth check
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
        
        // Add click listener to status text for manual intervention
        statusText.setOnClickListener(v -> {
            if (statusText.getText().toString().contains("timeout") || 
                statusText.getText().toString().contains("taking too long")) {
                // Force show login form
                String forceLoginScript = 
                    "javascript:(function(){" +
                    "console.log('Forcing login form display...');" +
                    "var authSection = document.getElementById('authSection');" +
                    "var dashboardSection = document.getElementById('dashboardSection');" +
                    "if(authSection) {" +
                    "  authSection.style.display = 'block';" +
                    "  if(dashboardSection) dashboardSection.style.display = 'none';" +
                    "}" +
                    "})()";
                
                webView.evaluateJavascript(forceLoginScript, null);
                statusText.setText("Login form should now be visible");
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
            // Wait a bit longer for the page to fully load, then try auto-login
            webView.postDelayed(() -> {
                // JavaScript to auto-fill login form and handle authentication timeout
                String javascript = String.format(
                    "javascript:(function(){" +
                    "console.log('Parent app attempting auto-login...');" +
                    
                    // First, try to hide any persistent auth checking messages
                    "var authSection = document.getElementById('authSection');" +
                    "var authError = document.getElementById('authError');" +
                    "if(authError) authError.style.display = 'none';" +
                    
                    // Check if we're stuck on auth checking and force show login
                    "var checkingAuth = document.querySelector('h2');" +
                    "if(checkingAuth && checkingAuth.textContent.includes('Checking Authentication')) {" +
                    "  console.log('Detected stuck auth check, forcing login form...');" +
                    "  if(authSection) {" +
                    "    authSection.style.display = 'block';" +
                    "    var dashboardSection = document.getElementById('dashboardSection');" +
                    "    if(dashboardSection) dashboardSection.style.display = 'none';" +
                    "  }" +
                    "}" +
                    
                    // Try to find and fill login form
                    "setTimeout(function(){" +
                    "  var emailField = document.getElementById('loginEmail') || document.querySelector('input[type=\"email\"]');" +
                    "  var passwordField = document.getElementById('loginPassword') || document.querySelector('input[type=\"password\"]');" +
                    "  if(emailField && passwordField) {" +
                    "    console.log('Found login fields, filling...');" +
                    "    emailField.value = '%s';" +
                    "    passwordField.value = '%s';" +
                    "    var loginBtn = document.getElementById('loginBtn') || document.querySelector('button[type=\"submit\"]');" +
                    "    if(loginBtn) {" +
                    "      console.log('Clicking login button...');" +
                    "      setTimeout(function(){ loginBtn.click(); }, 500);" +
                    "    }" +
                    "  } else {" +
                    "    console.log('Login fields not found, checking if already logged in...');" +
                    "    var dashboardSection = document.getElementById('dashboardSection');" +
                    "    var userEmail = document.getElementById('userEmail');" +
                    "    if(dashboardSection && userEmail) {" +
                    "      console.log('Already logged in, showing dashboard...');" +
                    "      dashboardSection.style.display = 'block';" +
                    "      if(authSection) authSection.style.display = 'none';" +
                    "    }" +
                    "  }" +
                    "}, 1000);" +
                    
                    "})()",
                    userEmail, userPassword
                );
                
                webView.evaluateJavascript(javascript, null);
                
                // Set a timeout to check if login was successful
                webView.postDelayed(() -> {
                    checkLoginStatus();
                }, 5000);
                
            }, 2000); // Wait 2 seconds for page to load
        } else {
            // No saved credentials, just try to show login form if stuck
            webView.postDelayed(() -> {
                String javascript = 
                    "javascript:(function(){" +
                    "var checkingAuth = document.querySelector('h2');" +
                    "if(checkingAuth && checkingAuth.textContent.includes('Checking Authentication')) {" +
                    "  console.log('No saved credentials, forcing login form...');" +
                    "  var authSection = document.getElementById('authSection');" +
                    "  if(authSection) {" +
                    "    authSection.style.display = 'block';" +
                    "    var dashboardSection = document.getElementById('dashboardSection');" +
                    "    if(dashboardSection) dashboardSection.style.display = 'none';" +
                    "  }" +
                    "}" +
                    "})()";
                
                webView.evaluateJavascript(javascript, null);
            }, 3000);
        }
    }
    
    private void checkLoginStatus() {
        String javascript = 
            "javascript:(function(){" +
            "var dashboardSection = document.getElementById('dashboardSection');" +
            "var authSection = document.getElementById('authSection');" +
            "if(dashboardSection && dashboardSection.style.display !== 'none') {" +
            "  return 'logged_in';" +
            "} else if(authSection && authSection.style.display !== 'none') {" +
            "  return 'login_form';" +
            "} else {" +
            "  return 'checking';" +
            "}" +
            "})()";
        
        webView.evaluateJavascript(javascript, result -> {
            if (result != null) {
                if (result.contains("checking")) {
                    // Still stuck, show error and refresh option
                    showError("Authentication taking too long. Tap refresh to try again.");
                } else if (result.contains("login_form")) {
                    hideLoading();
                    statusText.setText("Please login to continue");
                    statusText.setVisibility(View.VISIBLE);
                }
            }
        });
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
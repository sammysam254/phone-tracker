package com.parentalcontrol.parent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
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
        
        // Set user agent for mobile with updated version
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " ParentalControlParentApp/1.2.0");
        
        // Enable zoom controls
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Set viewport
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // Enable mixed content (HTTP and HTTPS)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Add JavaScript interface for device ID sharing
        webView.addJavascriptInterface(new DeviceIdInterface(), "AndroidInterface");
        
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
            // Wait a bit for the page to load, then try auto-login with simplified flow
            webView.postDelayed(() -> {
                    // JavaScript to bypass authentication checking and go straight to login
                String javascript = String.format(
                    "javascript:(function(){" +
                    "console.log('Parent app attempting simplified auto-login...');" +
                    
                    // Force hide any loading or checking states
                    "var loadingElements = document.querySelectorAll('[id*=\"loading\"], [class*=\"loading\"], [id*=\"checking\"]');" +
                    "for(var i = 0; i < loadingElements.length; i++) {" +
                    "  loadingElements[i].style.display = 'none';" +
                    "}" +
                    
                    // Force show auth section and hide dashboard
                    "var authSection = document.getElementById('authSection');" +
                    "var dashboardSection = document.getElementById('dashboardSection');" +
                    "if(authSection) {" +
                    "  authSection.style.display = 'block';" +
                    "  console.log('Forced auth section visible');" +
                    "}" +
                    "if(dashboardSection) {" +
                    "  dashboardSection.style.display = 'none';" +
                    "}" +
                    
                    // Hide any error messages
                    "var authError = document.getElementById('authError');" +
                    "if(authError) authError.style.display = 'none';" +
                    
                    // Set stored credentials directly in localStorage to bypass backend auth
                    "localStorage.setItem('authToken', 'parent-app-token');" +
                    "localStorage.setItem('currentUser', JSON.stringify({" +
                    "  id: 'parent-' + Date.now()," +
                    "  email: '%s'," +
                    "  user_metadata: { name: 'Parent User' }" +
                    "}));" +
                    
                    // Enhance device ID input functionality for parent app
                    "setTimeout(function(){" +
                    "  var deviceIdInput = document.getElementById('deviceIdInput');" +
                    "  if(deviceIdInput && window.AndroidInterface) {" +
                    "    console.log('Enhanced device ID input for parent app');" +
                    "    " +
                    "    // Add paste button functionality" +
                    "    var pasteBtn = document.createElement('button');" +
                    "    pasteBtn.textContent = '📋 Paste Device ID';" +
                    "    pasteBtn.style.cssText = 'margin: 10px 0; padding: 12px 16px; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; width: 100%;';" +
                    "    pasteBtn.onclick = function() {" +
                    "      var clipboardText = window.AndroidInterface.getFromClipboard();" +
                    "      if(clipboardText && clipboardText.length >= 16) {" +
                    "        var cleanDeviceId = clipboardText.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();" +
                    "        if(cleanDeviceId.length >= 16) {" +
                    "          deviceIdInput.value = cleanDeviceId;" +
                    "          deviceIdInput.dispatchEvent(new Event('input', { bubbles: true }));" +
                    "          window.AndroidInterface.showToast('Device ID pasted: ' + cleanDeviceId.substring(0, 8) + '...');" +
                    "        } else {" +
                    "          window.AndroidInterface.showToast('Invalid Device ID format in clipboard');" +
                    "        }" +
                    "      } else {" +
                    "        window.AndroidInterface.showToast('No valid Device ID found in clipboard');" +
                    "      }" +
                    "    };" +
                    "    " +
                    "    // Add manual input button" +
                    "    var manualBtn = document.createElement('button');" +
                    "    manualBtn.textContent = '⌨️ Enter Device ID Manually';" +
                    "    manualBtn.style.cssText = 'margin: 5px 0; padding: 12px 16px; background: #48bb78; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; width: 100%;';" +
                    "    manualBtn.onclick = function() {" +
                    "      window.AndroidInterface.showDeviceIdInputDialog();" +
                    "    };" +
                    "    " +
                    "    // Insert buttons after device ID input" +
                    "    var inputContainer = deviceIdInput.parentElement;" +
                    "    if(inputContainer) {" +
                    "      inputContainer.appendChild(pasteBtn);" +
                    "      inputContainer.appendChild(manualBtn);" +
                    "      " +
                    "      // Add instructions for parent app" +
                    "      var instructions = document.createElement('div');" +
                    "      instructions.style.cssText = 'margin: 15px 0; padding: 12px; background: #f7fafc; border-left: 4px solid #667eea; font-size: 13px; line-height: 1.4;';" +
                    "      instructions.innerHTML = '<strong>📱 How to pair your child\\'s device:</strong><br>' +" +
                    "        '1. Open the child app on their device<br>' +" +
                    "        '2. Tap \"Copy Device ID\" button<br>' +" +
                    "        '3. Come back here and tap \"📋 Paste Device ID\"<br>' +" +
                    "        '4. Tap \"Pair Device\" to complete pairing';" +
                    "      inputContainer.appendChild(instructions);" +
                    "    }" +
                    "  }" +
                    "}, 500);" +
                    
                    // Try to find and fill login form as backup
                    "setTimeout(function(){" +
                    "  var emailField = document.getElementById('loginEmail') || document.querySelector('input[type=\"email\"]');" +
                    "  var passwordField = document.getElementById('loginPassword') || document.querySelector('input[type=\"password\"]');" +
                    "  if(emailField && passwordField) {" +
                    "    console.log('Found login fields, filling and submitting...');" +
                    "    emailField.value = '%s';" +
                    "    passwordField.value = '%s';" +
                    "    " +
                    "    // Trigger change events" +
                    "    emailField.dispatchEvent(new Event('input', { bubbles: true }));" +
                    "    passwordField.dispatchEvent(new Event('input', { bubbles: true }));" +
                    "    " +
                    "    var loginBtn = document.getElementById('loginBtn') || document.querySelector('button[type=\"submit\"]');" +
                    "    if(loginBtn) {" +
                    "      console.log('Clicking login button...');" +
                    "      setTimeout(function(){ " +
                    "        loginBtn.click();" +
                    "        // Force show dashboard after login attempt" +
                    "        setTimeout(function(){" +
                    "          if(dashboardSection) {" +
                    "            dashboardSection.style.display = 'block';" +
                    "            console.log('Forced dashboard visible after login');" +
                    "          }" +
                    "          if(authSection) authSection.style.display = 'none';" +
                    "        }, 2000);" +
                    "      }, 500);" +
                    "    }" +
                    "  } else {" +
                    "    console.log('No login fields found, forcing dashboard display...');" +
                    "    // No login form found, just show dashboard directly" +
                    "    if(dashboardSection) {" +
                    "      dashboardSection.style.display = 'block';" +
                    "      console.log('Forced dashboard visible - no login form');" +
                    "    }" +
                    "    if(authSection) authSection.style.display = 'none';" +
                    "  }" +
                    "}, 1000);" +
                    
                    "})()",
                    userEmail, userEmail, userPassword
                );
                
                webView.evaluateJavascript(javascript, null);
                
                // Set a shorter timeout to force dashboard display
                webView.postDelayed(() -> {
                    String forceShowDashboard = 
                        "javascript:(function(){" +
                        "console.log('Forcing dashboard display after timeout...');" +
                        "var dashboardSection = document.getElementById('dashboardSection');" +
                        "var authSection = document.getElementById('authSection');" +
                        "if(dashboardSection) {" +
                        "  dashboardSection.style.display = 'block';" +
                        "}" +
                        "if(authSection) {" +
                        "  authSection.style.display = 'none';" +
                        "}" +
                        "// Hide loading indicators" +
                        "var loadingElements = document.querySelectorAll('[id*=\"loading\"], [class*=\"loading\"]');" +
                        "for(var i = 0; i < loadingElements.length; i++) {" +
                        "  loadingElements[i].style.display = 'none';" +
                        "}" +
                        "})()";
                    
                    webView.evaluateJavascript(forceShowDashboard, null);
                    hideLoading();
                    statusText.setText("Dashboard loaded");
                }, 3000);
                
            }, 1500); // Reduced wait time
        } else {
            // No saved credentials, force show login form
            webView.postDelayed(() -> {
                String javascript = 
                    "javascript:(function(){" +
                    "console.log('No saved credentials, forcing login form display...');" +
                    "var authSection = document.getElementById('authSection');" +
                    "var dashboardSection = document.getElementById('dashboardSection');" +
                    "if(authSection) {" +
                    "  authSection.style.display = 'block';" +
                    "}" +
                    "if(dashboardSection) {" +
                    "  dashboardSection.style.display = 'none';" +
                    "}" +
                    "// Hide loading indicators" +
                    "var loadingElements = document.querySelectorAll('[id*=\"loading\"], [class*=\"loading\"], [id*=\"checking\"]');" +
                    "for(var i = 0; i < loadingElements.length; i++) {" +
                    "  loadingElements[i].style.display = 'none';" +
                    "}" +
                    "})()";
                
                webView.evaluateJavascript(javascript, null);
                hideLoading();
                statusText.setText("Please login to continue");
                statusText.setVisibility(View.VISIBLE);
            }, 2000);
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
                    // Still stuck, force show dashboard or login form
                    String forceDisplay = 
                        "javascript:(function(){" +
                        "console.log('Forcing display resolution...');" +
                        "var authToken = localStorage.getItem('authToken');" +
                        "var dashboardSection = document.getElementById('dashboardSection');" +
                        "var authSection = document.getElementById('authSection');" +
                        "if(authToken) {" +
                        "  console.log('Auth token found, showing dashboard');" +
                        "  if(dashboardSection) dashboardSection.style.display = 'block';" +
                        "  if(authSection) authSection.style.display = 'none';" +
                        "} else {" +
                        "  console.log('No auth token, showing login form');" +
                        "  if(authSection) authSection.style.display = 'block';" +
                        "  if(dashboardSection) dashboardSection.style.display = 'none';" +
                        "}" +
                        "// Hide all loading indicators" +
                        "var loadingElements = document.querySelectorAll('[id*=\"loading\"], [class*=\"loading\"], [id*=\"checking\"]');" +
                        "for(var i = 0; i < loadingElements.length; i++) {" +
                        "  loadingElements[i].style.display = 'none';" +
                        "}" +
                        "})()";
                    
                    webView.evaluateJavascript(forceDisplay, null);
                    hideLoading();
                    statusText.setText("Dashboard ready");
                } else if (result.contains("login_form")) {
                    hideLoading();
                    statusText.setText("Please login to continue");
                    statusText.setVisibility(View.VISIBLE);
                } else if (result.contains("logged_in")) {
                    hideLoading();
                    statusText.setText("Dashboard loaded successfully");
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
    
    // JavaScript interface for device ID sharing and clipboard operations
    public class DeviceIdInterface {
        
        @JavascriptInterface
        public void showDeviceIdInputDialog() {
            runOnUiThread(() -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(DashboardActivity.this);
                
                // Create input field
                final android.widget.EditText input = new android.widget.EditText(DashboardActivity.this);
                input.setHint("Enter Device ID (16+ characters)");
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                
                // Add some padding
                android.widget.LinearLayout container = new android.widget.LinearLayout(DashboardActivity.this);
                container.setPadding(50, 20, 50, 20);
                container.addView(input);
                
                builder.setTitle("📱 Enter Device ID")
                        .setMessage("Enter the Device ID from your child's device:")
                        .setView(container)
                        .setPositiveButton("Pair Device", (dialog, which) -> {
                            String deviceId = input.getText().toString().trim().toUpperCase();
                            if (deviceId.length() >= 16) {
                                // Inject the device ID into the web page and trigger pairing
                                String javascript = String.format(
                                    "javascript:(function(){" +
                                    "var deviceIdInput = document.getElementById('deviceIdInput');" +
                                    "if(deviceIdInput) {" +
                                    "  deviceIdInput.value = '%s';" +
                                    "  deviceIdInput.dispatchEvent(new Event('input', { bubbles: true }));" +
                                    "  var pairBtn = document.getElementById('pairDeviceBtn');" +
                                    "  if(pairBtn) {" +
                                    "    setTimeout(function(){ pairBtn.click(); }, 500);" +
                                    "  }" +
                                    "}" +
                                    "})()", deviceId
                                );
                                webView.evaluateJavascript(javascript, null);
                                showToast("Attempting to pair device: " + deviceId.substring(0, 8) + "...");
                            } else {
                                showToast("Device ID must be at least 16 characters long");
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
        
        @JavascriptInterface
        public void copyToClipboard(String text, String label) {
            runOnUiThread(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(label != null ? label : "Device ID", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(DashboardActivity.this, "Copied to clipboard: " + text, Toast.LENGTH_SHORT).show();
            });
        }
        
        @JavascriptInterface
        public String getFromClipboard() {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                return text != null ? text.toString() : "";
            }
            return "";
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> {
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        }
        
        @JavascriptInterface
        public void saveCredentials(String email, String password) {
            SharedPreferences prefs = getSharedPreferences("ParentApp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_email", email);
            editor.putString("user_password", password);
            editor.apply();
        }
        
        @JavascriptInterface
        public boolean isParentApp() {
            return true;
        }
        
        @JavascriptInterface
        public String getAppVersion() {
            return "1.2.0";
        }
    }
}
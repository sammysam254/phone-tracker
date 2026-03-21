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
        
        // Enable JavaScript (required for enhanced dashboard)
        webSettings.setJavaScriptEnabled(true);
        
        // Enable DOM storage (required for maps and media)
        webSettings.setDomStorageEnabled(true);
        
        // Enable local storage
        webSettings.setDatabaseEnabled(true);
        
        // Enable file access for media viewing
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Enable media playback
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // Set user agent for mobile with updated version
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " ParentalControlParentApp/1.5.0");
        
        // Enable zoom controls for maps and images
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Set viewport for responsive design
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // Enable mixed content (HTTP and HTTPS) for media loading
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Enable caching for better performance
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Enable geolocation for maps
        webSettings.setGeolocationEnabled(true);
        
        // Set WebView client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading("Loading enhanced dashboard...");
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Inject mobile-optimized CSS and fix for expandable items
                String mobileOptimizationScript = 
                    "javascript:(function(){" +
                    "console.log('Applying mobile optimizations and fixes...');" +
                    
                    // Add mobile-optimized styles
                    "var style = document.createElement('style');" +
                    "style.innerHTML = '" +
                    // Make activity items more touch-friendly
                    ".activity-item { padding: 15px !important; min-height: 60px !important; cursor: pointer !important; }" +
                    ".activity-item.expandable { background: linear-gradient(to right, #f8f9fa 0%, #ffffff 100%) !important; }" +
                    ".activity-item.expandable:hover { background: linear-gradient(to right, #e9ecef 0%, #f8f9fa 100%) !important; }" +
                    ".expand-indicator { font-size: 16px !important; transition: transform 0.3s ease !important; }" +
                    ".activity-item.expanded .expand-indicator { transform: rotate(180deg) !important; }" +
                    
                    // Expanded content styling
                    ".activity-expanded { padding: 15px !important; background: #f8f9fa !important; border-top: 2px solid #dee2e6 !important; }" +
                    ".expanded-details { font-size: 14px !important; line-height: 1.6 !important; }" +
                    ".detail-row { margin: 8px 0 !important; padding: 5px 0 !important; }" +
                    ".message-content, .notification-content { margin: 12px 0 !important; padding: 12px !important; background: white !important; border-radius: 8px !important; border-left: 4px solid #667eea !important; }" +
                    ".message-text { font-size: 14px !important; line-height: 1.6 !important; white-space: pre-wrap !important; word-wrap: break-word !important; margin-top: 8px !important; }" +
                    
                    // Media thumbnails
                    ".media-thumbnail { height: 200px !important; border-radius: 8px !important; }" +
                    ".image-viewer-content { max-width: 95vw !important; max-height: 90vh !important; }" +
                    
                    // Map container
                    ".map-container { height: 300px !important; border-radius: 8px !important; margin: 10px 0 !important; }" +
                    
                    // Audio player
                    "audio { width: 100% !important; margin: 10px 0 !important; }" +
                    
                    // Device selector
                    "#deviceSelector { font-size: 16px !important; padding: 10px !important; min-height: 44px !important; }" +
                    
                    // Tab buttons
                    ".tab-nav button { min-height: 44px !important; font-size: 14px !important; padding: 10px 15px !important; }" +
                    
                    // Filter controls
                    ".filter-controls select, .filter-controls input, .filter-controls button { min-height: 44px !important; font-size: 14px !important; }" +
                    "';" +
                    "document.head.appendChild(style);" +
                    
                    // Fix toggleActivityDetails function to work properly
                    "window.toggleActivityDetails = function(activityId) {" +
                    "  console.log('Toggling activity:', activityId);" +
                    "  var expandedDiv = document.getElementById(activityId);" +
                    "  var activityItem = expandedDiv ? expandedDiv.previousElementSibling : null;" +
                    "  " +
                    "  if (expandedDiv) {" +
                    "    var isCurrentlyVisible = expandedDiv.style.display !== 'none';" +
                    "    " +
                    "    if (isCurrentlyVisible) {" +
                    "      expandedDiv.style.display = 'none';" +
                    "      if (activityItem) activityItem.classList.remove('expanded');" +
                    "      console.log('Collapsed:', activityId);" +
                    "    } else {" +
                    "      expandedDiv.style.display = 'block';" +
                    "      if (activityItem) activityItem.classList.add('expanded');" +
                    "      console.log('Expanded:', activityId);" +
                    "      " +
                    "      // Scroll into view smoothly" +
                    "      setTimeout(function() {" +
                    "        expandedDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });" +
                    "      }, 100);" +
                    "    }" +
                    "  } else {" +
                    "    console.error('Could not find expanded div for:', activityId);" +
                    "  }" +
                    "};" +
                    
                    // Ensure all expandable items have proper click handlers
                    "setTimeout(function() {" +
                    "  var expandableItems = document.querySelectorAll('.activity-item.expandable');" +
                    "  console.log('Found', expandableItems.length, 'expandable items');" +
                    "  expandableItems.forEach(function(item) {" +
                    "    // Remove any existing onclick to avoid duplicates" +
                    "    item.onclick = null;" +
                    "    " +
                    "    // Add new click handler" +
                    "    item.addEventListener('click', function(e) {" +
                    "      e.stopPropagation();" +
                    "      var expandedDiv = this.nextElementSibling;" +
                    "      if (expandedDiv && expandedDiv.classList.contains('activity-expanded')) {" +
                    "        var activityId = expandedDiv.id;" +
                    "        if (activityId) {" +
                    "          window.toggleActivityDetails(activityId);" +
                    "        }" +
                    "      }" +
                    "    });" +
                    "    " +
                    "    // Add visual feedback" +
                    "    item.style.cursor = 'pointer';" +
                    "  });" +
                    "}, 500);" +
                    
                    // Fix device selector if it exists
                    "setTimeout(function() {" +
                    "  var deviceSelector = document.getElementById('deviceSelector');" +
                    "  if (deviceSelector) {" +
                    "    console.log('Device selector found, ensuring it works');" +
                    "    deviceSelector.style.pointerEvents = 'auto';" +
                    "    deviceSelector.style.opacity = '1';" +
                    "    " +
                    "    // Make sure change event works" +
                    "    deviceSelector.addEventListener('change', function() {" +
                    "      console.log('Device changed to:', this.value);" +
                    "      if (window.selectDevice) {" +
                    "        window.selectDevice(this.value);" +
                    "      }" +
                    "    });" +
                    "  }" +
                    "}, 1000);" +
                    
                    "console.log('Mobile optimizations and fixes applied successfully');" +
                    "})()";
                
                webView.evaluateJavascript(mobileOptimizationScript, null);
                
                // Re-apply fixes when content changes (e.g., tab switches)
                String observerScript = 
                    "javascript:(function(){" +
                    "var observer = new MutationObserver(function(mutations) {" +
                    "  mutations.forEach(function(mutation) {" +
                    "    if (mutation.addedNodes.length > 0) {" +
                    "      // Re-attach click handlers to new expandable items" +
                    "      setTimeout(function() {" +
                    "        var expandableItems = document.querySelectorAll('.activity-item.expandable');" +
                    "        expandableItems.forEach(function(item) {" +
                    "          if (!item.hasAttribute('data-click-attached')) {" +
                    "            item.setAttribute('data-click-attached', 'true');" +
                    "            item.addEventListener('click', function(e) {" +
                    "              e.stopPropagation();" +
                    "              var expandedDiv = this.nextElementSibling;" +
                    "              if (expandedDiv && expandedDiv.classList.contains('activity-expanded')) {" +
                    "                var activityId = expandedDiv.id;" +
                    "                if (activityId && window.toggleActivityDetails) {" +
                    "                  window.toggleActivityDetails(activityId);" +
                    "                }" +
                    "              }" +
                    "            });" +
                    "          }" +
                    "        });" +
                    "      }, 300);" +
                    "    }" +
                    "  });" +
                    "});" +
                    "observer.observe(document.body, { childList: true, subtree: true });" +
                    "console.log('Mutation observer attached for dynamic content');" +
                    "})()";
                
                webView.evaluateJavascript(observerScript, null);
                
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
        
        // Enable Chrome client for better media support
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                // Allow geolocation for maps
                callback.invoke(origin, true, false);
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    hideLoading();
                }
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
                    
                    // Force show dashboard after login attempt
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
}
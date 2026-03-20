// API Configuration
const SUPABASE_URL = 'https://gejzprqznycnbfzeaxza.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdlanpwcnF6bnljbmJmemVheHphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5OTM2MTQsImV4cCI6MjA4OTU2OTYxNH0.zl9tfulUKL3aDbz4NjQOgOTk5JdMd8_Pf1YvHHN0SOQ';

// Import Supabase client
import { createClient } from 'https://cdn.skypack.dev/@supabase/supabase-js@2';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
let currentUser = null;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    checkAuthState();
    setInterval(refreshData, 30000); // Refresh every 30 seconds
});

// Authentication state management
async function checkAuthState() {
    const { data: { session } } = await supabase.auth.getSession();
    
    if (session) {
        currentUser = session.user;
        loadDashboard();
    } else {
        showLogin();
    }
    
    // Listen for auth changes
    supabase.auth.onAuthStateChange((event, session) => {
        if (event === 'SIGNED_IN') {
            currentUser = session.user;
            location.reload();
        } else if (event === 'SIGNED_OUT') {
            currentUser = null;
            showLogin();
        }
    });
}

// Authentication
function showLogin() {
    document.body.innerHTML = `
        <div class="container mt-5">
            <div class="row justify-content-center">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header text-center">
                            <h4><i class="fas fa-shield-alt"></i> Parental Control Login</h4>
                        </div>
                        <div class="card-body">
                            <form id="loginForm">
                                <div class="mb-3">
                                    <label for="email" class="form-label">Email</label>
                                    <input type="email" class="form-control" id="email" required>
                                </div>
                                <div class="mb-3">
                                    <label for="password" class="form-label">Password</label>
                                    <input type="password" class="form-control" id="password" required>
                                </div>
                                <button type="submit" class="btn btn-primary w-100">Login</button>
                            </form>
                            <hr>
                            <p class="text-center">
                                <a href="#" onclick="showRegister()">Don't have an account? Register</a>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.getElementById('loginForm').addEventListener('submit', login);
}

function showRegister() {
    document.body.innerHTML = `
        <div class="container mt-5">
            <div class="row justify-content-center">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header text-center">
                            <h4><i class="fas fa-user-plus"></i> Register Parent Account</h4>
                        </div>
                        <div class="card-body">
                            <form id="registerForm">
                                <div class="mb-3">
                                    <label for="name" class="form-label">Full Name</label>
                                    <input type="text" class="form-control" id="name" required>
                                </div>
                                <div class="mb-3">
                                    <label for="email" class="form-label">Email</label>
                                    <input type="email" class="form-control" id="email" required>
                                </div>
                                <div class="mb-3">
                                    <label for="password" class="form-label">Password</label>
                                    <input type="password" class="form-control" id="password" required>
                                </div>
                                <button type="submit" class="btn btn-primary w-100">Register</button>
                            </form>
                            <hr>
                            <p class="text-center">
                                <a href="#" onclick="showLogin()">Already have an account? Login</a>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.getElementById('registerForm').addEventListener('submit', register);
}

async function login(e) {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    try {
        const { data, error } = await supabase.auth.signInWithPassword({
            email,
            password
        });
        
        if (error) {
            alert('Login failed: ' + error.message);
        } else {
            // Success handled by auth state change listener
        }
    } catch (error) {
        alert('Login error: ' + error.message);
    }
}

async function register(e) {
    e.preventDefault();
    
    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    try {
        const { data, error } = await supabase.auth.signUp({
            email,
            password,
            options: {
                data: {
                    name: name
                }
            }
        });
        
        if (error) {
            alert('Registration failed: ' + error.message);
        } else {
            alert('Registration successful! Please check your email to verify your account.');
            showLogin();
        }
    } catch (error) {
        alert('Registration error: ' + error.message);
    }
}

async function logout() {
    await supabase.auth.signOut();
}

// Dashboard functions
async function loadDashboard() {
    await Promise.all([
        loadDevices(),
        loadRecentActivities(),
        updateStats()
    ]);
}

async function loadDevices() {
    try {
        const { data: devices, error } = await supabase
            .from('devices')
            .select('*')
            .eq('parent_id', currentUser.id);
        
        if (error) throw error;
        
        document.getElementById('deviceCount').textContent = devices.length;
        
        const devicesList = document.getElementById('devicesList');
        if (devices.length === 0) {
            devicesList.innerHTML = '<p class="text-muted">No devices registered yet.</p>';
            return;
        }
        
        devicesList.innerHTML = devices.map(device => `
            <div class="card mb-3">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col">
                            <h5 class="card-title">
                                <i class="fas fa-mobile-alt"></i> ${device.device_name}
                            </h5>
                            <p class="card-text">
                                <small class="text-muted">ID: ${device.device_id}</small><br>
                                <small class="text-muted">Last Active: ${new Date(device.last_active).toLocaleString()}</small>
                            </p>
                        </div>
                        <div class="col-auto">
                            <span class="badge ${device.consent_granted ? 'bg-success' : 'bg-warning'}">
                                ${device.consent_granted ? 'Consent Granted' : 'Pending Consent'}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading devices:', error);
    }
}

async function loadRecentActivities() {
    try {
        const { data: devices, error: devicesError } = await supabase
            .from('devices')
            .select('device_id')
            .eq('parent_id', currentUser.id);
        
        if (devicesError) throw devicesError;
        
        if (devices.length === 0) {
            document.getElementById('recentActivities').innerHTML = 
                '<p class="text-muted">No devices to show activities for.</p>';
            return;
        }
        
        // Load activities for first device (or all devices)
        const deviceId = devices[0].device_id;
        const { data: activities, error } = await supabase
            .from('activities')
            .select('*')
            .eq('device_id', deviceId)
            .order('timestamp', { ascending: false })
            .limit(10);
        
        if (error) throw error;
        
        const activitiesHtml = activities.map(activity => {
            const icon = getActivityIcon(activity.activity_type);
            const time = new Date(activity.timestamp).toLocaleString();
            
            return `
                <div class="card activity-card mb-2">
                    <div class="card-body py-2">
                        <div class="row align-items-center">
                            <div class="col-auto">
                                <i class="${icon}"></i>
                            </div>
                            <div class="col">
                                <strong>${activity.activity_type.toUpperCase()}</strong>
                                <small class="text-muted d-block">${time}</small>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        document.getElementById('recentActivities').innerHTML = 
            activitiesHtml || '<p class="text-muted">No recent activities.</p>';
            
    } catch (error) {
        console.error('Error loading activities:', error);
    }
}

async function updateStats() {
    try {
        const { data: devices, error: devicesError } = await supabase
            .from('devices')
            .select('device_id')
            .eq('parent_id', currentUser.id);
        
        if (devicesError || devices.length === 0) {
            return;
        }
        
        const deviceId = devices[0].device_id;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        const { data: activities, error } = await supabase
            .from('activities')
            .select('activity_type')
            .eq('device_id', deviceId)
            .gte('timestamp', today.toISOString());
        
        if (error) throw error;
        
        const stats = activities.reduce((acc, activity) => {
            acc[activity.activity_type] = (acc[activity.activity_type] || 0) + 1;
            return acc;
        }, {});
        
        document.getElementById('callCount').textContent = stats.call || 0;
        document.getElementById('smsCount').textContent = stats.sms || 0;
        document.getElementById('screenTime').textContent = '4.2h'; // Placeholder
        
    } catch (error) {
        console.error('Error updating stats:', error);
    }
}

function getActivityIcon(type) {
    const icons = {
        'call': 'fas fa-phone text-primary',
        'sms': 'fas fa-sms text-success',
        'app_usage': 'fas fa-mobile-alt text-info',
        'camera': 'fas fa-camera text-warning',
        'mic': 'fas fa-microphone text-danger'
    };
    return icons[type] || 'fas fa-circle text-secondary';
}

async function addDevice() {
    const deviceName = document.getElementById('deviceName').value;
    const pairingCode = document.getElementById('pairingCode').value;
    
    if (!pairingCode || pairingCode.length !== 6) {
        alert('Please enter a valid 6-digit pairing code');
        return;
    }
    
    try {
        // Call the pairing function
        const { data, error } = await supabase.rpc('pair_device_with_parent', {
            pairing_code_input: pairingCode,
            parent_user_id: currentUser.id
        });
        
        if (error) throw error;
        
        if (data.success) {
            alert(`Device "${data.device_name}" paired successfully!`);
            document.getElementById('addDeviceForm').reset();
            bootstrap.Modal.getInstance(document.getElementById('addDeviceModal')).hide();
            loadDevices();
        } else {
            alert('Pairing failed: ' + data.error);
        }
    } catch (error) {
        alert('Error pairing device: ' + error.message);
    }
}

function showSection(sectionName) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    
    // Show selected section
    document.getElementById(sectionName).style.display = 'block';
    
    // Update active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    document.querySelector(`[href="#${sectionName}"]`).classList.add('active');
}

function refreshData() {
    loadDashboard();
}
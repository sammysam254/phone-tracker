const express = require('express');
const cors = require('cors');
const path = require('path');
const { createClient } = require('@supabase/supabase-js');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Supabase configuration
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
const supabase = createClient(supabaseUrl, supabaseServiceKey);

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Serve static files from web-dashboard directory
app.use(express.static(path.join(__dirname, '../web-dashboard')));

// Specific route for APK downloads with proper headers
app.get('/apk/:filename', (req, res) => {
  const filename = req.params.filename;
  
  // Security check - only allow APK files
  if (!filename.endsWith('.apk')) {
    return res.status(404).json({ error: 'File not found' });
  }
  
  const filePath = path.join(__dirname, '../web-dashboard/apk', filename);
  
  // Set proper headers for APK download
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
  res.setHeader('Cache-Control', 'no-cache');
  
  // Send file
  res.sendFile(filePath, (err) => {
    if (err) {
      console.error('APK download error:', err);
      res.status(404).json({ error: 'APK file not found' });
    }
  });
});

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ 
    status: 'healthy', 
    timestamp: new Date().toISOString(),
    version: '1.0.0'
  });
});

// Root endpoint - redirect to dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../web-dashboard/index.html'));
});

// API info endpoint
app.get('/api', (req, res) => {
  res.json({ 
    message: 'Parental Control API Server',
    version: '1.0.0',
    endpoints: {
      'GET /health': 'Health check',
      'GET /api': 'API information',
      'POST /api/register': 'Parent registration',
      'POST /api/login': 'Parent login',
      'POST /api/verify-token': 'Verify authentication token',
      'POST /api/device/register': 'Register child device',
      'POST /api/pair-device': 'Pair device with parent account',
      'POST /api/activity': 'Log device activity',
      'GET /api/activities/:deviceId': 'Get device activities',
      'GET /api/devices': 'Get parent devices',
      'GET /api/stats/:deviceId': 'Get device statistics',
      'GET /apk/:filename': 'Download APK files',
      'GET /download': 'Download page'
    },
    dashboard: 'Access web dashboard at root URL (/)',
    downloads: {
      'Release APK': '/apk/app-release.apk',
      'Debug APK': '/apk/app-debug.apk',
      'Download Page': '/download'
    }
  });
});

// Authentication middleware
const authenticateUser = async (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'No token provided' });
  }

  try {
    const { data: { user }, error } = await supabase.auth.getUser(token);
    
    if (error || !user) {
      return res.status(403).json({ error: 'Invalid token' });
    }
    
    req.user = user;
    next();
  } catch (error) {
    return res.status(403).json({ error: 'Token verification failed' });
  }
};

// Routes
app.post('/api/register', async (req, res) => {
  try {
    const { email, password, name } = req.body;
    
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
      return res.status(400).json({ error: error.message });
    }
    
    res.status(201).json({ 
      message: 'Parent registered successfully',
      user: data.user 
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password
    });
    
    if (error) {
      return res.status(401).json({ error: error.message });
    }
    
    res.json({ 
      token: data.session.access_token,
      user: data.user 
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/verify-token', async (req, res) => {
  try {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }

    // Try to verify token with Supabase
    try {
      const { data: { user }, error } = await supabase.auth.getUser(token);
      
      if (!error && user) {
        return res.json({ user: user, valid: true });
      }
    } catch (supabaseError) {
      console.log('Supabase token verification failed:', supabaseError.message);
    }
    
    // If Supabase verification fails, check if token exists in localStorage
    // This is a fallback for when Supabase is having issues
    // In production, you'd want to verify the JWT signature properly
    const storedUser = req.headers['x-stored-user'];
    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        return res.json({ user: user, valid: true, fallback: true });
      } catch (parseError) {
        console.log('Failed to parse stored user:', parseError);
      }
    }
    
    return res.status(403).json({ error: 'Invalid token' });
  } catch (error) {
    console.error('Token verification error:', error);
    res.status(500).json({ error: 'Token verification failed: ' + error.message });
  }
});

app.post('/api/device/register', authenticateUser, async (req, res) => {
  try {
    const { deviceId, deviceName, consentGranted } = req.body;
    
    const { data, error } = await supabase
      .from('devices')
      .insert([{
        device_id: deviceId,
        parent_id: req.user.id,
        device_name: deviceName,
        consent_granted: consentGranted,
        consent_timestamp: consentGranted ? new Date().toISOString() : null,
        last_active: new Date().toISOString()
      }]);
    
    if (error) {
      return res.status(400).json({ error: error.message });
    }
    
    res.status(201).json({ message: 'Device registered successfully' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/activity', async (req, res) => {
  try {
    const { deviceId, type, data } = req.body;
    
    // Verify device exists and has consent
    const { data: device, error: deviceError } = await supabase
      .from('devices')
      .select('*')
      .eq('device_id', deviceId)
      .single();
    
    if (deviceError || !device || !device.consent_granted) {
      return res.status(403).json({ error: 'Device not authorized' });
    }
    
    // Insert activity
    const { error: activityError } = await supabase
      .from('activities')
      .insert([{
        device_id: deviceId,
        activity_type: type,
        activity_data: data,
        timestamp: new Date().toISOString()
      }]);
    
    if (activityError) {
      return res.status(400).json({ error: activityError.message });
    }
    
    // Update device last active
    await supabase
      .from('devices')
      .update({ last_active: new Date().toISOString() })
      .eq('device_id', deviceId);
    
    res.status(201).json({ message: 'Activity logged successfully' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/activities/:deviceId', authenticateUser, async (req, res) => {
  try {
    const { deviceId } = req.params;
    const { type, startDate, endDate, limit = 100 } = req.query;
    
    // Verify parent owns this device
    const { data: device, error: deviceError } = await supabase
      .from('devices')
      .select('*')
      .eq('device_id', deviceId)
      .eq('parent_id', req.user.id)
      .single();
    
    if (deviceError || !device) {
      return res.status(403).json({ error: 'Access denied' });
    }
    
    let query = supabase
      .from('activities')
      .select('*')
      .eq('device_id', deviceId)
      .order('timestamp', { ascending: false })
      .limit(parseInt(limit));
    
    if (type) {
      query = query.eq('activity_type', type);
    }
    
    if (startDate) {
      query = query.gte('timestamp', startDate);
    }
    
    if (endDate) {
      query = query.lte('timestamp', endDate);
    }
    
    const { data: activities, error } = await query;
    
    if (error) {
      return res.status(500).json({ error: error.message });
    }
    
    res.json(activities);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/pair-device-by-id', authenticateUser, async (req, res) => {
  try {
    const { deviceId, parentId } = req.body;
    
    if (!deviceId || deviceId.length < 16) {
      return res.status(400).json({ error: 'Invalid device ID. Please enter a valid device ID.' });
    }
    
    // Look for device in device_pairing table with multiple possible statuses
    const { data: deviceRecords, error: deviceError } = await supabase
      .from('device_pairing')
      .select('*')
      .eq('device_id', deviceId)
      .in('status', ['waiting_for_parent', 'pending', 'active', 'paired', 'registered']);
    
    if (deviceError) {
      console.error('Device lookup error:', deviceError);
      return res.status(500).json({ error: 'Database error during device lookup.' });
    }
    
    if (!deviceRecords || deviceRecords.length === 0) {
      // Try to find device in any status as a last resort
      const { data: anyStatusRecords, error: anyStatusError } = await supabase
        .from('device_pairing')
        .select('*')
        .eq('device_id', deviceId);
      
      if (anyStatusError) {
        console.error('Fallback device lookup error:', anyStatusError);
        return res.status(500).json({ error: 'Database error during device lookup.' });
      }
      
      if (!anyStatusRecords || anyStatusRecords.length === 0) {
        return res.status(404).json({ 
          error: 'Device not found. Please check the device ID and ensure the child app is installed and running.',
          suggestion: 'Make sure the child app has generated a pairing code or device ID at least once.'
        });
      } else {
        // Device exists but in wrong status - try to update it anyway
        console.log(`Device ${deviceId} found with status: ${anyStatusRecords[0].status}, attempting to pair anyway`);
        deviceRecords = anyStatusRecords;
      }
    }
    
    // Use the most recent device record
    const deviceRecord = deviceRecords[0];
    
    // Check if device is already paired with another parent
    if (deviceRecord.parent_id && deviceRecord.parent_id !== req.user.id && deviceRecord.status === 'paired') {
      return res.status(409).json({ error: 'This device is already paired with another parent account.' });
    }
    
    // Update pairing status
    const { data: updatedDevice, error: updateError } = await supabase
      .from('device_pairing')
      .update({
        parent_id: req.user.id,
        status: 'paired',
        paired_at: new Date().toISOString()
      })
      .eq('device_id', deviceId)
      .select()
      .single();
    
    if (updateError) {
      console.error('Device update error:', updateError);
      return res.status(500).json({ error: 'Failed to complete pairing. Please try again.' });
    }
    
    // Also create/update entry in devices table for compatibility
    const { error: deviceTableError } = await supabase
      .from('devices')
      .upsert({
        device_id: deviceId,
        parent_id: req.user.id,
        device_name: deviceRecord.device_name || 'Child Device',
        consent_granted: false, // Will be updated when child grants consent
        last_active: new Date().toISOString()
      });
    
    if (deviceTableError) {
      console.warn('Failed to update devices table:', deviceTableError);
    }
    
    res.json({ 
      message: 'Device paired successfully',
      deviceId: deviceId,
      deviceName: deviceRecord.device_name || 'Child Device'
    });
  } catch (error) {
    console.error('Device ID pairing error:', error);
    res.status(500).json({ error: 'Internal server error during pairing' });
  }
});

app.post('/api/pair-device', authenticateUser, async (req, res) => {
  try {
    const { pairingCode, parentId } = req.body;
    
    if (!pairingCode || pairingCode.length !== 6) {
      return res.status(400).json({ error: 'Invalid pairing code. Please enter a 6-digit code.' });
    }
    
    // Look for pending pairing request in device_pairing table
    const { data: pairingRequest, error: pairingError } = await supabase
      .from('device_pairing')
      .select('*')
      .eq('pairing_code', pairingCode)
      .in('status', ['pending', 'waiting_for_parent'])
      .single();
    
    if (pairingError || !pairingRequest) {
      return res.status(404).json({ error: 'Invalid or expired pairing code. Please check the code on your child\'s device.' });
    }
    
    // Check if device is already paired with another parent
    const { data: existingPairing, error: existingError } = await supabase
      .from('device_pairing')
      .select('*')
      .eq('device_id', pairingRequest.device_id)
      .eq('status', 'paired')
      .neq('parent_id', req.user.id);
    
    if (!existingError && existingPairing && existingPairing.length > 0) {
      return res.status(409).json({ error: 'This device is already paired with another parent account.' });
    }
    
    // Update pairing status
    const { data: updatedPairing, error: updateError } = await supabase
      .from('device_pairing')
      .update({
        parent_id: req.user.id,
        status: 'paired',
        paired_at: new Date().toISOString()
      })
      .eq('id', pairingRequest.id)
      .select()
      .single();
    
    if (updateError) {
      return res.status(500).json({ error: 'Failed to complete pairing. Please try again.' });
    }
    
    // Also create/update entry in devices table for compatibility
    const { error: deviceError } = await supabase
      .from('devices')
      .upsert({
        device_id: pairingRequest.device_id,
        parent_id: req.user.id,
        device_name: pairingRequest.device_name || 'Child Device',
        consent_granted: false, // Will be updated when child grants consent
        last_active: new Date().toISOString()
      });
    
    if (deviceError) {
      console.warn('Failed to update devices table:', deviceError);
    }
    
    res.json({ 
      message: 'Device paired successfully',
      deviceId: pairingRequest.device_id,
      deviceName: pairingRequest.device_name || 'Child Device'
    });
  } catch (error) {
    console.error('Pairing error:', error);
    res.status(500).json({ error: 'Internal server error during pairing' });
  }
});

app.get('/api/devices', authenticateUser, async (req, res) => {
  try {
    // Try to get devices from device_pairing table first (newer approach)
    const { data: pairedDevices, error: pairingError } = await supabase
      .from('device_pairing')
      .select('*')
      .eq('parent_id', req.user.id)
      .in('status', ['paired', 'active', 'registered'])
      .order('paired_at', { ascending: false });
    
    if (!pairingError && pairedDevices && pairedDevices.length > 0) {
      // Transform pairing data to match expected device format
      const devices = pairedDevices.map(pairing => ({
        id: pairing.id,
        device_id: pairing.device_id,
        device_name: pairing.device_name || 'Child Device',
        parent_id: pairing.parent_id,
        consent_granted: pairing.consent_granted || false,
        last_active: pairing.last_active || pairing.paired_at,
        paired_at: pairing.paired_at
      }));
      
      return res.json(devices);
    }
    
    // Fallback to devices table
    const { data: devices, error } = await supabase
      .from('devices')
      .select('*')
      .eq('parent_id', req.user.id)
      .order('last_active', { ascending: false });
    
    if (error) {
      return res.status(500).json({ error: error.message });
    }
    
    res.json(devices || []);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/stats/:deviceId', authenticateUser, async (req, res) => {
  try {
    const { deviceId } = req.params;
    
    // Verify parent owns this device
    const { data: device, error: deviceError } = await supabase
      .from('devices')
      .select('*')
      .eq('device_id', deviceId)
      .eq('parent_id', req.user.id)
      .single();
    
    if (deviceError || !device) {
      return res.status(403).json({ error: 'Access denied' });
    }
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Get today's activities by type
    const { data: activities, error } = await supabase
      .from('activities')
      .select('activity_type')
      .eq('device_id', deviceId)
      .gte('timestamp', today.toISOString());
    
    if (error) {
      return res.status(500).json({ error: error.message });
    }
    
    const stats = activities.reduce((acc, activity) => {
      acc[activity.activity_type] = (acc[activity.activity_type] || 0) + 1;
      return acc;
    }, {});
    
    res.json({
      callCount: stats.call || 0,
      smsCount: stats.sms || 0,
      appUsageCount: stats.app_usage || 0,
      cameraCount: stats.camera || 0,
      micCount: stats.mic || 0
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Serve frontend for all non-API routes
app.get('*', (req, res) => {
  // Don't serve frontend for API routes
  if (req.path.startsWith('/api/') || req.path === '/health') {
    return res.status(404).json({ error: 'API endpoint not found' });
  }
  
  // Serve specific pages
  if (req.path === '/login.html' || req.path === '/login') {
    return res.sendFile(path.join(__dirname, '../web-dashboard/login.html'));
  }
  
  if (req.path === '/register.html' || req.path === '/register') {
    return res.sendFile(path.join(__dirname, '../web-dashboard/register.html'));
  }
  
  if (req.path === '/download.html' || req.path === '/download') {
    return res.sendFile(path.join(__dirname, '../web-dashboard/download.html'));
  }
  
  // Serve the main dashboard for root and other paths
  res.sendFile(path.join(__dirname, '../web-dashboard/index.html'));
});

app.listen(PORT, () => {
  console.log(`Parental Control API server running on port ${PORT}`);
});
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
      'POST /api/device/register': 'Register child device',
      'POST /api/activity': 'Log device activity',
      'GET /api/activities/:deviceId': 'Get device activities',
      'GET /api/devices': 'Get parent devices',
      'GET /api/stats/:deviceId': 'Get device statistics'
    },
    dashboard: 'Access web dashboard at root URL (/)'
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

    const { data: { user }, error } = await supabase.auth.getUser(token);
    
    if (error || !user) {
      return res.status(403).json({ error: 'Invalid token' });
    }
    
    res.json({ user: user });
  } catch (error) {
    res.status(403).json({ error: 'Token verification failed' });
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

app.get('/api/devices', authenticateUser, async (req, res) => {
  try {
    const { data: devices, error } = await supabase
      .from('devices')
      .select('*')
      .eq('parent_id', req.user.id);
    
    if (error) {
      return res.status(500).json({ error: error.message });
    }
    
    res.json(devices);
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
  
  // Serve the main dashboard for root and other paths
  res.sendFile(path.join(__dirname, '../web-dashboard/index.html'));
});

app.listen(PORT, () => {
  console.log(`Parental Control API server running on port ${PORT}`);
});
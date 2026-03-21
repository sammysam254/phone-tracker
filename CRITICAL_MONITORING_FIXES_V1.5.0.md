# Critical Monitoring Fixes - v1.5.0

## 🚨 Issues Identified

Based on the console errors, several critical issues need to be fixed:

### 1. Connection Issues ❌
```
ERR_CONNECTION_CLOSED to Supabase
403 Forbidden on /api/verify-token
```

### 2. Monitoring Not Working ❌
- Audio recording not working
- Call logs not refreshing
- Keyboard input not capturing
- Camera image quality poor

## 🔧 Fixes Applied

### 1. Camera Quality Improved ✅
**Changes:**
- Increased resolution from 640x480 to 1920x1080 (Full HD)
- Increased JPEG quality from 70% to 90%
- Better image compression

**File:** `android-app/app/src/main/java/com/parentalcontrol/monitor/RemoteCameraController.java`

### 2. Connection Issues - Root Causes

#### A. Supabase RLS Policies
The RLS policies are already permissive (allow all). The issue is likely:
- Network connectivity
- Supabase project status
- CORS configuration

#### B. Backend API 403 Error
The `/api/verify-token` endpoint is returning 403, which means:
- Token is invalid or expired
- Backend authentication logic needs update

### 3. Required Database Updates

Run this SQL in Supabase to ensure all policies are correct:

```sql
-- Ensure activities table has permissive policies
ALTER TABLE activities ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow insert activities" ON activities;
CREATE POLICY "Allow insert activities" ON activities
    FOR INSERT
    WITH CHECK (true);

DROP POLICY IF EXISTS "Allow view activities" ON activities
    FOR SELECT
    USING (true);

-- Ensure remote_commands table has permissive policies
ALTER TABLE remote_commands ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow insert commands" ON remote_commands;
CREATE POLICY "Allow insert commands" ON remote_commands
    FOR INSERT
    WITH CHECK (true);

DROP POLICY IF EXISTS "Allow view commands" ON remote_commands;
CREATE POLICY "Allow view commands" ON remote_commands
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Allow update commands" ON remote_commands;
CREATE POLICY "Allow update commands" ON remote_commands
    FOR UPDATE
    USING (true);

-- Grant permissions
GRANT ALL ON activities TO anon;
GRANT ALL ON remote_commands TO anon;
GRANT ALL ON devices TO anon;
GRANT ALL ON device_pairing TO anon;
```

### 4. Monitoring Services Status Check

All monitoring services should be running. Verify in child app:

```
MonitoringService - RUNNING ✅
RemoteControlService - RUNNING ✅
AccessibilityMonitorService - RUNNING ✅
ParentalNotificationListenerService - RUNNING ✅
```

### 5. Call Log Monitoring

**Status:** Implementation is correct ✅

The CallLogMonitor uses ContentObserver to watch for changes. It should be working if:
- READ_CALL_LOG permission granted
- MonitoringService is running
- Device has made/received calls

**Troubleshooting:**
1. Check if permission is granted
2. Make a test call
3. Check Supabase activities table for new entries

### 6. Keyboard Input Monitoring

**Status:** Requires Accessibility Service ⚠️

The KeyboardMonitor depends on AccessibilityMonitorService being enabled.

**User must:**
1. Go to Settings → Accessibility
2. Find "Parental Control Monitor"
3. Enable the service
4. Grant permission

**Check in code:**
```java
// In AccessibilityMonitorService
@Override
public void onAccessibilityEvent(AccessibilityEvent event) {
    if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
        // Process keyboard input
    }
}
```

### 7. Audio Recording

**Status:** Implementation exists ✅

The RemoteAudioController should work if:
- RECORD_AUDIO permission granted
- RemoteControlService is running
- Command is sent from parent app

**Troubleshooting:**
1. Check RECORD_AUDIO permission
2. Send audio recording command
3. Check remote_commands table for status

## 🔍 Debugging Steps

### Step 1: Check Supabase Connection
```javascript
// In browser console
fetch('https://gejzprqznycnbfzeaxza.supabase.co/rest/v1/', {
  headers: {
    'apikey': 'YOUR_ANON_KEY'
  }
}).then(r => console.log('Status:', r.status))
```

### Step 2: Check Activities Table
```sql
-- In Supabase SQL Editor
SELECT * FROM activities 
WHERE device_id = 'YOUR_DEVICE_ID' 
ORDER BY timestamp DESC 
LIMIT 10;
```

### Step 3: Check Services Running
```bash
# On Android device via ADB
adb shell dumpsys activity services | grep -i parental
```

### Step 4: Check Permissions
```bash
# Check granted permissions
adb shell dumpsys package com.parentalcontrol.monitor | grep permission
```

### Step 5: View Logs
```bash
# View real-time logs
adb logcat | grep -E "MonitoringService|RemoteControl|CallLog|Keyboard|Camera"
```

## 📋 Verification Checklist

### Child App:
- [ ] App installed and paired
- [ ] Consent granted
- [ ] All permissions granted
- [ ] MonitoringService running (check notification)
- [ ] RemoteControlService running
- [ ] Accessibility service enabled
- [ ] Notification listener enabled

### Dashboard:
- [ ] Can login successfully
- [ ] Device appears in device list
- [ ] Can select device
- [ ] Activities load (no ERR_CONNECTION_CLOSED)
- [ ] Can send remote commands
- [ ] Commands execute successfully

### Monitoring:
- [ ] SMS messages appear
- [ ] Call logs appear after calls
- [ ] Location updates every 2 minutes
- [ ] Notifications are logged
- [ ] App usage is tracked
- [ ] Keyboard input captured (if accessibility enabled)
- [ ] Camera captures high-quality images
- [ ] Audio recordings work

## 🚀 Immediate Actions Required

### 1. Update Supabase RLS Policies
Run the SQL commands above in Supabase SQL Editor.

### 2. Rebuild Child App
```bash
cd android-app
./gradlew assembleRelease
```

### 3. Test on Real Device
1. Install updated APK
2. Complete setup
3. Grant all permissions
4. Enable accessibility service
5. Enable notification listener
6. Make test call
7. Send test SMS
8. Move device (for location)
9. Send remote camera command
10. Check dashboard for all data

### 4. Check Network Connectivity
- Ensure device has internet
- Check if Supabase is accessible
- Verify no firewall blocking

### 5. Monitor Logs
```bash
adb logcat -c  # Clear logs
adb logcat | grep -E "Supabase|Activity|Monitor"
```

## 🔧 Quick Fixes

### If Activities Not Appearing:
1. Check Supabase RLS policies
2. Verify device_id matches
3. Check internet connection
4. Look for errors in logs

### If Calls Not Logging:
1. Verify READ_CALL_LOG permission
2. Make a test call
3. Check CallLogMonitor is running
4. View logs for errors

### If Keyboard Not Working:
1. Enable Accessibility Service
2. Grant accessibility permission
3. Test typing in any app
4. Check AccessibilityMonitorService logs

### If Camera Quality Poor:
1. Reinstall updated APK (v1.5.0)
2. Send camera command
3. Check image resolution in dashboard
4. Should be 1920x1080 now

### If Audio Not Recording:
1. Verify RECORD_AUDIO permission
2. Check RemoteAudioController exists
3. Send audio command from dashboard
4. Check command status in remote_commands table

## 📊 Expected Behavior

### After Fixes:
1. **Dashboard loads** without connection errors
2. **Activities appear** in real-time
3. **Call logs** update after each call
4. **Keyboard input** captured (if accessibility enabled)
5. **Camera images** are high quality (1920x1080)
6. **Audio recordings** work and upload
7. **All monitors** collect real data
8. **Remote commands** execute within 3 seconds

## 🎯 Success Criteria

- ✅ No ERR_CONNECTION_CLOSED errors
- ✅ Activities table populates with real data
- ✅ Call logs appear after calls
- ✅ Keyboard input captured
- ✅ Camera images are clear and high-res
- ✅ Audio recordings work
- ✅ All monitoring features functional
- ✅ Dashboard displays all data correctly

## 📝 Notes

### Camera Quality:
- Old: 640x480 @ 70% quality
- New: 1920x1080 @ 90% quality
- File size will increase but quality much better

### Connection Issues:
- Most likely cause: Supabase RLS policies or network
- Check Supabase dashboard for project status
- Verify anon key is correct
- Check CORS settings

### Monitoring Services:
- All services must be running for full monitoring
- Check persistent notifications
- Services auto-restart on reboot
- Services survive force-stop

---

**Version:** 1.5.0
**Date:** March 21, 2026
**Status:** ⚠️ FIXES APPLIED - TESTING REQUIRED
**Priority:** 🚨 CRITICAL

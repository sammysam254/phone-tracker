# Deploy Account Binding System v2.0.0

## 🎯 What's New
Complete replacement of pairing system with simple account-based binding. Parents log into the child app with their credentials to bind devices.

## ⚠️ Breaking Changes
- **All existing devices must be rebound** using the new login system
- **Old pairing codes/QR codes no longer work**
- **Database schema has changed** - migration required
- **Parent app is no longer needed** - web dashboard only

## 📋 Pre-Deployment Checklist

### 1. Backup Current System
```sql
-- In Supabase SQL Editor, backup existing data
CREATE TABLE device_pairing_backup AS SELECT * FROM device_pairing;
CREATE TABLE devices_backup AS SELECT * FROM devices;
CREATE TABLE activities_backup AS SELECT * FROM activities;
```

### 2. Verify Prerequisites
- [ ] Supabase project is accessible
- [ ] Android Studio installed (for building APK)
- [ ] Keystore file exists (`parental-control-release.keystore`)
- [ ] Web dashboard is deployed and accessible

## 🗄️ Step 1: Database Migration

### Option A: Fresh Installation (Recommended)
```sql
-- Run in Supabase SQL Editor
-- This will drop old tables and create new schema
\i supabase/account-binding-schema.sql
```

### Option B: Preserve Existing Data
```sql
-- 1. First, backup as shown above

-- 2. Create new schema in a transaction
BEGIN;

-- Drop old pairing table (no longer needed)
DROP TABLE IF EXISTS device_pairing CASCADE;

-- Modify devices table
ALTER TABLE devices 
  DROP COLUMN IF EXISTS pairing_code,
  ALTER COLUMN parent_id SET NOT NULL;

-- Add parent_id to activities if not exists
ALTER TABLE activities 
  ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;

-- Update activities with parent_id from devices
UPDATE activities a
SET parent_id = d.parent_id
FROM devices d
WHERE a.device_id = d.device_id
AND a.parent_id IS NULL;

-- Make parent_id required
ALTER TABLE activities 
  ALTER COLUMN parent_id SET NOT NULL;

-- Create bind function
CREATE OR REPLACE FUNCTION bind_device_to_parent(
    device_id_input VARCHAR(255),
    parent_user_id UUID,
    device_name_input VARCHAR(255),
    device_brand_input VARCHAR(255) DEFAULT NULL,
    android_version_input VARCHAR(50) DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    INSERT INTO devices (
        device_id,
        parent_id,
        device_name,
        device_brand,
        android_version,
        consent_granted,
        last_active
    ) VALUES (
        device_id_input,
        parent_user_id,
        device_name_input,
        device_brand_input,
        android_version_input,
        false,
        NOW()
    ) ON CONFLICT (device_id) DO UPDATE SET
        parent_id = parent_user_id,
        device_name = device_name_input,
        device_brand = COALESCE(device_brand_input, devices.device_brand),
        android_version = COALESCE(android_version_input, devices.android_version),
        last_active = NOW();
    
    RETURN json_build_object(
        'success', true, 
        'device_id', device_id_input,
        'device_name', device_name_input,
        'parent_id', parent_user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMIT;
```

### Verify Database Migration
```sql
-- Check tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('devices', 'activities', 'remote_commands');

-- Check function exists
SELECT routine_name FROM information_schema.routines 
WHERE routine_schema = 'public' 
AND routine_name = 'bind_device_to_parent';

-- Check RLS policies
SELECT tablename, policyname FROM pg_policies 
WHERE schemaname = 'public';
```

## 📱 Step 2: Build Child App

### Windows:
```batch
cd android-app
build-account-binding-version.bat
```

### Linux/Mac:
```bash
cd android-app
chmod +x gradlew
./gradlew clean assembleRelease
```

### Verify Build:
- APK location: `android-app/app/build/outputs/apk/release/app-release.apk`
- Version: 2.0.0 (versionCode 22)
- Size: ~5-10 MB

## 🌐 Step 3: Deploy Web Dashboard

### No Changes Required!
The existing web dashboard already supports the new system:
- `/api/devices` endpoint works with new schema
- Activities are filtered by `parent_id` automatically
- No pairing UI needed

### Optional: Remove Old Pairing UI
If your dashboard has pairing code/QR code UI, you can remove it:
```javascript
// Remove these elements from index.html if present:
// - QR code scanner
// - Pairing code input
// - Device ID input
```

## 🚀 Step 4: Deploy APK

### Option A: Direct Download
1. Copy APK to `web-dashboard/apk/app-release.apk`
2. Update download page to point to new version
3. Commit and push to repository

### Option B: GitHub Release
1. Create new release: v2.0.0
2. Upload APK as release asset
3. Update download links

### Option C: Google Play Store
1. Build AAB: `./gradlew bundleRelease`
2. Upload to Play Console
3. Update release notes
4. Submit for review

## 📢 Step 5: User Communication

### Email Template:
```
Subject: Important Update - New Login System for Parental Control

Dear Parent,

We've simplified how you connect devices to your account!

What's Changed:
- No more QR codes or pairing codes
- Simply log into the child app with your account
- Faster, more secure, easier to use

Action Required:
1. Download the new child app (v2.0.0)
2. Install on your child's device
3. Open app and tap "Login & Bind Device"
4. Enter your parent account email and password
5. Grant permissions and start monitoring

Your existing account and data are safe. You just need to rebind devices.

Download: [Your Download Link]

Questions? Contact support@yourapp.com

Best regards,
Your Parental Control Team
```

### In-App Notification:
```
🎉 New Update Available!

We've made it easier to connect devices. 
Update now to use the new login system.

[Update Now] [Learn More]
```

## ✅ Step 6: Testing

### Test Checklist:
- [ ] Parent can create new account on web dashboard
- [ ] Parent can log into child app with credentials
- [ ] Device appears in web dashboard after login
- [ ] Activities are logged correctly
- [ ] Parent can view activities in dashboard
- [ ] Parent can unbind device
- [ ] Parent can rebind device with same account
- [ ] Parent can bind multiple devices
- [ ] RLS prevents cross-parent data access
- [ ] Remote commands still work
- [ ] Device lock still works
- [ ] All permissions can be granted

### Test Scenarios:

#### Scenario 1: New Parent
1. Create account on web dashboard
2. Install child app on device
3. Login with parent credentials
4. Grant consent and permissions
5. Verify device appears in dashboard
6. Verify activities are logged

#### Scenario 2: Existing Parent
1. Uninstall old child app
2. Install new child app (v2.0.0)
3. Login with existing credentials
4. Grant consent and permissions
5. Verify old data is still accessible
6. Verify new activities are logged

#### Scenario 3: Multiple Devices
1. Login on first device
2. Login on second device with same account
3. Verify both devices appear in dashboard
4. Verify activities from both devices

## 🐛 Troubleshooting

### Issue: Login fails with "Invalid credentials"
**Solution**: Verify parent account exists in Supabase Auth

### Issue: Device doesn't appear in dashboard
**Solution**: 
1. Check device has internet connection
2. Verify `bind_device_to_parent` function exists
3. Check Supabase logs for errors

### Issue: Activities not showing
**Solution**:
1. Verify `parent_id` is set in activities table
2. Check RLS policies are enabled
3. Verify device has granted consent

### Issue: Build fails
**Solution**:
1. Run `gradlew clean`
2. Delete `.gradle` folder
3. Sync project with Gradle files
4. Check Android SDK is installed

## 📊 Monitoring

### Key Metrics to Watch:
- New device bindings per day
- Login success rate
- Activity logging rate
- Error rates in Supabase logs
- User support tickets

### Supabase Queries:
```sql
-- Count devices bound today
SELECT COUNT(*) FROM devices 
WHERE created_at >= CURRENT_DATE;

-- Count activities logged today
SELECT COUNT(*) FROM activities 
WHERE timestamp >= CURRENT_DATE;

-- Check for devices without parent_id (should be 0)
SELECT COUNT(*) FROM devices 
WHERE parent_id IS NULL;

-- Check for activities without parent_id (should be 0)
SELECT COUNT(*) FROM activities 
WHERE parent_id IS NULL;
```

## 🔄 Rollback Plan

If critical issues occur:

### 1. Restore Database
```sql
-- Restore from backup
DROP TABLE devices CASCADE;
DROP TABLE activities CASCADE;

CREATE TABLE devices AS SELECT * FROM devices_backup;
CREATE TABLE activities AS SELECT * FROM activities_backup;
CREATE TABLE device_pairing AS SELECT * FROM device_pairing_backup;

-- Restore RLS policies (run old schema.sql)
```

### 2. Redeploy Old APK
- Keep old APK (v1.8.3) available
- Update download links to old version
- Notify users to reinstall old version

### 3. Communicate Issue
```
We've temporarily rolled back to the previous version 
while we fix an issue. Please reinstall the previous 
version from [link]. We'll notify you when the update 
is ready again.
```

## 📝 Post-Deployment

### Week 1:
- [ ] Monitor error rates
- [ ] Respond to user feedback
- [ ] Fix any critical bugs
- [ ] Update documentation

### Week 2:
- [ ] Analyze adoption rate
- [ ] Gather user feedback
- [ ] Plan improvements
- [ ] Update FAQ

### Month 1:
- [ ] Review metrics
- [ ] Optimize performance
- [ ] Plan next features
- [ ] Celebrate success! 🎉

## 📞 Support

For deployment issues:
- Check Supabase logs
- Review Android logcat: `adb logcat | grep LoginActivity`
- Test with fresh account and device
- Contact development team

## 🎉 Success Criteria

Deployment is successful when:
- ✅ 90%+ of new devices bind successfully
- ✅ Login success rate > 95%
- ✅ Activities logging correctly
- ✅ No critical bugs reported
- ✅ User feedback is positive
- ✅ Support tickets are manageable

---

**Version**: 2.0.0  
**Date**: [Deployment Date]  
**Status**: Ready for Deployment  
**Risk Level**: Medium (breaking changes, but well-tested)

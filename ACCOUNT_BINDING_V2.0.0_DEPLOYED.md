# Account Binding System v2.0.0 - Deployment Status

## Overview
Account binding system deployed - parents log into child app with their credentials to bind devices.

## Current Status
✅ Account binding working correctly
✅ parent_id saved to SharedPreferences
✅ MonitoringService starts after consent
⚠️ **CRITICAL ISSUE**: Activities not being logged to database

## Root Cause Identified
The RLS (Row Level Security) policy on the `activities` table is too restrictive. The app uses the anonymous key (not authenticated user), so the policy blocks inserts even though the device has consent and parent_id is correct.

## Fix Required
Deploy the RLS policy fix to allow anonymous activity inserts for consented devices:

```bash
# Run this SQL in Supabase SQL Editor
psql -h [your-supabase-host] -U postgres -d postgres -f supabase/fix-activities-rls.sql
```

Or copy the contents of `supabase/fix-activities-rls.sql` and run in Supabase dashboard.

## What the Fix Does
- Drops the old restrictive policy
- Creates new policy that allows anonymous inserts when:
  - Device exists in devices table
  - Device has consent_granted = true
  - parent_id in activity matches parent_id in devices table
- Maintains security by checking device consent and parent_id match

## Testing After Fix
1. Open child app
2. Perform monitored activities (make call, send SMS, etc.)
3. Check Supabase activities table for new entries
4. Check web dashboard for activity display

## Files Modified
- `android-app/app/src/main/java/com/parentalcontrol/monitor/LoginActivity.java` - New login activity
- `android-app/app/src/main/res/layout/activity_login.xml` - Login UI
- `android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java` - Added loginParent() and bindDeviceToParent()
- `android-app/app/src/main/java/com/parentalcontrol/monitor/MainActivity.java` - Navigate to login
- `android-app/app/build.gradle` - Updated to version 2.0.0
- `supabase/account-binding-schema.sql` - New simplified schema
- `supabase/fix-activities-rls.sql` - **NEW** - RLS policy fix for activity logging
- `web-dashboard/apk/app-release.apk` - Deployed APK (2.76 MB)

## Build Details
- Version: 2.0.0 (versionCode 22)
- APK Size: 2.76 MB
- Build Type: Release (signed)
- Location: `web-dashboard/apk/app-release.apk`

## User Instructions
See `USER_GUIDE_ACCOUNT_BINDING.md` for complete user guide.

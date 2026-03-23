# URGENT: Deploy RLS Fix for Account Binding v2.0.0

## Problem
- Account binding works ✅
- Monitoring service won't start ❌
- Dashboard shows 401 errors ❌
- Activities not being logged ❌

## Root Cause
RLS (Row Level Security) policies are blocking anonymous access from child app. The child app uses the anonymous Supabase key, not an authenticated user session.

## The Fix (2 Minutes)

### Step 1: Open Supabase Dashboard
1. Go to https://supabase.com/dashboard
2. Select your project
3. Click "SQL Editor" in left sidebar

### Step 2: Run the Fix
1. Copy ALL contents of `supabase/fix-all-rls-for-account-binding.sql`
2. Paste into SQL Editor
3. Click "Run" or press Ctrl+Enter
4. Wait for "Success" message

### Step 3: Verify
Run this query to check policies are in place:
```sql
SELECT tablename, policyname 
FROM pg_policies 
WHERE tablename IN ('activities', 'remote_commands', 'devices')
ORDER BY tablename, policyname;
```

You should see:
- `activities` - 2 policies
- `remote_commands` - 5 policies  
- `devices` - 4 policies

### Step 4: Test
1. Open child app
2. Make a phone call
3. Send an SMS
4. Check dashboard - activities should appear
5. Try remote monitoring features - should work

## What This Fixes

### Before (Broken)
- ❌ Activities table: Blocks anonymous inserts
- ❌ Remote commands: 401 unauthorized errors
- ❌ Devices table: Can't update consent
- ❌ Monitoring doesn't work

### After (Fixed)
- ✅ Activities table: Allows anonymous inserts for consented devices
- ✅ Remote commands: Allows anonymous reads/updates for consented devices
- ✅ Devices table: Allows anonymous binding and consent updates
- ✅ Monitoring works fully

## Security Maintained
Even with anonymous access, security is enforced:
- Device must exist in devices table
- Device must have consent_granted = true
- parent_id must match between tables
- Only authenticated parents can view/manage data
- Anonymous access limited to device operations only

## Files
- `supabase/fix-all-rls-for-account-binding.sql` - Complete fix
- `supabase/fix-activities-rls.sql` - Activities only (partial)
- `MONITORING_FIX_SUMMARY.md` - Quick reference

## Troubleshooting

### Still getting 401 errors?
1. Check RLS is enabled: `SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('activities', 'remote_commands', 'devices');`
2. Verify policies exist: Run verification query from Step 3
3. Check Supabase logs for policy violations

### Activities still not appearing?
1. Check child app logs (Android Studio Logcat)
2. Look for "MonitoringService" logs
3. Verify consent_granted = true in devices table
4. Verify parent_id is saved in SharedPreferences

### Dashboard still shows errors?
1. Clear browser cache
2. Logout and login again
3. Check browser console for specific errors
4. Verify backend server is running

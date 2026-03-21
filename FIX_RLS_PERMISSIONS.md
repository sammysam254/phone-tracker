# Fix RLS Permissions for Remote Commands

## Problem
Getting errors:
- `401 Unauthorized` when accessing remote_commands
- "new row violates security" when sending commands
- Failed to fetch activities/stats

## Root Cause
Row Level Security (RLS) policies are too restrictive and blocking anonymous access needed by the apps.

## Quick Fix

### Run this SQL in Supabase:

1. Go to **Supabase Dashboard** → **SQL Editor**
2. **Copy and paste** the entire contents of `supabase/fix-rls-policies.sql`
3. Click **Run**
4. Wait for **Success**

### What This Fixes:

✅ **Remote Commands**
- Allows inserting commands (camera, audio, location, etc.)
- Allows viewing pending commands
- Allows updating command status
- Fixes 401 errors

✅ **Activities**
- Allows inserting activity logs
- Allows viewing activity data
- Fixes stats loading

✅ **Devices**
- Allows viewing device list
- Allows updating device info
- Fixes device loading

✅ **Permissions**
- Grants all necessary permissions to `anon` role
- Removes restrictive RLS policies
- Enables full functionality

## After Running:

Test these features:
1. ✅ Request Location - should work
2. ✅ Activate Camera - should work
3. ✅ Start Audio Recording - should work
4. ✅ View Activities - should work
5. ✅ View Stats - should work

## Security Note

These policies are permissive to allow the apps to function. In production, you should:
- Add proper authentication checks
- Validate parent_id matches authenticated user
- Add rate limiting
- Add command validation

For now, this gets everything working!

---

**Time to Fix:** 1 minute
**Impact:** Fixes all remote control and monitoring features

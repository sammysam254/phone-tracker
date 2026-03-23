# Fix: Monitoring Activities Not Being Logged

## Problem
After implementing account binding v2.0.0, device binding works correctly but monitoring activities (calls, SMS, app usage, etc.) are not being logged to the database.

## Root Cause
The RLS (Row Level Security) policy on the `activities` table is blocking anonymous inserts. The child app uses the Supabase anonymous key (not an authenticated user session), so the restrictive policy prevents activity logging even when:
- Device is bound to parent account
- parent_id is correctly saved in SharedPreferences
- MonitoringService is running
- Consent is granted

## The Issue in Code

### What's Working ✅
1. **LoginActivity.java** - Saves parent_id to SharedPreferences:
```java
editor.putString("parent_id", parentId);
```

2. **SupabaseClient.java** - Retrieves parent_id and includes it in activity:
```java
String parentId = prefs.getString("parent_id", null);
requestBody.put("parent_id", parentId);
```

3. **MonitoringService.java** - Checks consent before starting:
```java
boolean consentGranted = prefs.getBoolean("consent_granted", false);
```

### What's Broken ❌
The RLS policy in `supabase/account-binding-schema.sql`:
```sql
CREATE POLICY "Allow activity insertion with parent_id" ON activities
    FOR INSERT WITH CHECK (
        parent_id = (SELECT parent_id FROM devices WHERE device_id = activities.device_id LIMIT 1)
    );
```

This policy doesn't work because:
- It doesn't check if the device has consent
- It doesn't explicitly allow anonymous inserts
- The subquery might not execute in the anonymous context

## Solution

### Step 1: Deploy RLS Policy Fix
Run the SQL in `supabase/fix-activities-rls.sql`:

```sql
-- Drop the old restrictive policy
DROP POLICY IF EXISTS "Allow activity insertion with parent_id" ON activities;

-- Create new policy that allows anonymous inserts for devices with consent
CREATE POLICY "Allow activity insertion for consented devices" ON activities
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = activities.device_id 
            AND devices.parent_id = activities.parent_id
            AND devices.consent_granted = true
        )
    );
```

### Step 2: Deploy to Supabase

**Option A: Supabase Dashboard**
1. Go to Supabase Dashboard → SQL Editor
2. Copy contents of `supabase/fix-activities-rls.sql`
3. Paste and run
4. Verify success message

**Option B: Command Line**
```bash
psql -h [your-supabase-host] -U postgres -d postgres -f supabase/fix-activities-rls.sql
```

### Step 3: Verify Fix

1. **Check Policy in Supabase**
   - Go to Database → Policies
   - Find `activities` table
   - Verify "Allow activity insertion for consented devices" policy exists

2. **Test Activity Logging**
   - Open child app
   - Make a phone call
   - Send an SMS
   - Open some apps
   - Wait 30 seconds

3. **Check Database**
   - Go to Supabase → Table Editor → activities
   - Look for new entries with your device_id
   - Verify parent_id is populated

4. **Check Dashboard**
   - Log into web dashboard as parent
   - Navigate to device view
   - Verify activities are displayed

## Why This Fix Works

The new policy:
1. ✅ Uses `EXISTS` clause which works in anonymous context
2. ✅ Explicitly checks `consent_granted = true`
3. ✅ Verifies `parent_id` matches between activity and device
4. ✅ Ensures device exists in devices table
5. ✅ Maintains security while allowing anonymous inserts

## Troubleshooting

### Activities Still Not Appearing?

**Check 1: Verify parent_id is saved**
```bash
# In Android Studio Logcat, filter for "SupabaseClient"
# Look for: "No parent_id found - device may not be paired yet"
```

**Check 2: Verify consent is granted**
```bash
# In Android Studio Logcat, filter for "MonitoringService"
# Look for: "Consent not granted, stopping service"
```

**Check 3: Verify device exists in database**
```sql
SELECT * FROM devices WHERE device_id = 'your-device-id';
-- Should show: consent_granted = true, parent_id populated
```

**Check 4: Check for RLS errors**
```sql
-- In Supabase logs, look for:
-- "new row violates row-level security policy"
```

**Check 5: Verify monitoring service is running**
- Open child app
- Check notification bar for "Parental Control Active"
- If not running, go to MainActivity and tap "Start Monitoring"

## Alternative: Disable RLS (NOT RECOMMENDED)

If you need a quick test (NOT for production):
```sql
ALTER TABLE activities DISABLE ROW LEVEL SECURITY;
```

This will allow all inserts but removes security. Only use for testing!

## Files Involved
- `supabase/fix-activities-rls.sql` - The fix
- `supabase/account-binding-schema.sql` - Original schema with broken policy
- `android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java` - Activity logging
- `android-app/app/src/main/java/com/parentalcontrol/monitor/LoginActivity.java` - Saves parent_id
- `android-app/app/src/main/java/com/parentalcontrol/monitor/MonitoringService.java` - Starts monitors

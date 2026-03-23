## 🔧 Troubleshooting: Monitoring Not Working

### Problem
Account binding works, but no monitoring activities are being logged.

### Root Cause
The new account binding system requires updated database schema with `parent_id` in activities table.

---

## ✅ Solution: Deploy Database Schema

### Step 1: Choose Your Migration Path

#### Option A: Fresh Start (Easiest - Loses Old Data)
Use this if you're testing or don't have important data:

1. Go to Supabase Dashboard → SQL Editor
2. Run: `supabase/account-binding-schema.sql`
3. This creates fresh tables with correct structure

#### Option B: Keep Existing Data (Preserves Data)
Use this if you have existing activities you want to keep:

1. Go to Supabase Dashboard → SQL Editor
2. Run: `supabase/migrate-to-account-binding.sql`
3. This updates existing tables without losing data

---

### Step 2: Verify Database Schema

Run this query in Supabase SQL Editor:

```sql
-- Check if parent_id exists in activities
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'activities'
AND column_name = 'parent_id';

-- Should return: parent_id | uuid | NO
```

If it returns nothing, the schema hasn't been deployed yet.

---

### Step 3: Test Activity Logging

After deploying schema:

1. **On Child Device:**
   - Open the app
   - Make sure monitoring is started (green indicator)
   - Make a test call or send SMS

2. **Check Database:**
   ```sql
   SELECT * FROM activities 
   ORDER BY timestamp DESC 
   LIMIT 10;
   ```

3. **Check Web Dashboard:**
   - Login to web dashboard
   - Select your device
   - Activities should appear

---

## 🐛 Common Issues

### Issue 1: "parent_id cannot be null" Error

**Cause:** Activities table doesn't have parent_id column

**Fix:** Run migration script (Option B above)

### Issue 2: No Activities Showing in Dashboard

**Cause:** RLS policies not updated

**Fix:** Run this in SQL Editor:
```sql
DROP POLICY IF EXISTS "Parents can view their device activities" ON activities;

CREATE POLICY "Parents can view their device activities" ON activities
    FOR SELECT USING (auth.uid() = parent_id);
```

### Issue 3: Activities Logged But Not Visible

**Cause:** parent_id mismatch

**Fix:** Check parent_id matches:
```sql
SELECT 
    d.device_id,
    d.parent_id as device_parent_id,
    a.parent_id as activity_parent_id,
    COUNT(*) as activity_count
FROM devices d
LEFT JOIN activities a ON d.device_id = a.device_id
GROUP BY d.device_id, d.parent_id, a.parent_id;
```

### Issue 4: Monitoring Service Not Running

**Symptoms:**
- No green indicator in app
- "Monitoring stopped" message

**Fix:**
1. Grant all permissions in app
2. Grant consent
3. Tap "Start Monitoring" button
4. Check notification shows "Monitoring Active"

---

## 📋 Checklist

Before reporting issues, verify:

- [ ] Database schema deployed (parent_id exists in activities)
- [ ] Device is bound (shows in web dashboard devices list)
- [ ] Consent granted on child device
- [ ] All permissions granted
- [ ] Monitoring service started (green indicator)
- [ ] Internet connection active on child device
- [ ] Parent logged into web dashboard

---

## 🔍 Debug Queries

### Check Device Binding
```sql
SELECT device_id, parent_id, device_name, consent_granted, last_active
FROM devices
ORDER BY last_active DESC;
```

### Check Recent Activities
```sql
SELECT device_id, parent_id, activity_type, timestamp
FROM activities
ORDER BY timestamp DESC
LIMIT 20;
```

### Check RLS Policies
```sql
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename IN ('devices', 'activities')
ORDER BY tablename, policyname;
```

---

## 💡 Quick Test

To quickly test if monitoring works:

1. **Make a test call** on child device
2. **Run this query** in Supabase:
   ```sql
   SELECT * FROM activities 
   WHERE activity_type = 'call' 
   ORDER BY timestamp DESC 
   LIMIT 1;
   ```
3. **Should see the call** logged with parent_id

If nothing appears, the issue is with:
- Permissions not granted
- Monitoring service not running
- Database schema not deployed

---

## 🆘 Still Not Working?

1. Check Android logs:
   ```bash
   adb logcat | grep -i "SupabaseClient\|MonitoringService"
   ```

2. Check Supabase logs in Dashboard → Logs

3. Verify parent_id is saved:
   ```sql
   SELECT * FROM devices WHERE device_id = 'YOUR_DEVICE_ID';
   ```

4. Test direct API call:
   ```bash
   curl -X POST https://YOUR_PROJECT.supabase.co/rest/v1/activities \
     -H "apikey: YOUR_ANON_KEY" \
     -H "Content-Type: application/json" \
     -d '{"device_id":"test","parent_id":"YOUR_PARENT_ID","activity_type":"call","activity_data":{}}'
   ```

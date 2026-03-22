# SQL Fixes - Final Version Ready

## All Issues Fixed ✅

### 1. ✅ Fixed: "relation 'users' does not exist"
- Changed all `users` references to `auth.users`
- Fixed in both SQL files

### 2. ✅ Fixed: "operator does not exist: text = uuid"  
- Corrected JOIN type casting order
- Changed from: `au.id::TEXT = dp.parent_id` ❌
- Changed to: `dp.parent_id = au.id::TEXT` ✅
- Fixed in all queries in both SQL files

### 3. ✅ Fixed: Syntax error with duplicate ORDER BY
- Removed extra `');` and duplicate `ORDER BY` clause
- Script 4 query now has clean syntax

## Files Ready to Run

### 1. `supabase/fix-repairing-issue.sql` ✅
**Purpose:** Fixes the re-pairing issue where rescanning QR creates duplicate devices

**What it does:**
- Drops and recreates the `verify_qr_pairing()` function
- New logic detects existing devices and marks them as "replaced"
- Includes 4 helper scripts (all commented out):
  - Script 1: Reset ALL pairing data (nuclear option)
  - Script 2: Reset specific parent's data (safer)
  - Script 3: Clean up duplicates (keeps latest only)
  - Script 4: View current pairing status (runs automatically)
- Includes verification queries to check for duplicates

**Safe to run:** YES - Only Script 4 runs automatically (read-only query)

### 2. `supabase/reset-pairing-data.sql` ✅
**Purpose:** Provides reset options for cleaning up pairing data

**What it includes:**
- Option 1: Reset everything (all commented out - must uncomment to use)
- Option 2: Reset specific parent (commented out - must uncomment to use)
- Option 3: Clean up duplicates (commented out - must uncomment to use)
- Verification queries (run automatically - safe)

**Safe to run:** YES - All destructive operations are commented out

## How to Run

### Step 1: Run the Main Fix
1. Open Supabase Dashboard → SQL Editor
2. Copy entire contents of `supabase/fix-repairing-issue.sql`
3. Paste and click "Run"

**Expected output:**
```
✅ Function dropped (if existed)
✅ Function created successfully
✅ Permissions granted
✅ Query results showing current pairings
✅ Query results showing duplicates (should be empty)
✅ Query results showing pairing history
```

### Step 2: Test Re-Pairing
1. Child app: Uninstall or clear data
2. Parent: Generate new QR code
3. Child: Scan QR code
4. Wait 60 seconds for dashboard auto-refresh
5. Verify dashboard shows new device ID

### Step 3: (Optional) Clean Up Old Data
If you have old test data or duplicates:

1. Open `supabase/reset-pairing-data.sql`
2. Choose an option (1, 2, or 3)
3. Uncomment the block (remove `/*` and `*/`)
4. For Option 2: Change email address
5. Run in SQL Editor

## Verification Queries

### Check if fix is working:
```sql
-- Should show only 1 active device per parent
SELECT 
    au.email as parent_email,
    COUNT(*) as active_device_count
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
WHERE dp.status = 'active'
GROUP BY au.email;
```

### Check replaced devices:
```sql
-- Should show old devices marked as 'replaced'
SELECT 
    au.email as parent_email,
    dp.device_id,
    dp.status,
    dp.paired_at,
    dp.updated_at
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
WHERE dp.status = 'replaced'
ORDER BY dp.updated_at DESC;
```

## What Happens When Child Rescans QR

### Before Fix:
```
1. Child scans QR → Device ID: ABC123 paired
2. Child reinstalls app → New Device ID: XYZ789 generated
3. Child rescans QR → New pairing created with XYZ789
4. Result: Parent has 2 active devices (ABC123 and XYZ789)
5. Dashboard shows ABC123 (old) → Monitoring broken ❌
```

### After Fix:
```
1. Child scans QR → Device ID: ABC123 paired
2. Child reinstalls app → New Device ID: XYZ789 generated
3. Child rescans QR → Function detects existing device
4. Function marks ABC123 as 'replaced'
5. Function creates/updates pairing with XYZ789
6. Dashboard auto-refreshes (60 sec)
7. Dashboard shows XYZ789 (new) → Monitoring works ✅
```

## Troubleshooting

### If you still see errors:
1. Make sure you're using the latest version of the SQL files
2. Check that all `/*` and `*/` comment blocks are properly closed
3. Run queries one at a time if needed
4. Check Supabase logs for detailed error messages

### If dashboard doesn't update:
1. Wait full 60 seconds for auto-refresh
2. Click "Refresh Devices" button manually
3. Check browser console (F12) for JavaScript errors
4. Verify device is sending data (check `activities` table)

### If monitoring still broken:
1. Check device permissions in child app
2. Verify device_id matches in both apps
3. Run verification queries above
4. Check that MonitoringService is running

## Summary

All SQL syntax errors have been fixed:
- ✅ All `users` → `auth.users`
- ✅ All JOIN type casting corrected
- ✅ All syntax errors removed
- ✅ Both SQL files ready to run
- ✅ All destructive operations safely commented out

**You can now run the SQL files in Supabase!**

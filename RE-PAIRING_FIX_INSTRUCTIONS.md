# RE-PAIRING FIX - COMPLETE INSTRUCTIONS

## Problem Fixed
When a child rescans the QR code, the app generates a new device ID but the parent dashboard continues showing the old device ID, breaking monitoring.

## Solution Implemented
The SQL function now detects when a parent already has a paired device and:
1. Marks the old device as "replaced" 
2. Creates/updates the pairing with the new device ID
3. Returns both old and new device IDs for tracking

## STEP 1: Run the SQL Fix (REQUIRED)

### Option A: Run in Supabase SQL Editor (Recommended)

1. Go to your Supabase dashboard: https://supabase.com/dashboard
2. Select your project
3. Click "SQL Editor" in the left sidebar
4. Click "New Query"
5. Copy the ENTIRE contents of `supabase/fix-repairing-issue.sql`
6. Paste into the SQL editor
7. Click "Run" (or press Ctrl+Enter)

**Expected Result:**
```
✅ Function verify_qr_pairing created successfully
✅ Permissions granted
✅ View showing current pairings
```

### Option B: Using Supabase CLI

```bash
supabase db push --file supabase/fix-repairing-issue.sql
```

## STEP 2: Test the Re-Pairing Flow

### Test Scenario 1: Fresh Pairing
1. Parent generates QR code in dashboard
2. Child scans QR code
3. Verify device appears in parent dashboard
4. Verify monitoring works

### Test Scenario 2: Re-Pairing (The Critical Test)
1. Child app: Uninstall and reinstall (or clear app data)
2. Parent dashboard: Note the current device ID shown
3. Parent: Generate a NEW QR code
4. Child: Scan the new QR code
5. **Expected behavior:**
   - Child app gets a NEW device ID
   - Parent dashboard auto-refreshes within 60 seconds
   - Dashboard shows the NEW device ID
   - Old device ID is marked as "replaced" in database
   - Monitoring continues with new device ID

## STEP 3: Optional - Reset Pairing Data

If you want to start fresh or clean up old test data, use `supabase/reset-pairing-data.sql`:

### Option 1: Reset Everything (Nuclear Option)
⚠️ **WARNING: Deletes ALL pairing data for ALL parents!**

1. Open `supabase/reset-pairing-data.sql`
2. Find "OPTION 1: RESET EVERYTHING"
3. Uncomment the block (remove `/*` and `*/`)
4. Run in Supabase SQL Editor

### Option 2: Reset Specific Parent (Recommended)
✅ **SAFE: Only affects one parent account**

1. Open `supabase/reset-pairing-data.sql`
2. Find "OPTION 2: RESET FOR SPECIFIC PARENT"
3. Change `'your-email@example.com'` to the actual parent email
4. Uncomment the block (remove `/*` and `*/`)
5. Run in Supabase SQL Editor

### Option 3: Clean Up Duplicates Only
✅ **SAFE: Keeps latest device, removes old ones**

1. Open `supabase/reset-pairing-data.sql`
2. Find "OPTION 3: CLEAN UP DUPLICATES"
3. Uncomment the block (remove `/*` and `*/`)
4. Run in Supabase SQL Editor

## STEP 4: Verify the Fix

### Check Current Pairings
Run this query in Supabase SQL Editor:

```sql
SELECT 
    au.email as parent_email,
    dp.device_id,
    dp.device_name,
    dp.status,
    dp.paired_at,
    d.last_active
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
LEFT JOIN devices d ON d.device_id = dp.device_id
ORDER BY au.email, dp.paired_at DESC;
```

### Check for Duplicates
Run this query to see if any parent has multiple active devices:

```sql
SELECT 
    au.email as parent_email,
    COUNT(*) as active_device_count,
    STRING_AGG(dp.device_id, ', ') as device_ids
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
WHERE dp.status = 'active'
GROUP BY au.email
HAVING COUNT(*) > 1;
```

**Expected Result:** No rows (each parent should have only 1 active device)

## How It Works

### Before the Fix:
```
Parent scans QR → Child pairs with Device ID: ABC123
Parent rescans QR → Child pairs with NEW Device ID: XYZ789
Result: Dashboard still shows ABC123 ❌ Monitoring broken!
```

### After the Fix:
```
Parent scans QR → Child pairs with Device ID: ABC123
Parent rescans QR → Child pairs with NEW Device ID: XYZ789
SQL Function:
  1. Detects parent already has device ABC123
  2. Marks ABC123 as "replaced"
  3. Creates new pairing with XYZ789
  4. Dashboard auto-refreshes (60 sec)
  5. Dashboard now shows XYZ789 ✅ Monitoring works!
```

## Troubleshooting

### Issue: "operator does not exist: text = uuid"
**Solution:** This has been fixed! All JOIN statements now use correct type casting: `dp.parent_id = au.id::TEXT`

### Issue: "relation 'users' does not exist"
**Solution:** This has been fixed! All references now use `auth.users` instead of `users`.

### Issue: Dashboard not showing new device
**Solution:** 
1. Wait 60 seconds for auto-refresh
2. Or click "Refresh Devices" button manually
3. Check browser console for errors (F12)

### Issue: Multiple devices showing for one parent
**Solution:** Run Option 3 cleanup script to remove duplicates

### Issue: Monitoring still not working after re-pairing
**Solution:**
1. Check device permissions in child app
2. Verify device is sending data: Check `activities` table
3. Run verification queries above
4. Check browser console and network tab for API errors

## Files Modified

1. `supabase/fix-repairing-issue.sql` - Improved SQL function with re-pairing logic
2. `supabase/reset-pairing-data.sql` - Reset scripts with 3 options
3. All `auth.users` references corrected (was `users`)

## Auto-Refresh Feature

The dashboard already checks for new pairings every 60 seconds:
- Detects when child scans QR code
- Shows notification when new device pairs
- Auto-selects new device if none currently selected
- Updates device list automatically

This was implemented in previous fix (v1.6.0).

## Next Steps

1. ✅ Run `supabase/fix-repairing-issue.sql` in Supabase
2. ✅ Test re-pairing flow with actual devices
3. ✅ Verify dashboard auto-refresh detects changes
4. ✅ (Optional) Clean up old test data with reset scripts
5. ✅ Monitor for any issues in production

## Support

If you encounter any issues:
1. Check Supabase logs for SQL errors
2. Check browser console for JavaScript errors
3. Verify device permissions in child app
4. Run verification queries to check database state

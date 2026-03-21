# URGENT: Database Schema Fix for QR Pairing

## Problem
QR code pairing fails with error:
```
column "updated_at" of relation "device_pairing" does not exist
```

## Solution
Run the schema fix SQL file in your Supabase database.

## Steps to Fix (IMMEDIATE):

### Option 1: Supabase Dashboard (Recommended)
1. Go to https://supabase.com/dashboard
2. Select your project
3. Click "SQL Editor" in the left sidebar
4. Click "New Query"
5. Copy the entire contents of `supabase/fix-device-pairing-schema.sql`
6. Paste into the SQL editor
7. Click "Run" button
8. Wait for "Success" message

### Option 2: Command Line
```bash
# If you have psql installed
psql -h [your-supabase-host] -U postgres -d postgres -f supabase/fix-device-pairing-schema.sql
```

## What This Fix Does

1. **Adds `updated_at` column** to `device_pairing` table
2. **Creates trigger** to auto-update `updated_at` on changes
3. **Fixes `verify_qr_pairing` function** to work correctly
4. **Updates RLS policies** to allow anonymous pairing
5. **Allows device registration** without authentication

## After Running the Fix

1. ✅ QR code pairing will work immediately
2. ✅ No app updates needed
3. ✅ Both web dashboard and parent app QR codes will work
4. ✅ Child app can successfully pair

## Testing

After running the SQL:
1. Generate QR code from web dashboard or parent app
2. Scan with child app
3. Pairing should complete successfully
4. Device should appear in dashboard

## Verification

Check if the fix was applied:
```sql
-- Check if updated_at column exists
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'device_pairing' 
AND column_name = 'updated_at';

-- Should return: updated_at | timestamp with time zone
```

## Important Notes

- This fix is **backward compatible**
- Existing data will not be affected
- The `updated_at` column will be set to NOW() for existing records
- All new records will automatically have `updated_at` set

## If Still Having Issues

1. Check Supabase logs for errors
2. Verify RLS policies are enabled
3. Check that anon key is correct in apps
4. Ensure Supabase URL is correct

## Files Modified

- `supabase/fix-device-pairing-schema.sql` - Schema fix (NEW)
- `supabase/qr-pairing-function.sql` - Original function (reference)

---

**Status:** 🚨 URGENT FIX REQUIRED
**Priority:** CRITICAL
**Impact:** Blocks all QR code pairing
**Time to Fix:** 2 minutes

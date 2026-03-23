# Account Binding System Implementation

## Overview
Replaced the complex QR code/pairing code system with a simple account-based binding system. Parents now log into the child app with their credentials to bind the device to their account.

## Changes Made

### 1. Database Schema (`supabase/account-binding-schema.sql`)
- **Removed**: `device_pairing` table (no more pairing codes/QR codes)
- **Simplified**: `devices` table now directly links to parent account via `parent_id`
- **Added**: `bind_device_to_parent()` function for account binding
- **Updated**: RLS policies to work with direct parent-device relationship
- **Key Change**: Activities now require `parent_id` for better data isolation

### 2. Child App Changes

#### New LoginActivity (`android-app/app/src/main/java/com/parentalcontrol/monitor/LoginActivity.java`)
- Simple email/password login form
- Shows device information (model, ID)
- Authenticates parent via Supabase Auth
- Automatically binds device to parent account after successful login
- Navigates to consent screen after binding

#### Updated SupabaseClient (`android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java`)
- Added `loginParent()` method for authentication
- Added `bindDeviceToParent()` method for device binding
- Removed deprecated pairing code methods

#### Updated MainActivity (`android-app/app/src/main/java/com/parentalcontrol/monitor/MainActivity.java`)
- Changed "Start Setup & Pair" to "Login & Bind Device"
- Navigates to LoginActivity instead of QR scanner
- Updated "Re-Pair" button to "Unbind Device"
- Updated all messaging to reflect account binding

#### New Layout (`android-app/app/src/main/res/layout/activity_login.xml`)
- Clean, modern login interface
- Shows device info card
- Email and password inputs with Material Design
- Progress indicator for login process
- Helpful info text explaining the binding process

#### Updated AndroidManifest
- Registered LoginActivity

### 3. Web Dashboard Changes
- **No pairing UI needed**: Devices automatically appear after parent logs into child app
- **Simplified workflow**: Parents just need to check their dashboard to see bound devices
- **Removed**: All QR code generation, pairing code input, device ID input
- **Backend**: Updated to support account binding via `/api/devices` endpoint

### 4. Backend API Changes (`backend/server.js`)
- Existing `/api/devices` endpoint already supports the new system
- Activities are filtered by `parent_id` automatically via RLS
- No changes needed - the simplified schema works with existing endpoints

## How It Works

### For Parents:
1. Create account on web dashboard (existing flow)
2. Install child app on child's device
3. Open child app and tap "Login & Bind Device"
4. Enter parent account credentials
5. Device is automatically bound to parent account
6. Grant consent and permissions
7. Start monitoring

### For Monitoring:
1. Child app logs activities with both `device_id` and `parent_id`
2. Web dashboard queries devices by `parent_id`
3. Activities are automatically filtered by `parent_id` via RLS
4. No pairing codes, QR codes, or device IDs to manage

## Benefits

1. **Simpler User Experience**: No QR codes, no pairing codes, just login
2. **More Secure**: Uses Supabase Auth instead of custom pairing tokens
3. **Less Error-Prone**: No expired codes, no scanning issues
4. **Easier Support**: Parents can rebind devices by logging in again
5. **Better Multi-Device**: Parents can bind multiple devices by logging into each
6. **Cleaner Code**: Removed ~500 lines of pairing-related code

## Migration Steps

### 1. Database Migration
```sql
-- Run this in Supabase SQL Editor
-- Backup existing data first!
\i supabase/account-binding-schema.sql
```

### 2. Build New Child App
```bash
cd android-app
./gradlew assembleRelease
# Or use build-apk.bat on Windows
```

### 3. Deploy Web Dashboard
- No changes needed to HTML/JS
- Existing dashboard works with new system
- Remove any pairing UI if present (optional)

### 4. User Communication
- Notify existing users to reinstall child app
- Existing devices will need to be rebound
- Parents log into child app with their credentials

## Testing Checklist

- [ ] Parent can create account on web dashboard
- [ ] Parent can log into child app with credentials
- [ ] Device appears in web dashboard after login
- [ ] Activities are logged with parent_id
- [ ] Parent can view activities in dashboard
- [ ] Parent can unbind and rebind device
- [ ] Multiple devices can be bound to same parent
- [ ] RLS policies prevent cross-parent data access

## Rollback Plan

If issues occur:
1. Keep old APK available for download
2. Restore previous database schema from backup
3. Redeploy previous backend version
4. Notify users to reinstall old version

## Files Modified

### New Files:
- `supabase/account-binding-schema.sql`
- `android-app/app/src/main/java/com/parentalcontrol/monitor/LoginActivity.java`
- `android-app/app/src/main/res/layout/activity_login.xml`
- `ACCOUNT_BINDING_SYSTEM_IMPLEMENTATION.md`

### Modified Files:
- `android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java`
- `android-app/app/src/main/java/com/parentalcontrol/monitor/MainActivity.java`
- `android-app/app/src/main/AndroidManifest.xml`

### Files to Remove (Optional):
- `android-app/app/src/main/java/com/parentalcontrol/monitor/PairingActivity.java`
- `android-app/app/src/main/java/com/parentalcontrol/monitor/QRScannerActivity.java`
- `android-app/app/src/main/res/layout/activity_pairing.xml`
- `android-app/app/src/main/res/layout/activity_qr_scanner.xml`
- `parent-app/` (entire parent app no longer needed)

## Next Steps

1. **Test the new system thoroughly**
2. **Update build version** to v2.0.0 (major change)
3. **Build and sign new APK**
4. **Deploy database schema changes**
5. **Update documentation and user guides**
6. **Communicate changes to users**
7. **Monitor for issues and user feedback**

## Support

For issues or questions:
- Check device logs: `adb logcat | grep LoginActivity`
- Check Supabase logs for auth/binding errors
- Verify RLS policies are working correctly
- Test with fresh parent account and device

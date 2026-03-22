# Build Instructions for v1.7.0

## What Changed

### Child App v1.7.0
- Version updated to 1.7.0 (versionCode 17)
- Added automatic clearing of old pairing data on app start
- Users will be forced to re-pair and grant permissions after update
- Device ID persists but all pairing/consent data is cleared

### Dashboard
- Now only shows devices with `status='active'`
- Old/replaced/inactive devices hidden from dropdown
- Cleaner device list after database reset

## Build Steps

### Option 1: Use Android Studio (Recommended)
1. Open Android Studio
2. Open the `android-app` folder
3. Wait for Gradle sync to complete
4. Go to Build → Generate Signed Bundle / APK
5. Select APK
6. Choose release variant
7. Build will be at: `android-app/app/build/outputs/apk/release/app-release.apk`

### Option 2: Command Line (if files unlock)
```bash
cd android-app
gradlew assembleRelease
```

### Option 3: Use Existing Build Script
```bash
cd android-app
build-apk.bat
```

## After Building

1. Rename APK with timestamp:
   ```
   child-app-YYYYMMDD_HHMMSS.apk
   ```

2. Copy to web-dashboard:
   ```bash
   copy app-release.apk ..\web-dashboard\apk\child-app-20260322_XXXXXX.apk
   copy app-release.apk ..\web-dashboard\apk\child-app-latest.apk
   ```

3. Update download.html with new version and timestamp

4. Commit and push:
   ```bash
   git add -A
   git commit -m "Deploy child app v1.7.0 with pairing reset"
   git push origin main
   ```

## Database Reset (IMPORTANT!)

Before users update, run this in Supabase SQL Editor:

```sql
-- Run supabase/fix-repairing-issue-clean.sql first
-- Then run supabase/reset-all-pairings.sql
```

This will:
- Update the pairing function to handle re-pairing
- Clear all old device pairings
- Force fresh start for all users

## What Users Will Experience

1. Update app to v1.7.0
2. App opens and clears old pairing data automatically
3. User sees "Not paired" status
4. User must:
   - Open parent dashboard
   - Generate new QR code
   - Scan QR code with child app
   - Grant all permissions again
5. Fresh pairing with new system

## Files Modified

- `android-app/app/build.gradle` - Version 17, 1.7.0
- `android-app/app/src/main/java/com/parentalcontrol/monitor/MainActivity.java` - Added clearOldPairingData()
- `web-dashboard/dashboard.js` - Filter to show only active devices
- `supabase/fix-repairing-issue-clean.sql` - New pairing function
- `supabase/reset-all-pairings.sql` - Database reset script

## Summary

v1.7.0 is a breaking update that forces all users to re-pair their devices. This ensures:
- Clean slate with new pairing system
- All permissions re-granted
- No old/stale device data
- Dashboard shows only active devices

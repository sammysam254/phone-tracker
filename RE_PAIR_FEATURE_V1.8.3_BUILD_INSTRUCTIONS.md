# Child App v1.8.3 - Re-Pair Feature Build Instructions

## What's New in v1.8.3
- ✅ "Re-Pair Device" button added to MainActivity
- ✅ Clears all local pairing data (SharedPreferences)
- ✅ Clears all server data (activities, pairing records) via SQL function
- ✅ Stops monitoring services before re-pairing
- ✅ Seamless flow to QR scanner after clearing
- ✅ SQL function `clear_device_pairing()` added to Supabase

## Files Modified
1. `android-app/app/src/main/java/com/parentalcontrol/monitor/MainActivity.java`
   - Added SupabaseClient instance
   - Added rePairDevice() method
   - Added clearLocalPairingData() method
   - Added Re-Pair button click listener
   - Added onDestroy() to cleanup SupabaseClient

2. `android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java`
   - Added clearDevicePairing() method

3. `android-app/app/src/main/res/layout/activity_main.xml`
   - Added "Re-Pair Device" button with danger styling

4. `android-app/app/build.gradle`
   - Updated versionCode: 20 → 21
   - Updated versionName: "1.8.2" → "1.8.3"

5. `supabase/clear-device-pairing-function.sql`
   - New SQL function to clear device pairing data

## MANUAL BUILD STEPS (To Avoid OneDrive Lock)

### Step 1: Close All Programs
Close Android Studio, File Explorer windows, and any programs that might have files open in the android-app directory.

### Step 2: Open Command Prompt as Administrator
1. Press Windows Key
2. Type "cmd"
3. Right-click "Command Prompt"
4. Select "Run as administrator"

### Step 3: Navigate to Project Directory
```cmd
cd "C:\Users\COLLINS KIBET\OneDrive\Desktop\phone activity\android-app"
```

### Step 4: Stop Gradle Daemon (If Running)
```cmd
gradlew --stop
```

### Step 5: Delete Build Directory Manually
```cmd
rmdir /s /q app\build
```

If this fails due to OneDrive lock, try:
```cmd
takeown /f app\build /r /d y
icacls app\build /grant administrators:F /t
rmdir /s /q app\build
```

### Step 6: Build Fresh APK
```cmd
gradlew assembleRelease --no-daemon --info
```

### Step 7: Verify Build Success
Check if APK exists:
```cmd
dir app\build\outputs\apk\release\app-release.apk
```

### Step 8: Copy APK to Web Dashboard
```cmd
copy app\build\outputs\apk\release\app-release.apk ..\web-dashboard\apk\child-app-v1.8.3.apk
```

## ALTERNATIVE: Build Outside OneDrive

If the above steps still fail due to OneDrive locking:

### Option A: Temporarily Pause OneDrive
1. Right-click OneDrive icon in system tray
2. Click "Pause syncing" → "2 hours"
3. Run build steps above
4. Resume OneDrive after build completes

### Option B: Copy Project to Non-OneDrive Location
```cmd
xcopy /E /I /Y "C:\Users\COLLINS KIBET\OneDrive\Desktop\phone activity\android-app" "C:\temp-build\android-app"
cd C:\temp-build\android-app
gradlew assembleRelease --no-daemon
copy app\build\outputs\apk\release\app-release.apk "C:\Users\COLLINS KIBET\OneDrive\Desktop\phone activity\web-dashboard\apk\child-app-v1.8.3.apk"
```

## Deploy SQL Function to Supabase

Before testing the app, deploy the SQL function:

1. Go to Supabase Dashboard: https://supabase.com/dashboard
2. Select your project
3. Go to SQL Editor
4. Open `supabase/clear-device-pairing-function.sql`
5. Copy and paste the entire content
6. Click "Run" to execute

## Testing the Re-Pair Feature

1. Install the new APK on a test device
2. Complete initial pairing with a parent account
3. Open the child app
4. Scroll down and tap "🔄 Re-Pair Device" button
5. Confirm the dialog
6. Verify:
   - Progress dialog shows "Clearing pairing data..."
   - Local data is cleared
   - Server data is cleared (check Supabase tables)
   - App navigates to QR Scanner
   - Can scan new QR code and pair again

## Verification Checklist

- [ ] APK builds successfully without errors
- [ ] Version shows 1.8.3 in app
- [ ] Re-Pair button is visible on MainActivity
- [ ] Re-Pair button shows confirmation dialog
- [ ] Clicking "Yes, Re-Pair" clears local data
- [ ] Server data is cleared (activities, device_pairing)
- [ ] App navigates to QR Scanner after clearing
- [ ] Can successfully pair again with new QR code
- [ ] Monitoring works after re-pairing

## Troubleshooting

### Build Fails with "Unable to delete directory"
- Close all programs accessing the project
- Stop Gradle daemon: `gradlew --stop`
- Pause OneDrive syncing
- Try building again

### APK Not Found After Build
- Check build output for errors
- Look for APK at: `app\build\outputs\apk\release\app-release.apk`
- Try building with `--stacktrace` flag for more details

### Re-Pair Button Not Working
- Check Logcat for errors
- Verify SQL function is deployed to Supabase
- Check network connectivity
- Verify parent_id exists in SharedPreferences

## Next Steps After Successful Build

1. Test the re-pair functionality thoroughly
2. Update web dashboard download links to v1.8.3
3. Create release notes for users
4. Deploy to production
5. Monitor for any issues

## Support

If you encounter issues:
- Check Logcat: `adb logcat | grep MainActivity`
- Check Supabase logs for SQL function errors
- Verify network connectivity
- Ensure parent_id is stored correctly

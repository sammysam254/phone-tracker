# Build Instructions for OneDrive Users

## Problem
OneDrive is locking the build directory, preventing Gradle from building the APK.

## Solution Options

### Option 1: Pause OneDrive Sync (Recommended)
1. Right-click OneDrive icon in system tray
2. Click "Pause syncing" → "2 hours"
3. Run the build:
   ```cmd
   cd android-app
   gradlew clean assembleRelease
   ```
4. Resume OneDrive sync after build completes

### Option 2: Exclude Build Directory from OneDrive
1. Right-click OneDrive icon in system tray
2. Click "Settings"
3. Go to "Backup" tab
4. Click "Manage backup"
5. Add `android-app\app\build` to exclusions
6. Run the build:
   ```cmd
   cd android-app
   gradlew clean assembleRelease
   ```

### Option 3: Move Project Outside OneDrive (Best)
1. Copy entire project to a local folder (e.g., `C:\Projects\phone-activity`)
2. Build from there:
   ```cmd
   cd C:\Projects\phone-activity\android-app
   gradlew clean assembleRelease
   ```

### Option 4: Build in Android Studio
1. Open Android Studio
2. File → Open → Select `android-app` folder
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Wait for build to complete
5. Click "locate" in the notification to find the APK

## After Successful Build

The APK will be at:
```
android-app\app\build\outputs\apk\release\app-release.apk
```

Copy it to:
```
web-dashboard\apk\child-app-v1.1.4.apk
```

## Current Status

The code changes for v1.1.4 (keyboard input fix) are complete and ready.
Only the build step is blocked by OneDrive file locking.

## Files Ready to Push

All code changes have been made:
- KeyboardMonitor.java (fixed)
- dashboard.js (fixed)
- dashboard-enhanced.js (fixed)
- build.gradle (version 1.1.4)

You can push the code changes now, and build the APK separately when OneDrive is paused.

# Android APK Build Instructions

## Prerequisites

1. **Android Studio**: Download and install Android Studio
2. **Java Development Kit (JDK)**: JDK 8 or higher
3. **Android SDK**: API level 24 (Android 7.0) or higher

## Setup Steps

### 1. Open Project in Android Studio
1. Launch Android Studio
2. Select "Open an existing project"
3. Navigate to the `android-app` folder
4. Click "OK" to open the project

### 2. Configure Supabase Credentials
The Supabase URL and API key are already configured in:
- `SupabaseClient.java` with your project credentials

### 3. Sync Project
1. Android Studio will prompt to sync the project
2. Click "Sync Now" to download dependencies
3. Wait for the sync to complete

### 4. Build APK

#### Debug APK (for testing)
1. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for the build to complete
3. Click "locate" in the notification to find the APK
4. APK will be in: `android-app/app/build/outputs/apk/debug/app-debug.apk`

#### Release APK (for distribution)
1. Go to **Build** → **Generate Signed Bundle / APK**
2. Select "APK" and click "Next"
3. Create a new keystore or use existing one:
   - **Keystore path**: Choose location for new keystore
   - **Password**: Create a strong password
   - **Key alias**: Enter alias name (e.g., "parental-control")
   - **Key password**: Create key password
   - **Validity**: Set to 25+ years
   - **Certificate info**: Fill in your details
4. Click "Next"
5. Select "release" build variant
6. Check "V1" and "V2" signature versions
7. Click "Finish"
8. APK will be in: `android-app/app/build/outputs/apk/release/app-release.apk`

## Installation Instructions

### For Testing (Debug APK)
1. Enable "Developer Options" on target device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable "USB Debugging" in Developer Options
3. Enable "Install unknown apps" for your file manager
4. Transfer APK to device and install

### For Distribution (Release APK)
1. The signed release APK can be distributed to parents
2. Recipients need to enable "Install unknown apps" 
3. Install the APK on the child's device

## Important Notes

### Permissions Required
The app requires these sensitive permissions:
- **Call Log Access**: To monitor phone calls
- **SMS Access**: To monitor text messages  
- **Camera**: To detect camera usage
- **Microphone**: To detect microphone usage
- **Location**: To track device location
- **Usage Stats**: To monitor app usage
- **Foreground Service**: To run monitoring in background

### Device Compatibility
- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)
- **Architecture**: Supports all Android architectures (ARM, x86)

### Security Considerations
1. **Code Obfuscation**: ProGuard rules are configured for release builds
2. **Network Security**: All communications use HTTPS with Supabase
3. **Data Encryption**: Sensitive data is encrypted before transmission
4. **Permission Validation**: App validates all permissions before operation

### Testing Checklist
Before distributing, test these features:
- [ ] App installs successfully
- [ ] Consent screen displays properly
- [ ] All permissions can be granted
- [ ] Monitoring service starts and runs
- [ ] Call logs are captured and uploaded
- [ ] SMS messages are captured and uploaded
- [ ] App usage statistics are collected
- [ ] Camera/microphone usage is detected
- [ ] Data appears in web dashboard
- [ ] Service survives device reboot
- [ ] App works on different Android versions

### Troubleshooting

**Build Errors:**
- Clean project: **Build** → **Clean Project**
- Rebuild: **Build** → **Rebuild Project**
- Invalidate caches: **File** → **Invalidate Caches and Restart**

**Permission Issues:**
- Some permissions require manual enabling in device settings
- Usage Stats permission needs special system settings access
- Camera/Microphone detection may vary by device manufacturer

**Network Issues:**
- Verify Supabase URL and API key are correct
- Check device internet connection
- Verify Supabase project is active and accessible

## Distribution

### For Parents
1. Provide the signed release APK
2. Include installation instructions
3. Provide web dashboard URL for monitoring
4. Include consent and legal compliance information

### Legal Compliance
- Ensure proper consent is obtained before installation
- Provide clear privacy policy
- Include data usage disclosure
- Comply with local privacy laws and regulations
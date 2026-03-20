# 📱 Android Studio Setup & APK Build Guide

## 🎯 **Step 1: Install Android Studio**

### **Download & Install:**

**Windows:**
1. Go to https://developer.android.com/studio
2. Download Android Studio for Windows
3. Run the installer (`android-studio-xxx-windows.exe`)
4. Follow installation wizard
5. Choose "Standard" installation type

**macOS:**
1. Download Android Studio for Mac
2. Open the `.dmg` file
3. Drag Android Studio to Applications folder
4. Launch from Applications

**Linux:**
1. Download Android Studio for Linux
2. Extract: `tar -xzf android-studio-xxx-linux.tar.gz`
3. Navigate: `cd android-studio/bin`
4. Run: `./studio.sh`

### **Initial Setup:**
1. **Welcome Screen** → Click "Next"
2. **Install Type** → Choose "Standard"
3. **UI Theme** → Choose your preference
4. **SDK Components** → Accept licenses and install
5. **Finish** → Wait for downloads to complete

---

## 🔧 **Step 2: Configure Android Studio**

### **SDK Manager Setup:**
1. Open Android Studio
2. Go to **Tools** → **SDK Manager**
3. **SDK Platforms** tab:
   - ✅ Android 14 (API 34) - Target SDK
   - ✅ Android 7.0 (API 24) - Minimum SDK
   - ✅ Android 13 (API 33)
   - ✅ Android 12 (API 31)

4. **SDK Tools** tab:
   - ✅ Android SDK Build-Tools 34.0.0
   - ✅ Android Emulator
   - ✅ Android SDK Platform-Tools
   - ✅ Google Play services
   - ✅ Intel x86 Emulator Accelerator (if using Intel)

5. Click **Apply** → **OK** → Accept licenses

### **JDK Configuration:**
1. **File** → **Project Structure** → **SDK Location**
2. **JDK Location**: Should auto-detect Java 11 or 17
3. If not found, download from: https://adoptium.net/

---

## 📂 **Step 3: Open the Project**

### **Import Project:**
1. **Open Android Studio**
2. **Open an Existing Project**
3. Navigate to your `android-app` folder
4. Select the `android-app` directory
5. Click **OK**

### **First-Time Setup:**
1. **Gradle Sync** will start automatically
2. Wait for "Gradle sync finished" message
3. If errors occur, click **Try Again**

### **Project Structure Verification:**
```
android-app/
├── app/
│   ├── src/main/java/com/parentalcontrol/monitor/
│   ├── src/main/res/
│   └── build.gradle
├── gradle/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🔨 **Step 4: Build Configuration**

### **Gradle Sync Issues:**
If you encounter sync issues:

1. **File** → **Invalidate Caches and Restart**
2. **Build** → **Clean Project**
3. **Build** → **Rebuild Project**

### **Update Gradle (if needed):**
1. **File** → **Project Structure** → **Project**
2. **Gradle Version**: 8.2 or higher
3. **Android Gradle Plugin Version**: 8.2.0

### **SDK Path Issues:**
1. **File** → **Project Structure** → **SDK Location**
2. Verify **Android SDK Location** is correct
3. Usually: `C:\Users\[username]\AppData\Local\Android\Sdk` (Windows)

---

## 🏗️ **Step 5: Build the APK**

### **Debug APK (for testing):**

**Method 1 - Menu:**
1. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete
3. Click **locate** in notification
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

**Method 2 - Terminal:**
```bash
# In Android Studio terminal
./gradlew assembleDebug

# Or on Windows
gradlew.bat assembleDebug
```

### **Release APK (for distribution):**

**Create Keystore:**
1. **Build** → **Generate Signed Bundle / APK**
2. Select **APK** → **Next**
3. **Create new keystore**:
   - **Keystore path**: Choose location (e.g., `keystore/parental-control.jks`)
   - **Password**: Create strong password (save it!)
   - **Key alias**: `parental-control-key`
   - **Key password**: Create key password (save it!)
   - **Validity**: 25 years
   - **Certificate**: Fill your details

4. **Next** → Select **release** → **Finish**

**Build Release APK:**
```bash
# Terminal method
./gradlew assembleRelease
```

---

## 📱 **Step 6: Test the APK**

### **Install on Device:**

**Enable Developer Options:**
1. **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Go back → **Developer Options**
4. Enable **USB Debugging**

**Install APK:**
```bash
# Using ADB
adb install app-debug.apk

# Or transfer APK to device and install manually
```

### **Testing Checklist:**
- [ ] App installs successfully
- [ ] Permission setup screen appears
- [ ] All permissions can be granted
- [ ] Pairing code generates
- [ ] Consent screen displays properly
- [ ] Monitoring service starts
- [ ] Activities are logged to Supabase

---

## 🛠️ **Step 7: Troubleshooting**

### **Common Build Errors:**

**1. Gradle Sync Failed:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

**2. SDK Not Found:**
- **File** → **Project Structure** → **SDK Location**
- Verify Android SDK path is correct

**3. Build Tools Version:**
- Update `compileSdk` and `targetSdk` in `app/build.gradle`
- Sync project

**4. Dependency Issues:**
```bash
# Update dependencies
./gradlew --refresh-dependencies
```

**5. Memory Issues:**
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

### **Performance Optimization:**

**Enable Parallel Builds:**
```properties
# In gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

**Increase Build Speed:**
1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
2. Increase **Command-line Options**: `--parallel --daemon`

---

## 📋 **Step 8: Build Variants**

### **Available Build Types:**

**Debug:**
- Debuggable: Yes
- Minified: No
- Signing: Debug keystore
- Package: `com.parentalcontrol.monitor.debug`

**Release:**
- Debuggable: No
- Minified: Yes (ProGuard)
- Signing: Release keystore
- Package: `com.parentalcontrol.monitor`

### **Switch Build Variants:**
1. **View** → **Tool Windows** → **Build Variants**
2. Select **debug** or **release**
3. Build APK

---

## 🚀 **Step 9: Distribution**

### **APK Locations:**
```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          # For testing
└── release/
    └── app-release.apk        # For distribution
```

### **APK Information:**
- **Size**: ~15-25 MB (depending on features)
- **Min Android**: 7.0 (API 24)
- **Target Android**: 14 (API 34)
- **Permissions**: 20+ (comprehensive monitoring)

### **Distribution Methods:**
1. **Direct APK**: Share `app-release.apk` file
2. **Google Play**: Upload to Play Console (requires review)
3. **Enterprise**: Internal distribution systems
4. **Web Download**: Host on your website

---

## ✅ **Final Checklist**

### **Before Distribution:**
- [ ] Test on multiple Android versions (7.0+)
- [ ] Verify all permissions work
- [ ] Test pairing with parent dashboard
- [ ] Confirm Supabase connectivity
- [ ] Test remote camera/audio features
- [ ] Verify location tracking
- [ ] Check call recording functionality
- [ ] Test accessibility service
- [ ] Verify notification monitoring

### **Security Verification:**
- [ ] Release APK is signed
- [ ] ProGuard obfuscation enabled
- [ ] No debug logs in release
- [ ] API keys are secure
- [ ] Permissions are justified

---

## 🎯 **Quick Build Commands**

```bash
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (after keystore setup)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Check for lint issues
./gradlew lint
```

---

## 📞 **Support**

**If you encounter issues:**
1. Check Android Studio logs: **View** → **Tool Windows** → **Build**
2. Gradle Console: **View** → **Tool Windows** → **Gradle Console**
3. Clean and rebuild project
4. Invalidate caches and restart
5. Update Android Studio and SDK tools

**Ready to build! Follow these steps and you'll have your APK ready for deployment.** 🚀
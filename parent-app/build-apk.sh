#!/bin/bash

echo "Building Parent App APK with Device ID Pairing Support..."
echo

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build release APK
echo "Building release APK..."
./gradlew assembleRelease

# Check if build was successful
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo
    echo "✅ BUILD SUCCESSFUL!"
    echo
    echo "APK Location: app/build/outputs/apk/release/app-release.apk"
    echo
    
    # Copy to web dashboard directory
    echo "Copying APK to web dashboard..."
    mkdir -p "../web-dashboard/parent-apk"
    cp "app/build/outputs/apk/release/app-release.apk" "../web-dashboard/parent-apk/app-release.apk"
    
    if [ -f "../web-dashboard/parent-apk/app-release.apk" ]; then
        echo "✅ APK copied to web dashboard successfully!"
        echo
        echo "Parent App v1.2.0 with Device ID Pairing is ready for deployment!"
        echo
        echo "Features included:"
        echo "- Device ID pairing support"
        echo "- Enhanced clipboard integration"
        echo "- Improved authentication flow"
        echo "- Better WebView integration"
        echo
    else
        echo "❌ Failed to copy APK to web dashboard"
    fi
    
    # Show file info
    echo "APK Details:"
    ls -la "app/build/outputs/apk/release/app-release.apk"
    
else
    echo
    echo "❌ BUILD FAILED!"
    echo "Check the error messages above for details."
    echo
fi
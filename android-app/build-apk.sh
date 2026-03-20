#!/bin/bash

echo "Building Parental Control Android APK..."
echo

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "Error: gradlew not found. Please run this script from the android-app directory."
    exit 1
fi

echo "Cleaning previous builds..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo
    echo "✅ Debug APK built successfully!"
    echo "Location: app/build/outputs/apk/debug/app-debug.apk"
    echo
    echo "To build release APK, run: ./gradlew assembleRelease"
    echo
else
    echo
    echo "❌ Build failed. Check the error messages above."
    echo
fi
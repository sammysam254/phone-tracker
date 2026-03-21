#!/bin/bash

echo "========================================"
echo "Keyboard Monitoring Test Script"
echo "========================================"
echo

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    echo "Please connect a device via USB and enable USB debugging."
    exit 1
fi

echo "✓ Device connected"
echo

# Check if app is installed
if ! adb shell pm list packages | grep -q "com.parentalcontrol.monitor"; then
    echo "❌ Child app not installed!"
    echo "Please install the app first."
    exit 1
fi

echo "✓ App installed"
echo

# Check if accessibility service is enabled
echo "Checking accessibility service..."
if adb shell settings get secure enabled_accessibility_services | grep -q "parentalcontrol"; then
    echo "✓ Accessibility service enabled"
else
    echo "⚠️  Accessibility service may not be enabled"
    echo "   Please enable it in: Settings → Accessibility → Parental Control Monitor"
fi

echo
echo "========================================"
echo "Starting Live Keyboard Monitoring"
echo "========================================"
echo
echo "Instructions:"
echo "1. Open any app on the child device"
echo "2. Type some text in a text field"
echo "3. Watch the logs below"
echo
echo "Press Ctrl+C to stop monitoring"
echo
echo "----------------------------------------"
echo

# Monitor keyboard logs
adb logcat -c  # Clear previous logs
adb logcat | grep --line-buffered -E "KeyboardMonitor|AccessibilityMonitor" | while read line; do
    if echo "$line" | grep -q "Processing keyboard input"; then
        echo "📝 $line"
    elif echo "$line" | grep -q "✓ Keyboard input logged successfully"; then
        echo "✅ $line"
    elif echo "$line" | grep -q "✗ Failed to log"; then
        echo "❌ $line"
    elif echo "$line" | grep -q "Skipping empty"; then
        echo "⏭️  $line"
    else
        echo "$line"
    fi
done

@echo off
echo ========================================
echo Keyboard Monitoring Test Script
echo ========================================
echo.

REM Check if device is connected
adb devices | findstr "device$" >nul
if errorlevel 1 (
    echo X No Android device connected!
    echo Please connect a device via USB and enable USB debugging.
    pause
    exit /b 1
)

echo √ Device connected
echo.

REM Check if app is installed
adb shell pm list packages | findstr "com.parentalcontrol.monitor" >nul
if errorlevel 1 (
    echo X Child app not installed!
    echo Please install the app first.
    pause
    exit /b 1
)

echo √ App installed
echo.

REM Check if accessibility service is enabled
echo Checking accessibility service...
adb shell settings get secure enabled_accessibility_services | findstr "parentalcontrol" >nul
if errorlevel 1 (
    echo ! Accessibility service may not be enabled
    echo   Please enable it in: Settings - Accessibility - Parental Control Monitor
) else (
    echo √ Accessibility service enabled
)

echo.
echo ========================================
echo Starting Live Keyboard Monitoring
echo ========================================
echo.
echo Instructions:
echo 1. Open any app on the child device
echo 2. Type some text in a text field
echo 3. Watch the logs below
echo.
echo Press Ctrl+C to stop monitoring
echo.
echo ----------------------------------------
echo.

REM Clear previous logs and start monitoring
adb logcat -c
adb logcat | findstr /C:"KeyboardMonitor" /C:"AccessibilityMonitor"

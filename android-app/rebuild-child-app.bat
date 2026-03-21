@echo off
echo ========================================
echo Building Child App v1.1.4
echo Keyboard Input Fix
echo ========================================
echo.

cd /d "%~dp0"

echo Cleaning previous builds...
call gradlew clean

echo.
echo Building release APK...
call gradlew assembleRelease

echo.
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK Location:
    echo %cd%\app\build\outputs\apk\release\app-release.apk
    echo.
    echo Version: 1.1.4 (Keyboard Input Fix)
    echo.
    echo Next Steps:
    echo 1. Install APK on child device
    echo 2. Test keyboard input in various apps
    echo 3. Check dashboard Keyboard tab
    echo 4. Monitor logs: adb logcat ^| grep KeyboardMonitor
    echo.
) else (
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo Check the error messages above.
)

pause

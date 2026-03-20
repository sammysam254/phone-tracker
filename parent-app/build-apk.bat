@echo off
echo Building Parent App APK with Device ID Pairing Support...
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean

REM Build release APK
echo Building release APK...
call gradlew assembleRelease

REM Check if build was successful
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo ✅ BUILD SUCCESSFUL!
    echo.
    echo APK Location: app\build\outputs\apk\release\app-release.apk
    echo.
    
    REM Copy to web dashboard directory
    echo Copying APK to web dashboard...
    if not exist "..\web-dashboard\parent-apk" mkdir "..\web-dashboard\parent-apk"
    copy "app\build\outputs\apk\release\app-release.apk" "..\web-dashboard\parent-apk\app-release.apk"
    
    if exist "..\web-dashboard\parent-apk\app-release.apk" (
        echo ✅ APK copied to web dashboard successfully!
        echo.
        echo Parent App v1.2.0 with Device ID Pairing is ready for deployment!
        echo.
        echo Features included:
        echo - Device ID pairing support
        echo - Enhanced clipboard integration
        echo - Improved authentication flow
        echo - Better WebView integration
        echo.
    ) else (
        echo ❌ Failed to copy APK to web dashboard
    )
    
    REM Show file info
    echo APK Details:
    dir "app\build\outputs\apk\release\app-release.apk"
    
) else (
    echo.
    echo ❌ BUILD FAILED!
    echo Check the error messages above for details.
    echo.
)

pause
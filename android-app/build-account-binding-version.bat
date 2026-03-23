@echo off
echo ========================================
echo Building Account Binding Version v2.0.0
echo ========================================
echo.

REM Check if we're in OneDrive
cd /d "%~dp0"
set CURRENT_DIR=%CD%
echo Current directory: %CURRENT_DIR%

if "%CURRENT_DIR:OneDrive=%" NEQ "%CURRENT_DIR%" (
    echo.
    echo WARNING: You are building from OneDrive!
    echo OneDrive can cause build issues with Gradle.
    echo.
    echo Recommended: Copy project to C:\Projects\ or another local folder
    echo.
    pause
)

echo.
echo Step 1: Cleaning previous builds...
call gradlew.bat clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Building release APK...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo ERROR: Build failed!
    echo.
    echo Common fixes:
    echo 1. Make sure Android SDK is installed
    echo 2. Check local.properties has correct SDK path
    echo 3. Try running: gradlew.bat --stop
    echo 4. Delete .gradle folder and try again
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build Successful!
echo ========================================
echo.
echo APK Location:
echo %CD%\app\build\outputs\apk\release\app-release.apk
echo.
echo Next Steps:
echo 1. Test the APK on a device
echo 2. Verify login and device binding works
echo 3. Check that activities are logged correctly
echo 4. Deploy database schema: supabase/account-binding-schema.sql
echo.
echo Version: 2.0.0 (Account Binding System)
echo.
pause

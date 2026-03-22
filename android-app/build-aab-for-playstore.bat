@echo off
REM Build Android App Bundle (AAB) for Google Play Store Submission
REM This creates an optimized bundle that Google can use to generate APKs

echo ========================================
echo Building AAB for Play Store Submission
echo ========================================
echo.
echo This will create an Android App Bundle (.aab) file
echo that you can upload to Google Play Console.
echo.
echo Requirements:
echo - Production keystore configured
echo - Internet connection for Gradle
echo.
pause

echo.
echo Cleaning previous builds...
call gradlew clean

echo.
echo Building release AAB...
call gradlew bundleRelease

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.

if exist "app\build\outputs\bundle\release\app-release.aab" (
    echo SUCCESS: AAB file created!
    echo.
    echo Location: app\build\outputs\bundle\release\app-release.aab
    echo.
    echo File size:
    for %%A in ("app\build\outputs\bundle\release\app-release.aab") do echo %%~zA bytes
    echo.
    echo Next steps:
    echo 1. Go to https://play.google.com/console
    echo 2. Create new app or select existing app
    echo 3. Navigate to Release ^> Production
    echo 4. Click "Create new release"
    echo 5. Upload this AAB file
    echo.
    echo AAB file is ready for Play Store submission!
) else (
    echo ERROR: AAB file not found!
    echo Please check the build output above for errors.
    echo.
    echo Common issues:
    echo - Keystore not configured
    echo - Gradle build errors
    echo - Missing dependencies
)

echo.
pause

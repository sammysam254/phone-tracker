@echo off
echo Building Parental Control Android APK...
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please run this script from the android-app directory.
    pause
    exit /b 1
)

echo Cleaning previous builds...
call gradlew.bat clean

echo Building debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Debug APK built successfully!
    echo Location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To build release APK, run: gradlew assembleRelease
    echo.
) else (
    echo.
    echo ❌ Build failed. Check the error messages above.
    echo.
)

pause
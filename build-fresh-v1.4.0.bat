@echo off
echo ========================================
echo Building Fresh v1.4.0 APK from Local Disk
echo ========================================
echo.

REM Set paths
set SOURCE_DIR=%~dp0
set BUILD_DIR=C:\Temp\phone-activity-build
set ANDROID_DIR=%BUILD_DIR%\android-app

echo Step 1: Copying project to local disk...
if exist "%BUILD_DIR%" (
    echo Removing old build directory...
    rmdir /s /q "%BUILD_DIR%"
)

echo Creating build directory...
mkdir "%BUILD_DIR%"

echo Copying files (this may take a moment)...
xcopy "%SOURCE_DIR%*" "%BUILD_DIR%\" /E /I /H /Y /EXCLUDE:%SOURCE_DIR%build-exclude.txt

echo.
echo Step 2: Cleaning build artifacts...
if exist "%ANDROID_DIR%\app\build" rmdir /s /q "%ANDROID_DIR%\app\build"
if exist "%ANDROID_DIR%\build" rmdir /s /q "%ANDROID_DIR%\build"
if exist "%ANDROID_DIR%\.gradle" rmdir /s /q "%ANDROID_DIR%\.gradle"

echo.
echo Step 3: Building APK from local disk...
cd /d "%ANDROID_DIR%"

echo Running Gradle build...
call gradlew.bat clean assembleRelease --no-daemon

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    pause
    exit /b 1
)

echo.
echo Step 4: Copying APK back to project...
set TIMESTAMP=%date:~-4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

set APK_SOURCE=%ANDROID_DIR%\app\build\outputs\apk\release\app-release.apk
set APK_DEST=%SOURCE_DIR%web-dashboard\apk\child-app-v1.4.0-%TIMESTAMP%.apk

if exist "%APK_SOURCE%" (
    copy "%APK_SOURCE%" "%APK_DEST%"
    echo.
    echo ========================================
    echo BUILD SUCCESS!
    echo ========================================
    echo APK Location: %APK_DEST%
    echo APK Size: 
    dir "%APK_DEST%" | find "child-app"
    echo.
    echo Opening APK folder...
    explorer "%SOURCE_DIR%web-dashboard\apk"
) else (
    echo.
    echo ========================================
    echo ERROR: APK not found!
    echo ========================================
)

echo.
echo Step 5: Cleaning up build directory...
cd /d "%SOURCE_DIR%"
rmdir /s /q "%BUILD_DIR%"

echo.
echo Done!
pause

@echo off
echo ========================================
echo Building Child App Outside OneDrive
echo ========================================
echo.

REM Create temp build directory
set TEMP_BUILD_DIR=C:\temp-parental-control-build
echo Creating temporary build directory: %TEMP_BUILD_DIR%

REM Remove old temp directory if exists
if exist "%TEMP_BUILD_DIR%" (
    echo Removing old temp directory...
    rmdir /s /q "%TEMP_BUILD_DIR%"
)

REM Create new temp directory
mkdir "%TEMP_BUILD_DIR%"

REM Copy project files
echo Copying project files...
xcopy /E /I /Y "%~dp0" "%TEMP_BUILD_DIR%\android-app"

REM Copy keystore files if they exist
if exist "%~dp0parental-control-release.keystore" (
    echo Copying keystore to root...
    copy /Y "%~dp0parental-control-release.keystore" "%TEMP_BUILD_DIR%\android-app\"
    echo Copying keystore to app directory...
    copy /Y "%~dp0parental-control-release.keystore" "%TEMP_BUILD_DIR%\android-app\app\"
)
if exist "%~dp0keystore.properties" (
    copy /Y "%~dp0keystore.properties" "%TEMP_BUILD_DIR%\android-app\"
)

REM Navigate to temp directory
cd /d "%TEMP_BUILD_DIR%\android-app"

REM Clean build
echo.
echo Cleaning previous build...
call gradlew.bat clean --no-daemon

REM Build release APK
echo.
echo Building release APK...
call gradlew.bat assembleRelease --no-daemon

REM Check if build was successful
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    
    REM Copy APK back to original location
    set ORIGINAL_DIR=%~dp0
    echo Copying APK back to original location...
    copy /Y "app\build\outputs\apk\release\app-release.apk" "%ORIGINAL_DIR%app\build\outputs\apk\release\app-release.apk"
    
    REM Also copy to web-dashboard
    copy /Y "app\build\outputs\apk\release\app-release.apk" "%ORIGINAL_DIR%..\web-dashboard\apk\child-app-v1.2.1.apk"
    
    echo.
    echo APK copied to:
    echo - %ORIGINAL_DIR%app\build\outputs\apk\release\app-release.apk
    echo - %ORIGINAL_DIR%..\web-dashboard\apk\child-app-v1.2.1.apk
    echo.
    
    REM Clean up temp directory
    echo Cleaning up temporary files...
    cd /d C:\
    rmdir /s /q "%TEMP_BUILD_DIR%"
    
    echo.
    echo Build complete! You can now install the APK.
    pause
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Check the error messages above.
    echo Temp directory preserved at: %TEMP_BUILD_DIR%
    pause
)

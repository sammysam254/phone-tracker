@echo off
echo ========================================
echo Production Signing Setup
echo ========================================
echo.

REM Check if keystore already exists
if exist "parental-control-release.keystore" (
    echo Keystore already exists!
    echo.
    choice /C YN /M "Do you want to use the existing keystore"
    if errorlevel 2 goto CREATE_NEW
    goto SETUP_PROPERTIES
)

:CREATE_NEW
echo.
echo Step 1: Creating Production Keystore
echo ========================================
echo.
echo You will be asked for:
echo - Keystore password (REMEMBER THIS!)
echo - Key password (can be same as keystore)
echo - Your name
echo - Organization
echo - City
echo - State
echo - Country code (2 letters, e.g., KE)
echo.
pause

keytool -genkeypair -v -keystore parental-control-release.keystore -alias parental-control-key -keyalg RSA -keysize 2048 -validity 10000

if errorlevel 1 (
    echo.
    echo ERROR: Keystore creation failed!
    echo Make sure Java JDK is installed.
    pause
    exit /b 1
)

echo.
echo ✓ Keystore created successfully!
echo.

:SETUP_PROPERTIES
echo Step 2: Setting up keystore.properties
echo ========================================
echo.

if exist "keystore.properties" (
    echo keystore.properties already exists!
    choice /C YN /M "Do you want to overwrite it"
    if errorlevel 2 goto BUILD_APK
)

echo.
set /p STORE_PASS="Enter keystore password: "
set /p KEY_PASS="Enter key password (press Enter if same as keystore): "

if "%KEY_PASS%"=="" set KEY_PASS=%STORE_PASS%

echo storePassword=%STORE_PASS%> keystore.properties
echo keyPassword=%KEY_PASS%>> keystore.properties
echo keyAlias=parental-control-key>> keystore.properties
echo storeFile=parental-control-release.keystore>> keystore.properties

echo.
echo ✓ keystore.properties created!
echo.

:BUILD_APK
echo Step 3: Building Signed APK
echo ========================================
echo.
echo This will build the app with production signing...
echo.
pause

REM Use the build-outside-onedrive script
call build-outside-onedrive.bat

if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Your production-signed APK is ready!
echo.
echo IMPORTANT:
echo - Keep parental-control-release.keystore safe
echo - Back it up to a secure location
echo - Never commit it to git
echo - You need it for all future updates
echo.
echo The APK has been copied to:
echo - web-dashboard\apk\child-app-v1.1.9.apk
echo.
pause

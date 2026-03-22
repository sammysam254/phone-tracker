@echo off
echo ========================================
echo Creating Production Keystore
echo ========================================
echo.

REM Set keystore details
set KEYSTORE_FILE=parental-control-release.keystore
set KEY_ALIAS=parental-control-key
set VALIDITY_DAYS=10000

echo This will create a production keystore for signing your APK.
echo.
echo IMPORTANT: Remember the passwords you enter!
echo Write them down in a safe place.
echo.

REM Check if keystore already exists
if exist "%KEYSTORE_FILE%" (
    echo Keystore already exists: %KEYSTORE_FILE%
    echo.
    choice /C YN /M "Do you want to create a new one (this will overwrite the existing keystore)"
    if errorlevel 2 goto :EOF
    del "%KEYSTORE_FILE%"
)

echo.
echo Creating keystore...
echo You will be asked for:
echo 1. Keystore password (remember this!)
echo 2. Key password (can be same as keystore password)
echo 3. Your name
echo 4. Organization name
echo 5. City/Locality
echo 6. State/Province
echo 7. Country code (2 letters, e.g., KE for Kenya)
echo.

keytool -genkeypair -v -keystore %KEYSTORE_FILE% -alias %KEY_ALIAS% -keyalg RSA -keysize 2048 -validity %VALIDITY_DAYS%

if errorlevel 1 (
    echo.
    echo ========================================
    echo KEYSTORE CREATION FAILED!
    echo ========================================
    echo.
    echo Make sure you have Java JDK installed.
    pause
    goto :EOF
)

echo.
echo ========================================
echo KEYSTORE CREATED SUCCESSFULLY!
echo ========================================
echo.
echo Keystore file: %KEYSTORE_FILE%
echo Key alias: %KEY_ALIAS%
echo.
echo IMPORTANT: Keep this keystore file safe!
echo - Back it up to a secure location
echo - Never commit it to git
echo - You need it to sign future updates
echo.
echo Next step: Update build.gradle with signing configuration
echo.
pause

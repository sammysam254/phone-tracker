@echo off
echo Creating production keystore with specified details...
echo.

REM Remove old keystore if exists
if exist "parental-control-release.keystore" (
    del "parental-control-release.keystore"
)

REM Create keystore with all details in one command
keytool -genkeypair -v ^
  -keystore parental-control-release.keystore ^
  -alias parental-control-key ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 10000 ^
  -storepass "41516512#Sam" ^
  -keypass "41516512#Sam" ^
  -dname "CN=Sam, OU=Development, O=Parental Control, L=Nairobi, ST=Nairobi, C=KE"

if errorlevel 1 (
    echo.
    echo ERROR: Keystore creation failed!
    pause
    exit /b 1
)

echo.
echo ✓ Keystore created successfully!
echo.

REM Create keystore.properties
echo storePassword=41516512#Sam> keystore.properties
echo keyPassword=41516512#Sam>> keystore.properties
echo keyAlias=parental-control-key>> keystore.properties
echo storeFile=parental-control-release.keystore>> keystore.properties

echo ✓ keystore.properties created!
echo.
echo Keystore details:
echo - File: parental-control-release.keystore
echo - Alias: parental-control-key
echo - Password: 41516512#Sam
echo - Name: Sam
echo - Organization: Parental Control
echo - City: Nairobi
echo - State: Nairobi
echo - Country: KE
echo.

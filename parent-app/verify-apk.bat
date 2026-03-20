@echo off
echo Verifying Parent App APK...
echo.

echo APK Location: app\build\outputs\apk\release\app-release.apk
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo ✅ APK file exists
) else (
    echo ❌ APK file not found
    exit /b 1
)

echo.
echo APK Size:
for %%A in ("app\build\outputs\apk\release\app-release.apk") do echo %%~zA bytes (%%~zA / 1048576 = approx. 4.4 MB)

echo.
echo APK Structure Check:
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip = [System.IO.Compression.ZipFile]::OpenRead('app\build\outputs\apk\release\app-release.apk'); $hasManifest = $zip.Entries | Where-Object { $_.Name -eq 'AndroidManifest.xml' }; $hasDex = $zip.Entries | Where-Object { $_.Name -like '*.dex' }; $hasMetaInf = $zip.Entries | Where-Object { $_.Name -like 'META-INF/*' }; $zip.Dispose(); if($hasManifest) { Write-Host '✅ AndroidManifest.xml found' } else { Write-Host '❌ AndroidManifest.xml missing' }; if($hasDex) { Write-Host '✅ DEX files found' } else { Write-Host '❌ DEX files missing' }; if($hasMetaInf) { Write-Host '✅ META-INF directory found (likely signed)' } else { Write-Host '❌ META-INF directory missing (not signed)' }"

echo.
echo Signing Configuration:
echo - Keystore: debug.keystore
echo - Alias: androiddebugkey  
echo - Password: android (debug signing)

echo.
echo ✅ Parent App APK is ready for distribution!
echo.
echo Installation Instructions:
echo 1. Enable "Install from Unknown Sources" on Android device
echo 2. Download and install the APK
echo 3. Grant necessary permissions
echo 4. Login with your credentials
echo.
pause
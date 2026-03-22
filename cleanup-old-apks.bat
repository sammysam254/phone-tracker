@echo off
REM Cleanup Old APK Files - Keep Only Production Versions
REM Child App: v1.1.9 (Latest Production)
REM Parent App: v1.5.1 (Latest Production)

echo ========================================
echo Cleaning Up Old APK Files
echo ========================================
echo.
echo This will remove old APK versions and keep only:
echo - Child App: v1.1.9 (Production)
echo - Parent App: v1.5.1 (Production)
echo.
pause

cd web-dashboard

echo.
echo Removing old child app versions...
if exist "apk\child-app-v1.1.4.apk" del "apk\child-app-v1.1.4.apk"
if exist "apk\child-app-v1.1.5.apk" del "apk\child-app-v1.1.5.apk"
if exist "apk\child-app-v1.1.6.apk" del "apk\child-app-v1.1.6.apk"
if exist "apk\child-app-v1.1.7.apk" del "apk\child-app-v1.1.7.apk"
if exist "apk\child-app-v1.3.0.apk" del "apk\child-app-v1.3.0.apk"
if exist "apk\child-app-v1.5.0.apk" del "apk\child-app-v1.5.0.apk"
if exist "apk\child-app-v1.5.1.apk" del "apk\child-app-v1.5.1.apk"
if exist "apk\child-app-v1.5.2.apk" del "apk\child-app-v1.5.2.apk"
if exist "apk\child-app-v1.5.3.apk" del "apk\child-app-v1.5.3.apk"
if exist "apk\child-monitor-v1.2.1-qr.apk" del "apk\child-monitor-v1.2.1-qr.apk"

echo.
echo Removing old parent app versions...
if exist "parent-apk\parent-app-v1.4.0.apk" del "parent-apk\parent-app-v1.4.0.apk"
if exist "parent-apk\parent-monitor-v1.3.1-qr.apk" del "parent-apk\parent-monitor-v1.3.1-qr.apk"
if exist "parent-apk\parent-monitor-v1.5.0.apk" del "parent-apk\parent-monitor-v1.5.0.apk"

cd ..

echo.
echo ========================================
echo Cleanup Complete!
echo ========================================
echo.
echo Remaining APK files:
echo - web-dashboard/apk/child-app-v1.1.9.apk (Production)
echo - web-dashboard/parent-apk/parent-monitor-v1.5.1.apk (Production)
echo.
pause

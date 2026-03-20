# Parent Dashboard APK Files

This directory contains the Android APK files for the Parent Dashboard application.

## Available Downloads

### Parent App - Release APK (Latest)
- **File:** `app-release.apk`
- **Version:** 1.1.1
- **Size:** 4.4 MB
- **Purpose:** Parent dashboard access (Production ready)
- **Optimized:** Yes (ProGuard enabled)
- **Signed:** Release certificate
- **Latest Fixes:** Authentication hanging & dashboard loading

## Installation Requirements

- **Android Version:** 7.0 (API 24) or higher
- **Architecture:** Universal (ARM, x86, x64)
- **Permissions:** Internet access and basic device permissions
- **Storage:** At least 50 MB free space

## Download URLs

When deployed, these files are accessible at:
- Parent App: `https://your-domain.com/parent-apk/app-release.apk`
- Child App: `https://your-domain.com/apk/app-release.apk`
- Download Page: `https://your-domain.com/download`

## Version History

### v1.1.1 (March 20, 2026) - Current
- ✅ Fixed authentication hanging issues
- ✅ Added parent app detection for faster loading
- ✅ Improved dashboard access and timeout handling
- ✅ Enhanced mobile WebView compatibility

### v1.1.0 (Previous)
- Initial authentication and web dashboard fixes
- Basic mobile optimization

## Features

- **Fast Authentication:** Simplified login process for mobile devices
- **Dashboard Access:** Full web dashboard functionality in mobile app
- **Device Management:** Pair and manage multiple child devices
- **Real-time Monitoring:** Live updates from connected child devices
- **Remote Controls:** Camera, audio, location, and emergency features

## Security Notes

✅ **Production Ready:** This APK is signed and ready for production deployment.

## Installation Instructions

1. **Download:** Get the latest `app-release.apk` file
2. **Enable Unknown Sources:** Settings → Security → Install unknown apps
3. **Install:** Tap the APK file and follow installation prompts
4. **Login:** Use your existing account or create a new one
5. **Pair Devices:** Enter pairing codes from child devices

## File Verification

You can verify the APK files using:

```bash
# Check APK signature
jarsigner -verify -verbose -certs app-release.apk

# Get APK information
aapt dump badging app-release.apk

# Check file size and hash
sha256sum app-release.apk
```

## Deployment Status

- ✅ APK files are properly signed
- ✅ File permissions are set correctly
- ✅ HTTPS is enabled for downloads
- ✅ Download links are tested
- ✅ Installation instructions are provided
- ✅ Legal compliance is verified

## Support

For issues with APK downloads or installation:
- Check the download page: `/download`
- Review installation guide
- Contact support team

---

**Last Updated:** March 20, 2026  
**Version:** 1.1.1  
**Build Date:** March 20, 2026
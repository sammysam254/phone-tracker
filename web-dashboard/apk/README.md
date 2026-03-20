# Parental Control APK Files

This directory contains the Android APK files for the Parental Control monitoring application.

## Available Downloads

### Child App - Release APK (Latest)
- **File:** `app-release.apk`
- **Version:** 1.2.1
- **Size:** 2.5 MB
- **Purpose:** Child device monitoring (Production ready)
- **Optimized:** Yes (ProGuard enabled)
- **Signed:** Release certificate
- **Latest Fixes:** Enhanced pairing compatibility & error handling

## Installation Requirements

- **Android Version:** 7.0 (API 24) or higher
- **Architecture:** Universal (ARM, x86, x64)
- **Permissions:** Multiple sensitive permissions required
- **Storage:** At least 50 MB free space

## Download URLs

When deployed, these files are accessible at:
- Child App: `https://your-domain.com/apk/app-release.apk`
- Parent App: `https://your-domain.com/parent-apk/app-release.apk`
- Download Page: `https://your-domain.com/download`

## Version History

### v1.2.1 (March 20, 2026) - Current
- ✅ Fixed pairing code compatibility issues
- ✅ Enhanced error handling and retry logic
- ✅ Improved offline mode support
- ✅ Better status synchronization with dashboard

### v1.2.0 (Previous)
- Initial pairing code generation fixes
- Basic error handling improvements

## Security Notes

✅ **Production Ready:** This APK is signed and ready for production deployment.

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
**Version:** 1.2.1  
**Build Date:** March 20, 2026
# Parental Control APK Files

This directory contains the Android APK files for the Parental Control monitoring application.

## Available Downloads

### Release APK (Recommended)
- **File:** `app-release.apk`
- **Size:** 2.6 MB
- **Purpose:** Production use and distribution
- **Optimized:** Yes (ProGuard enabled)
- **Signed:** Debug certificate (change for production)

### Debug APK (Testing)
- **File:** `app-debug.apk`
- **Size:** 8.4 MB
- **Purpose:** Development and testing
- **Optimized:** No (full debug symbols)
- **Signed:** Debug certificate

## Installation Requirements

- **Android Version:** 7.0 (API 24) or higher
- **Architecture:** Universal (ARM, x86, x64)
- **Permissions:** Multiple sensitive permissions required
- **Storage:** At least 50 MB free space

## Download URLs

When deployed, these files are accessible at:
- Release: `https://your-domain.com/apk/app-release.apk`
- Debug: `https://your-domain.com/apk/app-debug.apk`
- Download Page: `https://your-domain.com/download`

## Security Notes

⚠️ **Important:** Before production deployment:

1. **Sign with Production Certificate:**
   ```bash
   keytool -genkey -v -keystore production.keystore -alias parental-control -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Update Build Configuration:**
   - Configure signing in `android-app/app/build.gradle`
   - Use production Supabase URLs
   - Enable ProGuard for release builds

3. **Verify File Integrity:**
   - Check APK signatures before deployment
   - Scan for malware/vulnerabilities
   - Test on multiple devices

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

## Deployment Checklist

- [ ] APK files are properly signed
- [ ] File permissions are set correctly
- [ ] HTTPS is enabled for downloads
- [ ] Download links are tested
- [ ] Installation instructions are provided
- [ ] Legal compliance is verified

## Support

For issues with APK downloads or installation:
- Check the download page: `/download`
- Review installation guide
- Contact support team

---

**Last Updated:** March 20, 2026  
**Version:** 1.0.0  
**Build Date:** March 20, 2026
# QR Code Format Compatibility - Fixed

## Problem
The child app was showing "Invalid QR" error when scanning QR codes from the web dashboard, but worked fine with QR codes from the parent app.

## Root Cause
**Different QR code formats:**

### Parent App Format (Working):
```json
{
  "parentId": "parent_123456789",
  "pairingToken": "64-character-hex-token",
  "timestamp": 1710936000000
}
```

### Web Dashboard Format (Not Working):
```
TOKEN|TIMESTAMP|EMAIL
```

### Child App Expected Format:
```json
{
  "parentId": "xxx",
  "pairingToken": "xxx",
  "timestamp": 123456
}
```

**Result:** Child app could only parse the JSON format, not the pipe-delimited format.

## The Fix

Updated web dashboard to generate QR codes in the **exact same JSON format** as the parent app.

### Changes Made:

1. **Generate parentId from email** (same logic as parent app):
```javascript
const parentId = userEmail ? 
    `parent_${userEmail.split('').reduce((a, b) => ((a << 5) - a) + b.charCodeAt(0), 0)}` : 
    `parent_${Date.now()}`;
```

2. **Use JSON format** instead of pipe-delimited:
```javascript
const qrData = JSON.stringify({
    parentId: parentId,
    pairingToken: pairingToken,
    timestamp: timestamp  // milliseconds since epoch
});
```

3. **Increased token size** back to 64 characters (32 bytes) for compatibility:
```javascript
const array = new Uint8Array(32);  // 32 bytes = 64 hex chars
```

4. **Use millisecond timestamp** (not Unix seconds):
```javascript
const timestamp = Date.now();  // milliseconds
```

## QR Code Format Specification

### Standard Format (Used by Both):
```json
{
  "parentId": "parent_[hash]",
  "pairingToken": "[64-char-hex-token]",
  "timestamp": [milliseconds-since-epoch]
}
```

### Field Descriptions:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| parentId | String | Parent identifier from email hash | `"parent_-1234567890"` |
| pairingToken | String | 64-character hex token (32 bytes) | `"a1b2c3d4..."` |
| timestamp | Number | Milliseconds since Unix epoch | `1710936000000` |

### Example QR Data:
```json
{
  "parentId": "parent_-1234567890",
  "pairingToken": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2",
  "timestamp": 1710936000000
}
```

## Validation Logic

### Child App Validation:
```java
// Parse QR code
JSONObject data = new JSONObject(qrData);
String parentId = data.getString("parentId");
String pairingToken = data.getString("pairingToken");
long timestamp = data.getLong("timestamp");

// Check expiry (10 minutes)
long currentTime = System.currentTimeMillis();
if (currentTime - timestamp > 10 * 60 * 1000) {
    // Expired
    return;
}

// Proceed with pairing
pairWithParent(parentId, pairingToken);
```

### Expiry Calculation:
- **Valid Duration:** 10 minutes (600,000 milliseconds)
- **Check:** `currentTime - timestamp > 600000`
- **Result:** Reject if expired

## Compatibility Matrix

| QR Source | Format | Child App | Status |
|-----------|--------|-----------|--------|
| Parent App (Old) | JSON | ✅ Compatible | Working |
| Web Dashboard (Old) | Pipe-delimited | ❌ Incompatible | Failed |
| Parent App (Current) | JSON | ✅ Compatible | Working |
| Web Dashboard (Current) | JSON | ✅ Compatible | **FIXED** |

## Testing Results

### Before Fix:
```
❌ Web Dashboard QR → Child App: "Invalid QR code"
✅ Parent App QR → Child App: Success
```

### After Fix:
```
✅ Web Dashboard QR → Child App: Success
✅ Parent App QR → Child App: Success
```

## Data Size Comparison

### Old Web Dashboard Format:
```
TOKEN|TIMESTAMP|EMAIL
Length: ~69 characters
```

### New Web Dashboard Format:
```json
{"parentId":"parent_-1234567890","pairingToken":"a1b2...","timestamp":1710936000000}
```
**Length:** ~150 characters

**Note:** Still well under the 1,056 character limit for QR codes with error correction level M.

## Security Considerations

### Token Security:
- **Size:** 64 characters (32 bytes)
- **Entropy:** 256 bits
- **Generation:** `crypto.getRandomValues()`
- **Expiry:** 10 minutes
- **One-time use:** Token validated during pairing

### Parent ID Generation:
```javascript
// Hash email to generate consistent parent ID
const hash = email.split('').reduce((a, b) => 
    ((a << 5) - a) + b.charCodeAt(0), 0
);
const parentId = `parent_${hash}`;
```

**Benefits:**
- Same email always generates same parentId
- No need to store parentId separately
- Consistent across web and mobile

## Files Modified

1. `web-dashboard/dashboard.js`
   - Updated `generateQRCode()` function
   - Changed QR data format to JSON
   - Added parentId generation logic
   - Increased token size to 64 characters
   - Use millisecond timestamp

## Deployment

- ✅ Fixed and committed (commit 6247713)
- ✅ Pushed to GitHub
- ✅ Compatible with existing child app
- ✅ Compatible with existing parent app
- ✅ No app updates required

## Next Steps

1. ✅ Test QR code generation in web dashboard
2. ✅ Test QR code scanning with child app
3. ✅ Verify pairing completes successfully
4. ✅ Test with both web dashboard and parent app QR codes
5. ⏳ Monitor for any issues

## Important Notes

### Backward Compatibility:
- ✅ New web dashboard QR codes work with existing child app
- ✅ Existing parent app QR codes still work
- ✅ No breaking changes
- ✅ Seamless upgrade

### Format Consistency:
- Both parent app and web dashboard now generate identical format
- Child app can scan QR codes from either source
- No special handling needed

### Future Improvements:
Consider adding version field for future format changes:
```json
{
  "version": "1.0",
  "parentId": "...",
  "pairingToken": "...",
  "timestamp": 123456
}
```

---

**Status:** ✅ FIXED - QR Code Format Compatibility Resolved
**Commit:** 6247713
**Date:** March 20, 2026
**Result:** Both web dashboard and parent app generate compatible QR codes

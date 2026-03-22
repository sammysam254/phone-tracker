# CRITICAL FIX: Parent ID Missing in Activity Logs - v1.8.1

## Problem Identified ❌

The monitoring was not working because the `logActivity()` method in `SupabaseClient.java` was NOT including the `parent_id` field when sending activity data to Supabase.

### Root Cause:
- When QR pairing completes, the `parent_id` is saved to SharedPreferences
- However, the `logActivity()` method was only sending `device_id`, `activity_type`, and `activity_data`
- The `parent_id` field was MISSING from the request body
- This caused all monitoring data to be rejected or not associated with the parent account

## Solution ✅

### Changes Made:

**File:** `android-app/app/src/main/java/com/parentalcontrol/monitor/SupabaseClient.java`

**Method:** `logActivity()`

**Before:**
```java
public void logActivity(String deviceId, String activityType, JSONObject data, ApiCallback callback) {
    executor.execute(() -> {
        try {
            URL url = new URL(SUPABASE_URL + "/rest/v1/activities");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("device_id", deviceId);
            requestBody.put("activity_type", activityType);
            requestBody.put("activity_data", data);
            requestBody.put("timestamp", java.time.Instant.now().toString());
            // ... rest of code
        }
    });
}
```

**After:**
```java
public void logActivity(String deviceId, String activityType, JSONObject data, ApiCallback callback) {
    executor.execute(() -> {
        try {
            // Get parent_id from SharedPreferences
            android.content.SharedPreferences prefs = context.getSharedPreferences("ParentalControl", Context.MODE_PRIVATE);
            String parentId = prefs.getString("parent_id", null);
            
            if (parentId == null || parentId.isEmpty()) {
                Log.w(TAG, "No parent_id found - device may not be paired yet");
                if (callback != null) {
                    callback.onError("Device not paired - no parent_id found");
                }
                return;
            }
            
            URL url = new URL(SUPABASE_URL + "/rest/v1/activities");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Create request body with parent_id
            JSONObject requestBody = new JSONObject();
            requestBody.put("device_id", deviceId);
            requestBody.put("parent_id", parentId);  // ✅ ADD PARENT_ID
            requestBody.put("activity_type", activityType);
            requestBody.put("activity_data", data);
            requestBody.put("timestamp", java.time.Instant.now().toString());
            // ... rest of code
        }
    });
}
```

### Key Changes:
1. ✅ Retrieve `parent_id` from SharedPreferences at the start of the method
2. ✅ Check if `parent_id` exists - if not, return error (device not paired)
3. ✅ Include `parent_id` in the request body sent to Supabase
4. ✅ Add logging to help debug pairing issues

## Impact

### What This Fixes:
- ✅ All monitoring data will now be associated with the correct parent account
- ✅ Parents will see activity from their paired child devices
- ✅ Dashboard will display monitoring data correctly
- ✅ Call logs, SMS, location, app usage, etc. will all work

### Affected Features:
- Call monitoring
- SMS monitoring
- Location tracking
- App usage tracking
- Web activity monitoring
- Camera/microphone monitoring
- Keyboard input monitoring
- Screen interactions
- Notifications
- Remote commands

## Build Instructions

### Version Update:
- Version: 1.8.0 → 1.8.1
- Version Code: 18 → 19

### Build Command:
```bash
cd android-app
./gradlew clean assembleRelease
```

### If OneDrive Causes Issues:
```bash
cd android-app
./build-outside-onedrive.bat
```

### Output Location:
```
android-app/app/build/outputs/apk/release/app-release.apk
```

### Deploy To:
```
web-dashboard/apk/child-monitor-v1.8.1.apk
```

## Testing Checklist

After installing v1.8.1:

1. ✅ Uninstall old child app
2. ✅ Install v1.8.1 APK
3. ✅ Pair device using QR code
4. ✅ Grant all permissions
5. ✅ Make a test call - check if it appears in dashboard
6. ✅ Send a test SMS - check if it appears in dashboard
7. ✅ Open some apps - check if usage appears in dashboard
8. ✅ Check location tracking in dashboard
9. ✅ Verify all monitoring features work

## Database Schema

The `activities` table should have these columns:
- `id` (primary key)
- `device_id` (text)
- `parent_id` (text) ← THIS WAS MISSING IN REQUESTS
- `activity_type` (text)
- `activity_data` (jsonb)
- `timestamp` (timestamptz)
- `created_at` (timestamptz)

## Why This Happened

The fresh build in v1.8.0 was correct in terms of pairing logic, but the `logActivity()` method was never updated to include the `parent_id` field. This is a critical oversight that prevented all monitoring from working.

## Next Steps

1. Build v1.8.1 with this fix
2. Deploy to web-dashboard
3. Update download page
4. Test thoroughly
5. Notify users to update

## Deployment Status

- [x] Code fixed in SupabaseClient.java
- [x] Version updated to 1.8.1
- [ ] APK built
- [ ] APK deployed to web-dashboard
- [ ] Download page updated
- [ ] Pushed to GitHub
- [ ] Tested with real device

---

**This is a CRITICAL fix that makes monitoring actually work!**

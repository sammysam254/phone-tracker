# Device Lock with Unlock Code - Implementation Guide

## Files Created:
1. ✅ `supabase/device-lock-schema.sql` - Database schema
2. ✅ `UnlockCodeManager.java` - Manages unlock codes
3. ✅ `DeviceLockActivity.java` - Lock screen overlay
4. ✅ `activity_device_lock.xml` - Lock screen layout

## Remaining Steps:

### Step 6: Update AndroidManifest.xml

Add these permissions and activity:

```xml
<!-- Add to manifest -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.DISABLE_KEYGUARD" />

<!-- Add inside <application> tag -->
<activity
    android:name=".DeviceLockActivity"
    android:excludeFromRecents="true"
    android:launchMode="singleInstance"
    android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
    android:screenOrientation="portrait" />
```

### Step 7: Update RemoteControlService.java

Add lock command handling in `handleRemoteCommand()`:

```java
private void handleRemoteCommand(String command, String data) {
    switch (command) {
        case "lock_device":
            handleLockDevice(data);
            break;
        case "unlock_device":
            handleUnlockDevice();
            break;
        // ... existing cases
    }
}

private void handleLockDevice(String data) {
    try {
        JSONObject lockData = new JSONObject(data);
        String unlockCode = lockData.getString("unlock_code");
        String message = lockData.optString("message", "Device locked by parent");
        
        // Save lock state
        UnlockCodeManager unlockManager = new UnlockCodeManager(this);
        unlockManager.lockDevice(unlockCode, message);
        
        // Show lock screen
        Intent lockIntent = new Intent(this, DeviceLockActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(lockIntent);
        
        // Also use device admin lock
        RemoteDeviceController controller = new RemoteDeviceController(this);
        controller.lockDevice();
        
        Log.i(TAG, "Device locked with unlock code");
    } catch (Exception e) {
        Log.e(TAG, "Error locking device", e);
    }
}

private void handleUnlockDevice() {
    UnlockCodeManager unlockManager = new UnlockCodeManager(this);
    unlockManager.unlockDevice();
    Log.i(TAG, "Device unlocked remotely");
}
```

### Step 8: Update PermissionSetupActivity.java

Add SYSTEM_ALERT_WINDOW permission request:

```java
private void requestOverlayPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
}
```

### Step 9: Web Dashboard - Add Lock Controls

Add to `dashboard.js`:

```javascript
// Lock device
async function lockDevice(deviceId) {
    try {
        const { data, error } = await supabase
            .rpc('lock_device', {
                p_device_id: deviceId,
                p_lock_message: 'Device locked by parent'
            });
        
        if (error) throw error;
        
        alert(`Device locked! Unlock code: ${data.unlock_code}\nSave this code to unlock the device.`);
        
        // Send command to device
        await sendRemoteCommand(deviceId, 'lock_device', {
            unlock_code: data.unlock_code,
            message: 'Device locked by parent'
        });
        
    } catch (error) {
        console.error('Error locking device:', error);
        alert('Failed to lock device');
    }
}

// Unlock device
async function unlockDevice(deviceId) {
    try {
        const { data, error } = await supabase
            .rpc('unlock_device', {
                p_device_id: deviceId
            });
        
        if (error) throw error;
        
        // Send command to device
        await sendRemoteCommand(deviceId, 'unlock_device', {});
        
        alert('Device unlocked successfully');
        
    } catch (error) {
        console.error('Error unlocking device:', error);
        alert('Failed to unlock device');
    }
}

// Generate new unlock code
async function generateUnlockCode(deviceId) {
    try {
        // Get current lock status
        const { data: lockStatus } = await supabase
            .rpc('get_device_lock_status', {
                p_device_id: deviceId
            });
        
        if (!lockStatus.is_locked) {
            alert('Device is not locked');
            return;
        }
        
        // Lock again with new code
        const { data, error } = await supabase
            .rpc('lock_device', {
                p_device_id: deviceId,
                p_lock_message: 'New unlock code generated'
            });
        
        if (error) throw error;
        
        alert(`New unlock code: ${data.unlock_code}\nThis code expires in 24 hours.`);
        
        // Send new code to device
        await sendRemoteCommand(deviceId, 'lock_device', {
            unlock_code: data.unlock_code,
            message: 'New unlock code generated'
        });
        
    } catch (error) {
        console.error('Error generating unlock code:', error);
        alert('Failed to generate unlock code');
    }
}
```

### Step 10: Add UI Buttons to Dashboard

Add to `index.html` in the device controls section:

```html
<div class="control-buttons">
    <button onclick="lockDevice('DEVICE_ID')" class="btn-danger">
        🔒 Lock Device
    </button>
    <button onclick="unlockDevice('DEVICE_ID')" class="btn-success">
        🔓 Unlock Device
    </button>
    <button onclick="generateUnlockCode('DEVICE_ID')" class="btn-warning">
        🔑 Generate Unlock Code
    </button>
</div>
```

### Step 11: Request Device Admin Permission

Update `MainActivity.java` to request device admin on first launch:

```java
private void checkDeviceAdmin() {
    RemoteDeviceController controller = new RemoteDeviceController(this);
    if (!controller.isDeviceAdminEnabled()) {
        new AlertDialog.Builder(this)
            .setTitle("Device Admin Required")
            .setMessage("Device admin permission is required for remote lock feature. Enable it now?")
            .setPositiveButton("Enable", (dialog, which) -> {
                controller.requestDeviceAdmin();
            })
            .setNegativeButton("Later", null)
            .show();
    }
}
```

## Testing Steps:

1. **Install Updated App**
   - Grant SYSTEM_ALERT_WINDOW permission
   - Enable Device Admin

2. **Test Lock from Dashboard**
   - Click "Lock Device"
   - Note the unlock code
   - Device should show lock screen

3. **Test Unlock with Code**
   - Enter unlock code on device
   - Device should unlock

4. **Test Remote Unlock**
   - Lock device again
   - Click "Unlock Device" from dashboard
   - Device should unlock immediately

5. **Test Generate New Code**
   - While device is locked
   - Click "Generate Unlock Code"
   - Old code should stop working
   - New code should work

## Security Notes:

- Unlock codes expire after 24 hours
- Lock screen prevents back button
- Lock screen restarts if user tries to leave
- Device admin lock works as backup
- Only parent can unlock remotely

## Database Setup:

Run `supabase/device-lock-schema.sql` in Supabase SQL Editor before testing.

## Build Version:

Update to v1.3.0 when implementing this feature.

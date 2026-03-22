package com.parentalcontrol.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class UnlockCodeManager {
    
    private static final String TAG = "UnlockCodeManager";
    private static final String PREFS_NAME = "DeviceLockPrefs";
    private static final String KEY_IS_LOCKED = "is_locked";
    private static final String KEY_UNLOCK_CODE = "unlock_code";
    private static final String KEY_LOCK_MESSAGE = "lock_message";
    private static final String KEY_LOCKED_AT = "locked_at";
    
    private Context context;
    private SharedPreferences prefs;
    
    public UnlockCodeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Lock the device with unlock code
     */
    public void lockDevice(String unlockCode, String message) {
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, true)
            .putString(KEY_UNLOCK_CODE, unlockCode)
            .putString(KEY_LOCK_MESSAGE, message)
            .putLong(KEY_LOCKED_AT, System.currentTimeMillis())
            .apply();
        
        Log.i(TAG, "Device locked with unlock code");
    }
    
    /**
     * Unlock the device
     */
    public void unlockDevice() {
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .remove(KEY_UNLOCK_CODE)
            .remove(KEY_LOCK_MESSAGE)
            .apply();
        
        Log.i(TAG, "Device unlocked");
    }
    
    /**
     * Check if device is locked
     */
    public boolean isDeviceLocked() {
        return prefs.getBoolean(KEY_IS_LOCKED, false);
    }
    
    /**
     * Verify unlock code
     */
    public boolean verifyUnlockCode(String code) {
        String storedCode = prefs.getString(KEY_UNLOCK_CODE, "");
        return storedCode.equals(code);
    }
    
    /**
     * Get lock message
     */
    public String getLockMessage() {
        return prefs.getString(KEY_LOCK_MESSAGE, "Device locked by parent");
    }
    
    /**
     * Get locked timestamp
     */
    public long getLockedAt() {
        return prefs.getLong(KEY_LOCKED_AT, 0);
    }
}

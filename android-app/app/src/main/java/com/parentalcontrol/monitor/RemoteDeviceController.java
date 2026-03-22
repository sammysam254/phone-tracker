package com.parentalcontrol.monitor;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoteDeviceController {
    
    private static final String TAG = "RemoteDeviceController";
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;
    
    public RemoteDeviceController(Context context) {
        this.context = context;
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.deviceAdminComponent = new ComponentName(context, ParentalDeviceAdminReceiver.class);
    }
    
    /**
     * Check if device admin is enabled
     */
    public boolean isDeviceAdminEnabled() {
        return devicePolicyManager != null && 
               devicePolicyManager.isAdminActive(deviceAdminComponent);
    }
    
    /**
     * Request device admin activation
     * For Activity contexts, use startActivityForResult with REQUEST_CODE_ENABLE_ADMIN
     * Returns true if request was initiated, false if already enabled
     */
    public boolean requestDeviceAdmin() {
        if (!isDeviceAdminEnabled()) {
            try {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Enable device admin to allow remote lock and security features. This is required for parental control functionality.");
                
                // If context is an Activity, use startActivityForResult
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).startActivityForResult(intent, 1001); // REQUEST_CODE_ENABLE_ADMIN
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                
                Log.i(TAG, "Device admin request initiated");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error requesting device admin", e);
                return false;
            }
        }
        Log.i(TAG, "Device admin already enabled");
        return false;
    }
    
    /**
     * Lock the device immediately
     */
    public boolean lockDevice() {
        try {
            if (isDeviceAdminEnabled()) {
                devicePolicyManager.lockNow();
                Log.i(TAG, "Device locked successfully");
                return true;
            } else {
                Log.w(TAG, "Device admin not enabled, cannot lock device");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error locking device", e);
            return false;
        }
    }
    
    /**
     * Wipe device data (factory reset)
     */
    public boolean wipeDevice(boolean wipeExternalStorage) {
        try {
            if (isDeviceAdminEnabled()) {
                int flags = 0;
                if (wipeExternalStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    flags = DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
                }
                devicePolicyManager.wipeData(flags);
                Log.i(TAG, "Device wipe initiated");
                return true;
            } else {
                Log.w(TAG, "Device admin not enabled, cannot wipe device");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error wiping device", e);
            return false;
        }
    }
    
    /**
     * Reset device password
     */
    public boolean resetPassword(String newPassword) {
        try {
            if (isDeviceAdminEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ requires user to set password
                    Log.w(TAG, "Password reset not supported on Android 8.0+");
                    return false;
                } else {
                    boolean success = devicePolicyManager.resetPassword(newPassword, 0);
                    Log.i(TAG, "Password reset: " + success);
                    return success;
                }
            } else {
                Log.w(TAG, "Device admin not enabled, cannot reset password");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting password", e);
            return false;
        }
    }
    
    /**
     * Uninstall an app
     */
    public void uninstallApp(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "Uninstall initiated for: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error uninstalling app: " + packageName, e);
        }
    }
    
    /**
     * Install an APK file
     */
    public void installApp(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "Install initiated for: " + apkFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error installing app", e);
        }
    }
    
    /**
     * Silent install (requires system permissions)
     */
    public void silentInstallApp(File apkFile) {
        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            
            OutputStream out = session.openWrite("package", 0, -1);
            InputStream in = new FileInputStream(apkFile);
            
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();
            
            Intent intent = new Intent(context, context.getClass());
            intent.setAction("INSTALL_COMPLETE");
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, sessionId, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            
            session.commit(pendingIntent.getIntentSender());
            session.close();
            
            Log.i(TAG, "Silent install initiated for: " + apkFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error during silent install", e);
            // Fall back to regular install
            installApp(apkFile);
        }
    }
    
    /**
     * Get list of installed apps
     */
    public java.util.List<android.content.pm.ApplicationInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }
    
    /**
     * Disable camera
     */
    public boolean disableCamera(boolean disabled) {
        try {
            if (isDeviceAdminEnabled()) {
                devicePolicyManager.setCameraDisabled(deviceAdminComponent, disabled);
                Log.i(TAG, "Camera " + (disabled ? "disabled" : "enabled"));
                return true;
            } else {
                Log.w(TAG, "Device admin not enabled, cannot disable camera");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling camera", e);
            return false;
        }
    }
    
    /**
     * Set maximum failed password attempts before wipe
     */
    public boolean setMaxFailedPasswordAttempts(int maxAttempts) {
        try {
            if (isDeviceAdminEnabled()) {
                devicePolicyManager.setMaximumFailedPasswordsForWipe(deviceAdminComponent, maxAttempts);
                Log.i(TAG, "Max failed password attempts set to: " + maxAttempts);
                return true;
            } else {
                Log.w(TAG, "Device admin not enabled");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting max failed password attempts", e);
            return false;
        }
    }
}

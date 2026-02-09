package com.childusage.tracker.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Helper to guide users through MIUI/HyperOS specific settings
 * that prevent background service killing.
 *
 * On Redmi/HyperOS devices, the system aggressively kills background
 * processes. This class provides intents to open the relevant settings pages.
 */
public class MiuiSurvival {

    private static final String TAG = "MiuiSurvival";

    /**
     * Try to open MIUI Autostart management page.
     * On HyperOS/MIUI, apps must be whitelisted for autostart.
     */
    public static boolean openAutoStartSettings(Context context) {
        Intent[] intents = {
                // MIUI autostart manager
                new Intent().setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )),
                // HyperOS variant
                new Intent().setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartDetailActivity"
                )),
                // Fallback: MIUI security center
                new Intent().setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.securitycenter.MainActivity"
                )),
        };

        for (Intent intent : intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.d(TAG, "Intent failed: " + intent.getComponent(), e);
            }
        }
        return false;
    }

    /**
     * Try to open MIUI battery saver settings for this app,
     * so the user can set it to "No restrictions".
     */
    public static boolean openBatterySaverSettings(Context context) {
        Intent[] intents = {
                // MIUI app-level battery settings
                new Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                // HyperOS battery optimization
                new Intent().setComponent(new ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                )).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        };

        for (Intent intent : intents) {
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.d(TAG, "Battery intent failed", e);
            }
        }
        return false;
    }

    /**
     * Try to open MIUI "Lock App" settings (recent task lock).
     * When an app is locked, it won't be killed by "Clear All" in recents.
     */
    public static boolean openAppLockSettings(Context context) {
        try {
            Intent intent = new Intent().setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.securitycenter.MainActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Lock settings failed", e);
            return false;
        }
    }

    /**
     * Check if the device is running MIUI / HyperOS.
     */
    public static boolean isMiui() {
        try {
            @SuppressWarnings("rawtypes")
            Class clazz = Class.forName("android.os.SystemProperties");
            Object result = clazz.getMethod("get", String.class, String.class)
                    .invoke(null, "ro.miui.ui.version.name", "");
            return result != null && !result.toString().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}

package com.childusage.tracker.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * AccessibilityService that detects foreground app changes.
 * When a new window appears, it checks if the app is in our tracked set,
 * and notifies the UsageAccumulator (via SharedPreferences-based signaling
 * to the TrackingService).
 */
public class AppAccessibilityService extends AccessibilityService {

    private static final String TAG = "A11yWatcher";
    private String lastPackage = "";

    // Static callback — TrackingService registers a listener
    public interface ForegroundListener {
        void onForegroundChanged(String packageName);
    }

    private static volatile ForegroundListener listener;

    public static void setListener(ForegroundListener l) {
        listener = l;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;

        String packageName = pkg.toString();
        if (packageName.equals(lastPackage)) return;

        lastPackage = packageName;
        Log.d(TAG, "Foreground: " + packageName);

        ForegroundListener l = listener;
        if (l != null) {
            l.onForegroundChanged(packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            info.notificationTimeout = 100;
            setServiceInfo(info);
        }
        Log.i(TAG, "Accessibility service connected");
    }
}

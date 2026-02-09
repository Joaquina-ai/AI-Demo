package com.childusage.tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.childusage.tracker.service.TrackingService;
import com.childusage.tracker.util.DeviceHelper;

/**
 * Starts the TrackingService after device boot or app update,
 * so the service auto-restarts without manual intervention.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            if (DeviceHelper.isConfigured(context)) {
                Log.i("BootReceiver", "Starting tracking service after: " + action);
                TrackingService.start(context);
            }
        }
    }
}

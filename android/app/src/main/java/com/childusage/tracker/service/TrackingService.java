package com.childusage.tracker.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.childusage.tracker.App;
import com.childusage.tracker.MainActivity;
import com.childusage.tracker.R;
import com.childusage.tracker.model.AppInfo;
import com.childusage.tracker.util.AppScanner;
import com.childusage.tracker.util.DeviceHelper;
import com.childusage.tracker.util.SnapshotReporter;
import com.childusage.tracker.util.UsageAccumulator;
import com.childusage.tracker.util.UsageStatsReconciler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that:
 * 1. Listens for foreground app changes (via AccessibilityService callback)
 * 2. Accumulates usage time for tracked apps
 * 3. Periodically sends snapshot reports to the server
 */
public class TrackingService extends Service {

    private static final String TAG = "TrackingService";
    private static final long REPORT_INTERVAL_MS = 60_000; // 60 seconds

    private UsageAccumulator accumulator;
    private SnapshotReporter reporter;
    private Handler handler;
    private ExecutorService executor;

    // Tracked packages -> labels
    private final Map<String, String> trackedLabels = new HashMap<>();
    private final Set<String> trackedPackages = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        accumulator = new UsageAccumulator(this);
        reporter = new SnapshotReporter(this);
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();

        // Scan for target apps
        List<AppInfo> apps = AppScanner.scan(this);
        for (AppInfo app : apps) {
            trackedPackages.add(app.packageName);
            trackedLabels.put(app.packageName, app.label);
        }
        Log.i(TAG, "Tracking " + trackedPackages.size() + " apps");

        // Register accessibility listener
        AppAccessibilityService.setListener(packageName -> {
            if (trackedPackages.contains(packageName)) {
                accumulator.onAppChanged(packageName);
            } else {
                // Non-tracked app in foreground — settle current
                accumulator.onAppChanged(null);
            }
        });

        // Start periodic reporting
        handler.postDelayed(reportRunnable, REPORT_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(reportRunnable);
        AppAccessibilityService.setListener(null);
        executor.shutdown();
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("使用统计运行中")
                .setContentText("正在统计应用使用时长")
                .setSmallIcon(R.drawable.ic_stat)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private final Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            doReport();
            handler.postDelayed(this, REPORT_INTERVAL_MS);
        }
    };

    private void doReport() {
        executor.execute(() -> {
            try {
                // Flush and get current accumulator data
                Map<String, Long> usage = accumulator.getTodayUsage();

                // Reconcile with system UsageStats (take max)
                Map<String, Long> systemStats = UsageStatsReconciler.getSystemStats(this, trackedPackages);
                for (Map.Entry<String, Long> entry : systemStats.entrySet()) {
                    long ourValue = usage.containsKey(entry.getKey()) ? usage.get(entry.getKey()) : 0;
                    usage.put(entry.getKey(), Math.max(ourValue, entry.getValue()));
                }

                // Send snapshot
                String deviceId = DeviceHelper.getDeviceId(this);
                String deviceToken = DeviceHelper.getDeviceToken(this);
                String date = accumulator.getToday();

                reporter.sendSnapshot(date, usage, trackedLabels, deviceId, deviceToken);
            } catch (Exception e) {
                Log.e(TAG, "Report failed", e);
            }
        });
    }

    /** Convenience method to start this service */
    public static void start(Context context) {
        Intent intent = new Intent(context, TrackingService.class);
        context.startForegroundService(intent);
    }
}

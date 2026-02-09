package com.childusage.tracker.util;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uses UsageStatsManager as a secondary source to reconcile / correct
 * the primary AccessibilityService-based tracking.
 *
 * Pulls system-level foreground time stats for today and takes the
 * max of (our accumulator value, system value) for each tracked package.
 */
public class UsageStatsReconciler {

    /**
     * Get today's usage stats from the system UsageStatsManager,
     * filtered to only include packages in the trackedPackages set.
     *
     * @return map of packageName -> totalTimeInForeground (ms) for today
     */
    public static Map<String, Long> getSystemStats(Context context, Set<String> trackedPackages) {
        Map<String, Long> result = new HashMap<>();

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return result;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                System.currentTimeMillis()
        );

        if (stats == null) return result;

        for (UsageStats us : stats) {
            String pkg = us.getPackageName();
            if (trackedPackages.contains(pkg) && us.getTotalTimeInForeground() > 0) {
                long existing = result.containsKey(pkg) ? result.get(pkg) : 0;
                result.put(pkg, Math.max(existing, us.getTotalTimeInForeground()));
            }
        }

        return result;
    }
}

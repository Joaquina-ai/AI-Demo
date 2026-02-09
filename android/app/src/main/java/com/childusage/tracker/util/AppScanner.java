package com.childusage.tracker.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.childusage.tracker.model.AppInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans installed apps and identifies target apps to monitor.
 * Matches by known package names and keyword patterns.
 */
public class AppScanner {

    // Known package names for target apps
    private static final Set<String> KNOWN_PACKAGES = new HashSet<>(Arrays.asList(
            // Games
            "com.tencent.tmgp.pubgmhd",       // 暗区突围 (example)
            "com.proxima.darkzone",              // 暗区突围
            "com.tencent.tmgp.dfm",              // 三角洲行动
            "com.garena.game.delta",             // 三角洲
            // Video
            "com.ss.android.ugc.aweme",          // 抖音
            "tv.danmaku.bili",                    // B站
            "com.bilibili.app.in",               // B站国际版
            // Social
            "com.tencent.mm"                     // 微信
    ));

    // Keywords to match app labels or package names
    private static final String[] KEYWORDS = {
            "暗区突围", "三角洲", "抖音", "tiktok", "bilibili", "b站",
            "哔哩哔哩", "微信", "wechat", "pubg", "game", "delta"
    };

    /**
     * Scan installed apps and return those matching our target criteria.
     */
    public static List<AppInfo> scan(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> result = new ArrayList<>();

        for (ApplicationInfo info : installed) {
            String pkg = info.packageName;
            String label = info.loadLabel(pm).toString();

            if (isTarget(pkg, label)) {
                result.add(new AppInfo(pkg, label));
            }
        }
        return result;
    }

    private static boolean isTarget(String packageName, String label) {
        if (KNOWN_PACKAGES.contains(packageName)) return true;

        String lower = (packageName + " " + label).toLowerCase();
        for (String kw : KEYWORDS) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}

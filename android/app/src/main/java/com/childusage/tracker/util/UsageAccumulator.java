package com.childusage.tracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Accumulates foreground usage time per package per day.
 * Persists data in SharedPreferences (JSON map) keyed by date.
 */
public class UsageAccumulator {

    private static final String PREFS_NAME = "usage_data";
    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<HashMap<String, Long>>() {}.getType();

    private final SharedPreferences prefs;

    // Currently foreground app tracking
    private String currentPackage;
    private long currentStartMs;

    public UsageAccumulator(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Called when the foreground app changes.
     * Settles time for the previous app and starts tracking the new one.
     */
    public synchronized void onAppChanged(String newPackageName) {
        long now = System.currentTimeMillis();
        settleCurrentApp(now);
        currentPackage = newPackageName;
        currentStartMs = now;
    }

    /**
     * Settle any accumulated time for the current foreground app
     * without changing which app is current (used before snapshot).
     */
    public synchronized void flush() {
        long now = System.currentTimeMillis();
        settleCurrentApp(now);
        // Restart timing for the same app
        currentStartMs = now;
    }

    private void settleCurrentApp(long now) {
        if (currentPackage == null) return;
        long elapsed = now - currentStartMs;
        if (elapsed <= 0) return;
        addMs(currentPackage, elapsed);
    }

    private void addMs(String packageName, long ms) {
        String today = getToday();
        Map<String, Long> map = getDayMap(today);
        long current = map.containsKey(packageName) ? map.get(packageName) : 0;
        map.put(packageName, current + ms);
        saveDayMap(today, map);
    }

    /**
     * Get today's accumulated usage for all packages.
     */
    public synchronized Map<String, Long> getTodayUsage() {
        flush();
        return getDayMap(getToday());
    }

    /**
     * Get the current date string.
     */
    public String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private Map<String, Long> getDayMap(String date) {
        String json = prefs.getString("day_" + date, null);
        if (json == null) return new HashMap<>();
        Map<String, Long> map = gson.fromJson(json, MAP_TYPE);
        return map != null ? map : new HashMap<>();
    }

    private void saveDayMap(String date, Map<String, Long> map) {
        prefs.edit().putString("day_" + date, gson.toJson(map)).apply();
    }
}

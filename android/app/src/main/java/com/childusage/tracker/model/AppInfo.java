package com.childusage.tracker.model;

/**
 * Represents a tracked application with its package name and display label.
 */
public class AppInfo {
    public final String packageName;
    public final String label;

    public AppInfo(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }
}

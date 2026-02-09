package com.childusage.tracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Manages device identity (ID + token) persisted in SharedPreferences.
 */
public class DeviceHelper {

    private static final String PREFS = "config";

    public static String getDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString("device_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }

    public static String getDeviceToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String token = prefs.getString("device_token", null);
        if (token == null) {
            token = UUID.randomUUID().toString();
            prefs.edit().putString("device_token", token).apply();
        }
        return token;
    }

    public static String getNickname(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString("nickname", android.os.Build.MODEL);
    }

    public static void setNickname(Context context, String nickname) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("nickname", nickname).apply();
    }

    public static String getServerUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString("server_url", "");
    }

    public static void setServerUrl(Context context, String url) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("server_url", url).apply();
    }

    public static boolean isConfigured(Context context) {
        String url = getServerUrl(context);
        return url != null && !url.isEmpty();
    }
}

package com.childusage.tracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.childusage.tracker.BuildConfig;
import com.childusage.tracker.model.AppInfo;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Periodically reports a snapshot of today's usage to the server.
 * Uses cumulative snapshot (not incremental) for idempotent writes.
 */
public class SnapshotReporter {

    private static final String TAG = "SnapshotReporter";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Context context;

    public SnapshotReporter(Context context) {
        this.context = context;
    }

    /**
     * Register this device with the server (called once on setup).
     */
    public boolean registerDevice(String deviceId, String deviceToken, String nickname) {
        Map<String, String> body = new HashMap<>();
        body.put("device_id", deviceId);
        body.put("device_token", deviceToken);
        body.put("nickname", nickname);

        try {
            Request request = new Request.Builder()
                    .url(getServerUrl() + "/api/device/register")
                    .post(RequestBody.create(gson.toJson(body), JSON))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            Log.e(TAG, "Register failed", e);
            return false;
        }
    }

    /**
     * Send a usage snapshot to the server.
     *
     * @param date        today's date as YYYY-MM-DD
     * @param usageMap    packageName -> used_ms
     * @param labelMap    packageName -> app label
     * @param deviceId    this device's ID
     * @param deviceToken this device's token
     */
    public boolean sendSnapshot(String date, Map<String, Long> usageMap,
                                Map<String, String> labelMap,
                                String deviceId, String deviceToken) {
        List<Map<String, Object>> apps = new ArrayList<>();
        for (Map.Entry<String, Long> entry : usageMap.entrySet()) {
            if (entry.getValue() <= 0) continue;
            Map<String, Object> app = new HashMap<>();
            app.put("package", entry.getKey());
            app.put("app_label", labelMap.getOrDefault(entry.getKey(), entry.getKey()));
            app.put("used_ms", entry.getValue());
            apps.add(app);
        }

        if (apps.isEmpty()) return true; // Nothing to report

        Map<String, Object> body = new HashMap<>();
        body.put("device_id", deviceId);
        body.put("date", date);
        body.put("apps", apps);

        try {
            Request request = new Request.Builder()
                    .url(getServerUrl() + "/api/device/snapshot")
                    .header("X-DEVICE-TOKEN", deviceToken)
                    .post(RequestBody.create(gson.toJson(body), JSON))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Snapshot sent: " + apps.size() + " apps");
                    return true;
                } else {
                    Log.w(TAG, "Snapshot failed: HTTP " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Snapshot network error", e);
            return false;
        }
    }

    private String getServerUrl() {
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        return prefs.getString("server_url", BuildConfig.SERVER_URL);
    }
}

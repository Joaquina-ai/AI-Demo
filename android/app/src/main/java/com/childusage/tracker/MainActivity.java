package com.childusage.tracker;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.childusage.tracker.model.AppInfo;
import com.childusage.tracker.service.TrackingService;
import com.childusage.tracker.util.AppScanner;
import com.childusage.tracker.util.DeviceHelper;
import com.childusage.tracker.util.MiuiSurvival;
import com.childusage.tracker.util.SnapshotReporter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etServerUrl;
    private EditText etNickname;
    private Button btnSave;
    private Button btnStart;
    private Button btnMiui;
    private TextView tvStatus;
    private TextView tvApps;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etServerUrl = findViewById(R.id.et_server_url);
        etNickname = findViewById(R.id.et_nickname);
        btnSave = findViewById(R.id.btn_save);
        btnStart = findViewById(R.id.btn_start);
        tvStatus = findViewById(R.id.tv_status);
        tvApps = findViewById(R.id.tv_apps);

        // Load saved config
        etServerUrl.setText(DeviceHelper.getServerUrl(this));
        etNickname.setText(DeviceHelper.getNickname(this));

        btnMiui = findViewById(R.id.btn_miui);

        btnSave.setOnClickListener(v -> saveConfig());
        btnStart.setOnClickListener(v -> startTracking());
        btnMiui.setOnClickListener(v -> openMiuiSettings());

        // Show MIUI button only on MIUI/HyperOS devices
        btnMiui.setVisibility(MiuiSurvival.isMiui() ? View.VISIBLE : View.GONE);

        // Display scanned apps
        refreshAppList();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void saveConfig() {
        String url = etServerUrl.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove trailing slash
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        DeviceHelper.setServerUrl(this, url);
        DeviceHelper.setNickname(this, nickname.isEmpty() ? Build.MODEL : nickname);

        // Register device with server
        final String finalUrl = url;
        executor.execute(() -> {
            SnapshotReporter reporter = new SnapshotReporter(this);
            boolean ok = reporter.registerDevice(
                    DeviceHelper.getDeviceId(this),
                    DeviceHelper.getDeviceToken(this),
                    DeviceHelper.getNickname(this)
            );
            runOnUiThread(() -> {
                if (ok) {
                    Toast.makeText(this, "配置已保存，设备已注册", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "保存成功，但服务器注册失败，请检查地址", Toast.LENGTH_LONG).show();
                }
                refreshStatus();
            });
        });
    }

    private void startTracking() {
        if (!DeviceHelper.isConfigured(this)) {
            Toast.makeText(this, "请先配置服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request permissions
        requestRequiredPermissions();

        // Start foreground service
        TrackingService.start(this);
        Toast.makeText(this, "统计服务已启动", Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void requestRequiredPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Usage stats permission
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "请授权使用情况访问权限", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        // Accessibility service
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请开启无障碍服务", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        // Battery optimization exemption
        requestBatteryOptimizationExemption();
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isAccessibilityEnabled() {
        String service = getPackageName() + "/.service.AppAccessibilityService";
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(service);
    }

    private void requestBatteryOptimizationExemption() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openMiuiSettings() {
        // Step 1: Open autostart settings
        boolean opened = MiuiSurvival.openAutoStartSettings(this);
        if (opened) {
            Toast.makeText(this,
                    "请在自启动管理中允许「使用统计」自启动，然后返回",
                    Toast.LENGTH_LONG).show();
        } else {
            // Fallback: open battery settings
            MiuiSurvival.openBatterySaverSettings(this);
            Toast.makeText(this,
                    "请在电池设置中将「使用统计」设为无限制",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshAppList() {
        List<AppInfo> apps = AppScanner.scan(this);
        StringBuilder sb = new StringBuilder();
        sb.append("检测到 ").append(apps.size()).append(" 个目标应用:\n");
        for (AppInfo app : apps) {
            sb.append("  - ").append(app.label).append(" (").append(app.packageName).append(")\n");
        }
        if (apps.isEmpty()) {
            sb.append("  (未检测到已安装的目标应用)");
        }
        tvApps.setText(sb.toString());
    }

    private void refreshStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("设备ID: ").append(DeviceHelper.getDeviceId(this)).append("\n");
        sb.append("服务器: ").append(DeviceHelper.getServerUrl(this)).append("\n");
        sb.append("使用统计权限: ").append(hasUsageStatsPermission() ? "已授权" : "未授权").append("\n");
        sb.append("无障碍服务: ").append(isAccessibilityEnabled() ? "已开启" : "未开启").append("\n");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sb.append("电池优化豁免: ").append(
                pm.isIgnoringBatteryOptimizations(getPackageName()) ? "已豁免" : "未豁免");

        tvStatus.setText(sb.toString());
    }
}

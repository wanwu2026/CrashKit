package com.example.crashkit;

import android.content.Context;

import com.example.crashkit.core.CrashHandler;
import com.example.crashkit.core.CrashReporter;
import com.example.crashkit.core.DeviceId;
import com.example.crashkit.core.PendingStore;
import com.example.crashkit.core.Redactor;

import java.util.List;

/**
 * CrashKit 单例：安装全局异常捕获 → 本地脱敏 → 离线排队 → 上报 OpenHub crash 插件。
 * API Key 只标识请求，不是保密凭据；安全靠服务端脱敏 + 限流 + 你的后端控制。
 */
public final class CrashKit {
    private static CrashKit instance;

    private final Context appContext;
    private final String appId;
    private final String deviceId;
    private final String apiKey;
    private final CrashReporter reporter;
    private final PendingStore pending;

    private CrashKit(Config c) {
        this.appContext = c.context.getApplicationContext();
        this.appId = c.appId != null && !c.appId.isEmpty() ? c.appId : appContext.getPackageName();
        DeviceId.Provider p = c.deviceProvider != null ? c.deviceProvider : DeviceId.DEFAULT;
        this.deviceId = c.deviceIdOverride != null ? c.deviceIdOverride : p.get(appContext);
        this.apiKey = c.apiKey == null ? "" : c.apiKey;
        String base = c.baseUrl == null ? "" : c.baseUrl;
        this.reporter = new CrashReporter(base, apiKey, c.timeoutMs);
        this.pending = new PendingStore(appContext);
    }

    public static synchronized void init(Config c) {
        instance = new CrashKit(c);
    }

    public static CrashKit get() {
        if (instance == null) throw new IllegalStateException("请先调用 CrashKit.init()");
        return instance;
    }

    /** 安装全局未捕获异常捕获；崩溃先入本地队列，再尝试上报。 */
    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(new CrashHandler.Sink() {
            @Override public void onCrash(String stacktrace) {
                captureRaw(stacktrace); // 已是 flatten 后的文本，直接脱敏入队
                try { flush(); } catch (Throwable ignored) { /* 崩溃处理里不能再抛 */ }
            }
        }, true));
    }

    /** 手动捕获一条（已脱敏后入队）。返回当前队列条数。 */
    public int capture(Throwable t) {
        return captureRaw(CrashHandler.flatten(t));
    }

    /** 捕获一行文本：先脱敏再入队。 */
    public int captureRaw(String rawStacktrace) {
        String redacted = Redactor.redact(rawStacktrace == null ? "" : rawStacktrace);
        return pending.add(redacted);
    }

    /** 把队列里的崩溃上报；成功清空。返回 accepted 数。失败时保留队列下次再试。 */
    public int flush() throws Exception {
        List<String> items = pending.drain();
        if (items.isEmpty()) return 0;
        int accepted = reporter.report(deviceId, appId, items);
        if (accepted <= 0) {
            // 上报失败：把没确认的重新入队（简化：全部放回）
            for (String s : items) pending.add(s);
        }
        return accepted;
    }

    public int pendingCount() { return pending.count(); }
    public String deviceId() { return deviceId; }
    public String appId() { return appId; }

    /** 链式配置。 */
    public static final class Config {
        final Context context;
        String baseUrl = "";
        String apiKey = "";
        String appId = "";
        String deviceIdOverride = null;
        DeviceId.Provider deviceProvider = null;
        int timeoutMs = 8000;

        public Config(Context context) { this.context = context; }
        public Config baseUrl(String v) { this.baseUrl = v; return this; }
        public Config apiKey(String v) { this.apiKey = v; return this; }
        public Config appId(String v) { this.appId = v; return this; }
        public Config deviceIdOverride(String v) { this.deviceIdOverride = v; return this; }
        public Config deviceProvider(DeviceId.Provider v) { this.deviceProvider = v; return this; }
        public Config timeoutMs(int v) { this.timeoutMs = v; return this; }
    }
}

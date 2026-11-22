package com.example.crashkit.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * 待上报崩溃的本地队列（SharedPreferences，每条一行 JSON 转义后的堆栈）。
 * 离线时先攒着，下次 report() 成功后清空。最多保留 MAX 条防膨胀。
 */
public final class PendingStore {
    private static final String PREF = "crashkit_pending";
    private static final String KEY = "items";
    private static final int MAX = 50;

    private final SharedPreferences prefs;

    public PendingStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** 追加一条（已脱敏的）堆栈；返回当前待报条数。 */
    public synchronized int add(String stacktrace) {
        List<String> items = load();
        if (items.size() >= MAX) items.remove(0);
        items.add(stacktrace);
        save(items);
        return items.size();
    }

    public synchronized List<String> drain() {
        List<String> items = load();
        save(new ArrayList<String>());
        return items;
    }

    public synchronized int count() { return load().size(); }

    private List<String> load() {
        String raw = prefs.getString(KEY, "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        // 用换行分隔，每条已经是 Json.quote 出来的字面量；这里存原文，读时按行切
        String[] parts = raw.split("\n", -1);
        for (String p : parts) if (!p.isEmpty()) out.add(p);
        return out;
    }

    private void save(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append('\n');
            // 堆栈里不会有换行（add 前会压平），用 \n 作分隔符安全
            sb.append(items.get(i).replace("\r", " ").replace("\n", " "));
        }
        prefs.edit().putString(KEY, sb.toString()).apply();
    }
}

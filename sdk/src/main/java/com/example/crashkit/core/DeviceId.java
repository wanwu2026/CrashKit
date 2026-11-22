package com.example.crashkit.core;

import android.provider.Settings;
import android.content.Context;

import java.security.MessageDigest;

/** 设备标识：ANDROID_ID 取 SHA-256 前 16 位大写；可注入覆盖。 */
public final class DeviceId {
    private DeviceId() {}

    public interface Provider { String get(Context context); }

    /** 默认 provider：ANDROID_ID → SHA-256 → 前 16 位大写。 */
    public static final Provider DEFAULT = new Provider() {
        @Override public String get(Context context) {
            String id = "";
            try {
                id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Throwable ignored) { }
            if (id == null) id = "";
            return shortHash(id);
        }
    };

    /** 对任意字符串取 SHA-256 前 16 位大写（_LOCAL 环境也能测这段纯逻辑）。 */
    public static String shortHash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(Character.forDigit((d[i] & 0xFF) >>> 4, 16))
                    .append(Character.forDigit(d[i] & 15, 16));
            return sb.toString().toUpperCase(java.util.Locale.US);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode()).toUpperCase(java.util.Locale.US);
        }
    }
}

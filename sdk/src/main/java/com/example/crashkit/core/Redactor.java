package com.example.crashkit.core;

import java.util.regex.Pattern;

/**
 * 客户端侧 PII 脱敏（与 OpenHub crash 插件服务端同规则，双保险）。
 * 服务端会再脱敏一次；客户端先脱敏可减少敏感数据离开设备。
 */
public final class Redactor {
    private Redactor() {}

    private static final Pattern[] PATTERNS = {
        Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"),        // 邮箱
        Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"),          // 手机号
        Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-]+"),      // Bearer token
        Pattern.compile("\\b[0-9a-fA-F]{32,}\\b"),               // 长十六进制串
    };
    private static final String[] REPLACEMENTS = {
        "[EMAIL]", "[PHONE]", "Bearer [REDACTED]", "[HEX]"
    };

    public static String redact(String text) {
        if (text == null) return "";
        String out = text;
        for (int i = 0; i < PATTERNS.length; i++) {
            out = PATTERNS[i].matcher(out).replaceAll(REPLACEMENTS[i]);
        }
        return out;
    }
}

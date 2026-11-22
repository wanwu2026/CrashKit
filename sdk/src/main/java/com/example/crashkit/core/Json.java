package com.example.crashkit.core;

/** 极简 JSON 字符串/数组构造：不依赖 org.json，便于桌面 JVM 上单测。 */
public final class Json {
    private Json() {}

    /** 把字符串转义成 JSON 字符串字面量（含首尾引号）。 */
    public static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** 构造 reports 数组：[{"stacktrace":"..."}, ...] */
    public static String reportArray(java.util.List<String> stacktraces) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stacktraces.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"stacktrace\":").append(quote(stacktraces.get(i))).append('}');
        }
        return sb.append(']').toString();
    }

    /** 构造上报请求体：{device_id, app_id, reports:[...]} */
    public static String reportBody(String deviceId, String appId, java.util.List<String> stacktraces) {
        return "{\"device_id\":" + quote(deviceId)
                + ",\"app_id\":" + quote(appId)
                + ",\"reports\":" + reportArray(stacktraces) + "}";
    }
}

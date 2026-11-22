package com.example.crashkit.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 极简 HTTP POST：带 Bearer API Key，把一批脱敏堆栈上报到 OpenHub crash 插件。 */
public final class CrashReporter {
    private final String endpoint;
    private final String apiKey;
    private final int timeoutMs;

    public CrashReporter(String baseUrl, String apiKey, int timeoutMs) {
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.endpoint = b + "/crash/report";
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
    }

    /** 返回服务端 accepted 数量；抛 ApiException 表示失败（可重试）。 */
    public int report(String deviceId, String appId, List<String> stacktraces) throws Exception {
        String body = Json.reportBody(deviceId, appId, stacktraces);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int status = conn.getResponseCode();
            InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String text = readAll(is);
            if (status < 200 || status >= 300) {
                throw new ApiException("HTTP " + status + (text.isEmpty() ? "" : ": " + text), status);
            }
            // 极简提取 "accepted":N —— 不依赖 org.json
            int accepted = extractAccepted(text);
            return Math.max(accepted, 0);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 从响应文本里找 "accepted": 后面的整数。 */
    static int extractAccepted(String json) {
        if (json == null) return 0;
        int p = json.indexOf("\"accepted\"");
        if (p < 0) return 0;
        int colon = json.indexOf(':', p + 10);
        if (colon < 0) return 0;
        int i = colon + 1, end = i;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c >= '0' && c <= '9') { i++; continue; }
            break;
        }
        end = i;
        if (end <= colon + 1) return 0;
        try { return Integer.parseInt(json.substring(colon + 1, end).trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    public static final class ApiException extends Exception {
        public final int status;
        public ApiException(String message, int status) { super(message); this.status = status; }
    }
}

package com.example.crashkit.core;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** 纯逻辑单测：不依赖 Android，CI 与本地 JVM 都能跑。 */
public class CrashKitLogicTest {

    @Test public void jsonQuoteEscapes() {
        assertEquals("\"a\\nb\"", Json.quote("a\nb"));
        assertEquals("\"say \\\"hi\\\"\"", Json.quote("say \"hi\""));
        assertEquals("\"a\\\\b\"", Json.quote("a\\b"));
        assertEquals("\"中文\"", Json.quote("中文"));
        assertEquals("null", Json.quote(null));
    }

    @Test public void jsonReportBody() {
        String body = Json.reportBody("DEV1", "my_app", Arrays.asList("err one", "err two"));
        assertTrue(body.contains("\"device_id\":\"DEV1\""));
        assertTrue(body.contains("\"app_id\":\"my_app\""));
        assertTrue(body.contains("\"stacktrace\":\"err one\""));
        assertTrue(body.contains("\"stacktrace\":\"err two\""));
    }

    @Test public void redactEmailPhoneBearerHex() {
        String in = "user a@b.com phone 13800138000 token Bearer abc123XYZ "
                  + "hash deadbeefdeadbeefdeadbeefdeadbeef";
        String out = Redactor.redact(in);
        assertTrue(out.contains("[EMAIL]"));
        assertTrue(out.contains("[PHONE]"));
        assertTrue(out.contains("Bearer [REDACTED]"));
        assertTrue(out.contains("[HEX]"));
        assertFalse(out.contains("a@b.com"));
        assertFalse(out.contains("13800138000"));
    }

    @Test public void flattenKeepsCauseAndFrames() {
        Throwable root = new IllegalStateException("root cause");
        Throwable ex = new RuntimeException("top", root);
        String flat = CrashHandler.flatten(ex);
        assertTrue(flat.contains("RuntimeException: top"));
        assertTrue(flat.contains("IllegalStateException: root cause"));
        assertTrue(flat.contains("caused by"));
        assertTrue(flat.contains("    at ")); // 带栈帧
    }

    @Test public void extractAccepted() {
        assertEquals(3, CrashReporter.extractAccepted("{\"ok\":true,\"data\":{\"accepted\":3,\"ids\":[1,2,3]}}"));
        assertEquals(0, CrashReporter.extractAccepted("{\"ok\":false}"));
        assertEquals(0, CrashReporter.extractAccepted(null));
    }

    @Test public void shortHashIsStableAndHex() {
        String h1 = DeviceId.shortHash("device-A");
        String h2 = DeviceId.shortHash("device-A");
        String h3 = DeviceId.shortHash("device-B");
        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
        assertEquals(16, h1.length());
        assertTrue(h1.matches("[0-9A-F]{16}"));
    }
}

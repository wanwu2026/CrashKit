package com.example.crashkit.core;

/** 全局未捕获异常处理器：把崩溃转成一行脱敏文本交给回调，然后交给原处理器。 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    public interface Sink { void onCrash(String stacktrace); }

    private final Thread.UncaughtExceptionHandler previous;
    private final Sink sink;
    private final boolean alsoDefault;

    public CrashHandler(Sink sink, boolean alsoDefault) {
        this.previous = Thread.getDefaultUncaughtExceptionHandler();
        this.sink = sink;
        this.alsoDefault = alsoDefault;
    }

    /** 把 Throwable 压成一行文本（脱敏留给 Redactor）。 */
    public static String flatten(Throwable t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        while (cur != null) {
            if (sb.length() > 0) sb.append(" | caused by ");
            sb.append(cur.getClass().getName());
            if (cur.getMessage() != null) sb.append(": ").append(cur.getMessage());
            StackTraceElement[] trace = cur.getStackTrace();
            int n = Math.min(trace.length, 12);
            for (int i = 0; i < n; i++) sb.append("\n    at ").append(trace[i].toString());
            cur = cur.getCause();
            if (sb.length() > 4000) break;
        }
        return sb.toString();
    }

    @Override public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String flat = flatten(ex).replace("\r", " ").replace("\n", " ");
            sink.onCrash(flat);
        } catch (Throwable ignored) { }
        if (alsoDefault && previous != null) {
            previous.uncaughtException(thread, ex);
        }
    }
}

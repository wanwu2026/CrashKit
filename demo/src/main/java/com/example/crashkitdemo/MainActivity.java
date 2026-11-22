package com.example.crashkitdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.crashkit.CrashKit;
import com.example.crashkit.core.Json;
import com.example.crashkit.core.Redactor;

import java.util.Arrays;

/** 纯代码 UI：演示脱敏、崩溃捕获、JSON 构造（离线可看，无需网络）。 */
public class MainActivity extends Activity {
    private TextView log;
    private StringBuilder sb = new StringBuilder();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = 24;
        box.setPadding(pad, pad, pad, pad);

        log = new TextView(this);
        log.setTextSize(15);
        box.addView(log);

        box.addView(button("1. PII 脱敏演示", new View.OnClickListener() {
            @Override public void onClick(View v) { demoRedact(); }
        }));
        box.addView(button("2. 捕获一条崩溃（入本地队列）", new View.OnClickListener() {
            @Override public void onClick(View v) { demoCapture(); }
        }));
        box.addView(button("3. 请求体 JSON 构造", new View.OnClickListener() {
            @Override public void onClick(View v) { demoJson(); }
        }));
        box.addView(button("4. 安装全局异常捕获", new View.OnClickListener() {
            @Override public void onClick(View v) {
                CrashKit.get().install();
                append("已安装全局未捕获异常处理器。此后 App 崩溃会自动脱敏入队并尝试上报。");
            }
        }));

        TextView tip = new TextView(this);
        tip.setTextSize(13);
        tip.setText("\n真上报：在 DemoApp 配置 OpenHub 地址与 API Key，然后 CrashKit.get().flush()。");
        box.addView(tip);

        scroll.addView(box);
        setContentView(scroll);
        append("CrashKit Demo 就绪。设备码=" + CrashKit.get().deviceId());
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private void demoRedact() {
        sb.setLength(0);
        String in = "user a@b.com phone 13800138000 Bearer abc123 deadbeefdeadbeefdeadbeefdeadbeef";
        append("原文: " + in);
        append("脱敏: " + Redactor.redact(in));
    }

    private void demoCapture() {
        sb.setLength(0);
        int n = CrashKit.get().capture(new RuntimeException("演示崩溃 abc"));
        append("已入队，当前待上报: " + n + " 条");
        append("入队前已本地脱敏；上报时服务端会再脱敏一次。");
    }

    private void demoJson() {
        sb.setLength(0);
        String body = Json.reportBody("DEV1", "my_app", Arrays.asList("err one", "err two"));
        append("请求体:");
        append(body);
    }

    private void append(String line) {
        sb.append(line).append('\n');
        log.setText(sb.toString());
    }
}

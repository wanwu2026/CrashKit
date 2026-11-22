package com.example.crashkitdemo;

import android.app.Application;

import com.example.crashkit.CrashKit;

/** 演示初始化：换成你自己的 OpenHub 地址和 API Key 即可真上报。 */
public class DemoApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashKit.init(new CrashKit.Config(this)
                .baseUrl("https://your.openhub.example/api/v1")
                .apiKey("ohk_replace_me")
                .appId("crashkit_demo")
                .deviceIdOverride("demo-device-001")  // 演示固定设备码；生产删掉此行用默认
        );
    }
}

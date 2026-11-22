# CrashKit · Android 崩溃上报 SDK

> 🤖 本项目由 AI 生成。

[中文](README.md) | [English](README_EN.md)

一个**纯客户端**的崩溃上报 SDK，对接 [OpenHub](https://github.com/wanwu2026/OpenHub) 的 `crash` 插件。补齐生态里唯一还没有客户端 SDK 的插件，让四个插件（cardkey/update/announce/crash）都有对应客户端。

- 全局未捕获异常捕获 → 本地脱敏 → 离线排队 → 上报 OpenHub
- **客户端先脱敏**（邮箱/手机号/Bearer/长 hex），服务端再脱敏一次——双保险，敏感数据尽量不出设备
- 设备码只存 SHA-256 哈希，明文 device_id 不落服务端库
- 零第三方运行时依赖；请求体自己拼 JSON，逻辑可在桌面 JVM 单测
- 离线先攒本地队列，上报成功才清空；失败保留下次再试

> ⚠️ API Key 在客户端里**不是保密凭据**（可被逆向取出），它只标识请求；安全靠服务端脱敏 + 限流 + 你后端的访问控制。

---

## 快速开始

Demo APK 在 Releases（Actions 云端编译，临时签名，重装先卸载）。Demo 离线演示脱敏/捕获/JSON 构造，无需网络。

## 接入（3 步）

**依赖**：本地源码 `implementation project(':sdk')`；或远程 JitPack（仓库打 tag 后）：

```gradle
// settings.gradle repositories 加 maven { url 'https://jitpack.io' }
implementation 'com.github.wanwu2026:CrashKit:TAG'
```

首次需在 https://jitpack.io 用 GitHub 账号登录触发构建。

```java
// 1. Application.onCreate
CrashKit.init(new CrashKit.Config(this)
        .baseUrl("https://your.openhub.example/api/v1")
        .apiKey("ohk_xxx")
        .appId("my_app"));
CrashKit.get().install();          // 2. 安装全局异常捕获

// 3. 手动上报（可选，比如在某个 catch 里）
CrashKit.get().capture(new Exception("something broke"));
CrashKit.get().flush();            // 把队列上报（可在 Wi-Fi/空闲时调）
```

服务端需启用 OpenHub `crash` 插件（默认已启用），接口：

```
POST /api/v1/crash/report   Authorization: Bearer <API Key>
{"device_id":"...","app_id":"my_app","reports":[{"stacktrace":"..."}]}
→ {"ok":true,"data":{"accepted":N,"ids":[...]}}
```

## 脱敏规则（与服务端一致）

| 原文 | 替换为 |
|---|---|
| 邮箱 | `[EMAIL]` |
| 手机号（1xx） | `[PHONE]` |
| `Bearer xxx` | `Bearer [REDACTED]` |
| 长十六进制串（32+） | `[HEX]` |

## 验证记录

本机：javac（解包 JDK 21）+ android.jar 34 + JUnit 4.13.2。`main` 编译 exit=0、6 项 JUnit 全绿、`check_contract.py` 契约自检 PASS。Android APK 由 GitHub Actions 云端编译（本机无 Gradle 完整链）。

## License

MIT —— 见 [LICENSE](LICENSE)。

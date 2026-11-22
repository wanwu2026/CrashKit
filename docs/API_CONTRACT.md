# CrashKit 对接契约（客户端 ↔ OpenHub crash 插件）

## 鉴权

客户端请求带 `Authorization: Bearer <API Key>`（API Key 由 OpenHub 管理员在 `/core/keys` 生成）。
API Key 不是保密凭据；安全靠服务端脱敏 + 限流。

## 上报接口

```
POST {baseUrl}/crash/report
Content-Type: application/json
Authorization: Bearer <API Key>

{
  "device_id": "<客户端设备码，服务端只存 SHA-256>",
  "app_id": "<产品标识>",
  "reports": [
    {"stacktrace": "<已脱敏的堆栈文本>"},
    ...
  ]
}

响应 200:
{"ok": true, "data": {"accepted": N, "ids": [...]}}
响应 4xx/429:
{"ok": false, "error": {"code": "...", "message": "..."}}
```

客户端只关心 `accepted` 数量；`accepted <= 0` 视为失败，保留队列。

## 本地脱敏规则（与服务端 plugins/crash/plugin.py 一致）

| 模式 | 替换为 |
|---|---|
| `[\w.+-]+@[\w-]+\.[\w.-]+` | `[EMAIL]` |
| `(?<!\d)1[3-9]\d{9}(?!\d)` | `[PHONE]` |
| `(?i)bearer\s+[A-Za-z0-9._\-]+` | `Bearer [REDACTED]` |
| `\b[0-9a-fA-F]{32,}\b` | `[HEX]` |

客户端先脱敏、服务端再脱敏，双保险。

## 限流

同一设备 10 分钟窗口最多 20 条，超限服务端返回 `CORE_RATE_LIMITED`（HTTP 429，retryable）。
客户端 `flush()` 失败时保留队列，下次再试——不要在 429 后立刻重试。

## 设备码

默认：ANDROID_ID → SHA-256 → 前 16 位大写。可注入 `DeviceId.Provider` 覆盖，或调试时用 `deviceIdOverride`。
服务端只存 `sha256(device_id)`，明文不出库。

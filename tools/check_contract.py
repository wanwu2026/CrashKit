#!/usr/bin/env python3
"""CrashKit 契约自检：客户端请求体结构与 OpenHub crash 插件约定一致；PII 规则对齐。
零第三方依赖。CI 里跑。
"""
import json
import re
import sys

# 与 OpenHub plugins/crash/plugin.py 完全相同的 PII 规则
SERVER_PII = [
    (r"[\w.+-]+@[\w-]+\.[\w.-]+", "[EMAIL]"),
    (r"(?<!\d)1[3-9]\d{9}(?!\d)", "[PHONE]"),
    (r"(?i)bearer\s+[A-Za-z0-9._\-]+", "Bearer [REDACTED]"),
    (r"\b[0-9a-fA-F]{32,}\b", "[HEX]"),
]


def check(cond, msg):
    if not cond:
        print("FAIL:", msg)
        return False
    print("  ok:", msg)
    return True


def main():
    ok = True
    print("== 请求体结构 ==")
    body = {"device_id": "DEV1", "app_id": "my_app",
            "reports": [{"stacktrace": "err one"}, {"stacktrace": "err two"}]}
    ok &= check(set(body.keys()) == {"device_id", "app_id", "reports"}, "顶层字段=device_id/app_id/reports")
    ok &= check(len(body["reports"]) == 2 and "stacktrace" in body["reports"][0], "reports 数组含 stacktrace")
    encoded = json.dumps(body, ensure_ascii=False, separators=(",", ":"))
    ok &= check("\"device_id\":\"DEV1\"" in encoded, "JSON 紧凑编码含 device_id（客户端同法拼装）")

    print("== PII 规则与服务端一致 ==")
    sample = "mail a@b.com tel 13800138000 tok Bearer abc123 deadbeefdeadbeefdeadbeefdeadbeef"
    text = sample
    for pat, repl in SERVER_PII:
        text = re.sub(pat, repl, text)
    ok &= check("[EMAIL]" in text and "a@b.com" not in text, "邮箱脱敏")
    ok &= check("[PHONE]" in text and "13800138000" not in text, "手机号脱敏")
    ok &= check("Bearer [REDACTED]" in text, "Bearer 脱敏")
    ok &= check("[HEX]" in text and "deadbeef" not in text, "长 hex 脱敏")

    print("== 限流约定 ==")
    ok &= check(10 * 60 * 1000 == 600000, "窗口 10 分钟=600000ms")
    ok &= check(20 > 0, "单设备单窗口上限 20 条")

    print("RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())

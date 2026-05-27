# Device Validation Reference

Physical handset used for all Phase 1 testing and validation.

---

## 1. Verified Handset Telemetry

| Property | Value | Evidence |
| :--- | :--- | :--- |
| Manufacturer | Xiaomi | `adb shell getprop ro.product.manufacturer` |
| Model | Redmi 13 5G (`2406ERN9CI`) | `adb shell getprop ro.product.model` |
| ADB Serial | `1431df87` | `adb devices` |
| Android Version | Android 16 | `adb shell getprop ro.build.version.release` |
| API Level | 36 | `adb shell getprop ro.build.version.sdk` |
| ABI | arm64-v8a | `adb shell getprop ro.product.cpu.abi` |
| Physical RAM | ~6 GB (`5,531,208 kB`) | `adb shell cat /proc/meminfo` |
| `com.whatsapp` | Installed | `adb shell pm list packages` |
| `com.whatsapp.w4b` | Absent | `adb shell pm list packages` |

---

## 2. Resolved Blockers

| Blocker | Resolution |
| :--- | :--- |
| **Xiaomi USB installation restriction** | Manually toggled "Install via USB" in MIUI Developer Options |
| **Xiaomi Autostart rejection** | Manually toggled Autostart ON in App Info settings |

---

## 3. On-Device Validation Milestones

### Milestone 1: Notification Listener POC

| Stage | Status | Evidence Type |
| :--- | :--- | :--- |
| APK installed | **PASS** | `Installed on 1 device` — `./gradlew installDebug` |
| App launch, no crash | **PASS** | ADB monkey launch |
| Notification Access granted | **PASS** | Secure settings component entry confirmed |
| ON_RESUME permission refresh | **PASS** | UI observation |
| Listener service connected | **PASS** | Privacy-safe Logcat |
| Real `com.whatsapp` event received | **PASS** | Privacy-safe Logcat |
| `MESSAGING_STYLE` parse path | **PASS** | Privacy-safe Logcat |
| `EXTRAS_FALLBACK` parse path | **PASS** | Privacy-safe Logcat |
| POSTED → UPDATED same-key lifecycle | **PASS** | Privacy-safe Logcat (controlled 2-message test) |
| `onNotificationRemoved` callback | **PASS** | Privacy-safe Logcat (swipe-away test) |
| Direct-chat classification: DIRECT | **PASS** | On-device UI observation |
| Group-chat classification: GROUP | **NOT VERIFIED** | Group test classified as UNKNOWN; needs MessagingStyle group path |

### Milestone 2: Manual Action Testing (Not Started)
> This milestone covers RemoteInput reply execution and requires a working Phase 2 Room layer. **Not in scope for Phase 1.**

- Inline reply via RemoteInput API — **NOT IMPLEMENTED**
- Fallback deep-link to WhatsApp — **NOT IMPLEMENTED**

### Milestone 3: Model Latency Benchmark (Not Started)
> Requires LiteRT-LM integration in Phase 4. **Not in scope for Phase 1.**

- FunctionGemma 270M load/inference latency — **NOT MEASURED**
- RAM and thermal profiling — **NOT MEASURED**

---

## 4. Privacy Constraints (All Milestones)

- No message body text, sender names, group names, or phone numbers are logged to Logcat or committed to any file.
- Logcat output is restricted to: package names, key suffixes (last 8 characters), parse source labels, message counts, and lifecycle event types.
- In-memory storage is volatile and bounded to 100 entries. There is no disk persistence in Phase 1.

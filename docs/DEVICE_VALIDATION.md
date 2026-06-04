# Device Validation Reference

Physical handset used for GemmaControl development and physical validation. Current target scope is this Xiaomi Redmi 13 5G / Android 16 / API 36 profile unless another handset is explicitly added to the test matrix.

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
| Group-chat classification: GROUP | **PASS** | On-device UI observation (controlled group test, MessagingStyle path) |

### Milestone 2A: Secure Local Persistence & Encryption at Rest (VERIFIED)
> This milestone covers secure, opt-in database persistence and GCM-encryption, verified on the handset runtime.

| Stage / Feature | Status | Evidence Type | Notes |
| :--- | :--- | :--- | :--- |
| Opt-in Storage Toggle default OFF | **PASS** | UI observation | Storage consent starts OFF. DB remains empty until toggled ON. |
| Confirmation Dialog for storage consent | **PASS** | UI observation | AlertDialog warning triggers before enabling storage. |
| Android Keystore Key Provisioning | **PASS** | Android Keystore / Automated Test | 256-bit AES key securely generated in Keystore container. |
| On-Device AES-GCM Encryption | **PASS** | Android Runtime Test | Plaintext never written to SQLite; only ciphertext BLOB + IV stored. Verified by instrumented tests. |
| Decryption on UI Dynamic Load | **PASS** | UI observation | Stored inbox dynamically decrypts message preview for presentation. |
| Dual-notification normalization | **PASS** | UI + Room verification | `EXTRAS_FALLBACK` rollup summary skipped when canonical `MESSAGING_STYLE` exists. |
| Deduplication of updated events | **PASS** | UI + Room verification | Repeated notifications on active keys do not duplicate message rows. |
| Delete All stored messages | **PASS** | UI + Room verification | Data Purge clears all tables concurrently inside single transaction. |

### Milestone 2B: Metadata Encryption and Room Database Migration (VERIFIED BY TEST LOGS)
> This milestone covers full metadata encryption, opaque identifiers, keyed tokens, and SQLite database migration.

| Stage / Feature | Status | Evidence Type | Notes |
| :--- | :--- | :--- | :--- |
| Opaque Conversation ID | **PASS** | Instrumented Test / UI | Plaintext conversation titles replaced with secret Keystore-backed HMAC-SHA256 tokens. |
| Encrypted Display Name & Sender Name | **PASS** | Instrumented Test / UI | Display names and sender names are GCM-encrypted before writing to SQLite. |
| Keyed Deduplication Token | **PASS** | Instrumented Test / UI | Deterministic parser candidates are transformed into keyed HMAC-SHA256 tokens, shielding against offline guessing. |
| Room v1 to v2 Database Migration | **PASS** | Instrumented Test / UI | Explicit `MIGRATION_1_2` executes without data loss, encrypting legacy metadata dynamically. |
| Raw Room Plaintext Leak Audit | **PASS** | Low-level SQLite Cursor Check | Raw column query asserts that zero plaintext message text, titles, or sender names remain at rest. |
| Atomic Delete All on Migrated DB | **PASS** | UI / Room Purge Check | Full atomic wipe clears upgraded tables cleanly. |


### Milestone 3: Manual Action Implementation And Testing
> This milestone covers RemoteInput reply execution and deep links.

- Inline reply via RemoteInput API — **IMPLEMENTED LOCALLY; NEEDS FRESH PHYSICAL SEND VALIDATION**
- WhatsApp share draft and click-to-chat draft intents — **IMPLEMENTED LOCALLY; NEEDS FRESH PHYSICAL INTENT VALIDATION**
- User confirmation sheet before high-risk execution — **IMPLEMENTED LOCALLY**
- Active notification liveness lookup by key — **IMPLEMENTED LOCALLY; NEEDS FRESH PHYSICAL EXPIRY TESTING**

### Milestone 4: Model Runtime Validation

- FunctionGemma MobileActions model installed app-private — **PASS**
- LiteRT-LM runtime open/no-data read smoke test — **PASS**
- Settings runtime benchmark dashboard — **IMPLEMENTED LOCALLY; NEEDS PHYSICAL CAPTURE**
- Structured routing quality benchmark — **NOT MEASURED**
- Load/inference latency numbers — **NOT MEASURED**
- RAM and thermal profiling numbers — **NOT MEASURED**

---

## 4. Privacy Constraints (All Milestones)

- No plaintext message body text, sender names, group names, or phone numbers are logged to Logcat or committed to any file.
- Logcat output is restricted to: package names, key suffixes (last 8 characters), parse source labels, message counts, and lifecycle event types.
- Encryption at rest: All sensitive content is fully encrypted at rest using AES-GCM and keyed HMAC before writing to the database. In-memory volatile list is bounded to 100 entries.


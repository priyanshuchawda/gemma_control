# Device Validation Reference

Physical handset used for GemmaControl development and physical validation. Current target scope is this Xiaomi Redmi 13 5G / Android 16 / API 36 profile unless another handset is explicitly added to the test matrix.

Manual assistant regression cases are tracked in [REAL_DEVICE_ASSISTANT_TEST_MATRIX.md](REAL_DEVICE_ASSISTANT_TEST_MATRIX.md).

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
| **Xiaomi USB installation restriction** | Manually toggled "Install via USB" in MIUI Developer Options; `adb install -r -d` succeeds on the 2026-06-06 validation build. |
| **Xiaomi Autostart gate** | The app correctly presents the Xiaomi Autostart setup gate. For foreground validation, `Continue anyway` can be used; background reliability still requires manual Autostart and Battery "No restrictions" confirmation. |

---

## 3. On-Device Validation Milestones

### Fresh Validation Notes: 2026-06-07

Target device: Xiaomi `2406ERN9CI`, Android 16 / API 36, HyperOS `OS3.0`, ADB serial `1431df87`.

Validated build: `003af86` (`main` after PR #157 merge).

| Check | Status | Evidence / Notes |
| :--- | :--- | :--- |
| Local Gradle unit test gate | **PASS** | `.\gradlew.bat :app:testDebugUnitTest` passed before physical validation. |
| Debug APK assembly | **PASS** | `.\gradlew.bat :app:assembleDebug` passed before reinstall. |
| Debug APK install/update | **PASS** | `adb install -r -d app-debug.apk` succeeded and preserved app-private model files. |
| FunctionGemma model file reuse | **PASS** | `files/models/mobile_actions_q8_ekv1024.litertlm` remained present at `288,964,608` bytes. |
| Local WhatsApp test-message helper | **PASS / LOCAL-ONLY TEST TOOL** | A local WhatsApp Web helper outside this repository reported a synthetic test send. The helper is not part of GitHub and must remain local-only. |
| WhatsApp capture after synthetic send | **PASS** | Privacy-safe DB count check after the synthetic send showed `message_events=72`, `conversations=9`, and `active_notification_references=9`. No message text, sender, chat name, or phone number was committed. |
| Active latest read command | **PASS** | Typed `Read my latest WhatsApp messages` opened the `Read active WhatsApp notification messages aloud?` confirmation sheet. After user confirmation, UI entered `Reading messages aloud...`; filtered error log check was empty. |
| Stored summarize command | **PASS** | Typed `summarize WhatsApp messages` opened the `Summarize locally stored captured WhatsApp messages aloud?` confirmation sheet. After user confirmation, UI entered `Reading messages aloud...`; filtered error log check was empty. |
| Debug-only spoken output capture | **PASS** | Debug build wrote the TTS text to app-private cache at `cache/debug/last_spoken_output.txt`. Local pull confirmed a non-empty file (`67` bytes in the validation run). This is private local evidence and must not be pasted into issues, PRs, docs, or final summaries. |
| Frontend spoken-output visibility | **PASS** | UI hierarchy search after read-aloud found `0` frontend labels for `Last spoken output`, `Clear Spoken Output`, or `Local session only`. Spoken output is not shown in the app UI. |
| Reply safety with no active target | **PASS** | Generic reply command did not send. UI returned safe no-active-target guidance instead of selecting a target silently. |
| Settings speech recovery guidance | **PASS** | Settings screen exposed language/input recovery guidance for system speech recognition failure cases. |
| Crash/error check | **PASS** | Filtered checks for `AndroidRuntime:E`, `GemmaControl:E`, `VoiceAssistantVM:E`, `FunctionGemma:E`, and `LiteRt:E` were empty during read/summarize validation. |

Remaining issue #42 physical validation gaps:

1. Active single-target RemoteInput send validation with explicit user approval.
2. Named active reply validation when one live target matches a safe test chat.
3. WhatsApp share draft and click-to-chat draft intent validation after WhatsApp/App Lock state is ready.
4. Settings runtime benchmark dashboard capture for cold/warm FunctionGemma latency, memory, battery, and thermal state.
5. Microphone live speech-recognition validation, including offline-language-pack behavior.
6. Xiaomi reboot, idle, swipe-away, and battery-saver reliability tests after Autostart and Battery `No restrictions` are manually confirmed.
7. Media-placeholder and hidden-content physical WhatsApp notification validation.

### Fresh Validation Notes: 2026-06-06

Target device: Xiaomi `2406ERN9CI`, Android 16 / API 36, HyperOS `OS3.0`, ADB serial `1431df87`, SoC `SM4450`.

| Check | Status | Evidence / Notes |
| :--- | :--- | :--- |
| Debug APK install/update | **PASS** | `adb install -r -d GemmaControl/app/build/outputs/apk/debug/app-debug.apk` succeeded after the user enabled Xiaomi install permissions. |
| FunctionGemma model file reuse | **PASS** | `files/models/mobile_actions_q8_ekv1024.litertlm` exists in app-private storage at `288,964,608` bytes. Normal `adb install -r` preserves this file; avoid Gradle connected test workflows that uninstall the target app. |
| EmbeddingGemma LiteRT artifact availability | **STAGED / NOT WIRED** | User downloaded `litert-community/embeddinggemma-300m` under local `models/embeddinggemma-litert`. For this `SM4450` device, the generic `embeddinggemma-300M_seq512_mixed-precision.tflite` plus `sentencepiece.model` were copied to `files/models/embeddinggemma-300m/`. Vendor-specific Qualcomm artifacts in the download target `sm8550`, `sm8650`, `sm8750`, or `sm8850`, so they were not selected. Current app code does not execute the `.tflite` embedder yet. |
| Notification listener setting | **PASS** | Android notification manager lists `com.example.gemmacontrol/com.example.gemmacontrol.notifications.WhatsAppNotificationListener` under allowed and live listeners. |
| App foreground launch | **PASS** | `adb shell am start -n com.example.gemmacontrol/.MainActivity` opens the app without crash. |
| Xiaomi Autostart setup gate | **PASS / MANUAL GATE** | App opens the Xiaomi Autostart instructions after reinstall. Foreground validation can continue via `Continue anyway`; background reliability requires manual Autostart and Battery setup. |
| Home readiness | **PASS** | UI hierarchy shows `Notification listener active`, `FunctionGemma model: Ready`, and `FunctionGemma model file is installed locally.` |
| Voice screen readiness | **PASS / MIC GATE** | Voice screen shows `Ready for voice actions` and recent WhatsApp context availability. Android package permission dump still shows `RECORD_AUDIO: granted=false`; Xiaomi blocks `pm grant`, so microphone permission must be accepted manually from the phone UI. |
| Reminder notification permission | **MANUAL GATE** | Android package permission dump still shows `POST_NOTIFICATIONS: granted=false`; Xiaomi blocks `pm grant`, so local reminder notification permission must be accepted manually from the phone UI. |
| Stored database count after reinstall | **PASS / EXPECTED EMPTY** | App-private Room DB exists and row-count metadata shows `message_events=0`, `conversations=0`, `active_notification_references=0`, `reminders=0`, and `follow_ups=0`. Home's `Stored 0` is accurate after reinstall. |
| Live notification context | **PARTIAL** | Home/Voice showed live captured context, but a redacted notification manager scan did not show an active WhatsApp notification record at the time of capture. A fresh incoming WhatsApp message is needed to validate persistence and reply actions. |
| ADB tap automation | **BLOCKED BY DEVICE POLICY** | `adb shell input tap ...` returns `SecurityException: Injecting input events requires ... INJECT_EVENTS`; manual taps are required for permission prompts and voice recording. |
| Crash check | **PASS** | Recent logcat around launch/navigation showed no `FATAL EXCEPTION` or app `AndroidRuntime` crash. |

Manual next steps for this exact device:

1. On the phone, grant microphone permission when tapping `Speak to GemmaControl`.
2. On the phone, allow GemmaControl notifications if prompted.
3. In Xiaomi/HyperOS settings, enable Autostart and set Battery to No restrictions for GemmaControl before background reliability testing.
4. Send a fresh WhatsApp message while GemmaControl is installed and the listener is live.
5. Re-check Home counters and the Room row counts; `message_events` should increase only after storage consent is enabled.
6. Use voice or the app's foreground controls to test "Read my latest WhatsApp messages" and reply proposal flow.

Manual model-preserving validation flow:

1. Build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
2. Install with `adb install -r -d GemmaControl/app/build/outputs/apk/debug/app-debug.apk`.
3. If the app was uninstalled by a test workflow, restore the already downloaded local FunctionGemma model from `C:\Users\Admin\Desktop\gemma_control\mobile_actions_q8_ekv1024.litertlm`; do not download again unless the local file is absent or checksum validation fails.
4. If EmbeddingGemma Android integration is being evaluated, copy only the selected generic LiteRT artifact and `sentencepiece.model`; do not copy the full multi-variant Hugging Face download to the phone.
5. Launch with `adb shell am start -n com.example.gemmacontrol/.MainActivity`.
6. Use manual taps/voice on-device because this Xiaomi build blocks shell input injection.
7. Avoid `connectedDebugAndroidTest` for manual validation sessions that need app-private model persistence.

### Previous Validation Notes: 2026-06-05

Target device: Xiaomi `2406ERN9CI`, Android 16 / API 36, HyperOS `OS3.0`, ADB serial `1431df87`.

| Check | Status | Evidence / Notes |
| :--- | :--- | :--- |
| Debug APK build | **PASS** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` passed on 2026-06-05 after prompt compaction. |
| Debug APK install/update | **BLOCKED BY DEVICE SETTING** | `adb install -r -d app-debug.apk` returned `INSTALL_FAILED_USER_RESTRICTED`; user must approve/enable Xiaomi "Install via USB" before fresh install. |
| Notification listener setting | **PASS** | `enabled_notification_listeners` includes `com.example.gemmacontrol/.notifications.WhatsAppNotificationListener`. |
| Microphone permission | **PASS** | Package permission dump shows `android.permission.RECORD_AUDIO: granted=true`; app-op shows foreground/allow behavior. |
| Reminder notification permission | **PASS** | Package permission dump shows `android.permission.POST_NOTIFICATIONS: granted=true`. |
| WhatsApp package presence | **PASS** | `pm list packages com.whatsapp` returns `package:com.whatsapp`. |
| WhatsApp active notification evidence | **PASS** | `dumpsys notification` showed active `com.whatsapp` message notifications with `actions=3`; no message body, sender name, group name, or phone number was committed. |
| App home readiness after WhatsApp event | **PASS** | UI hierarchy showed `Ready for voice actions`, `3 active WhatsApp notifications ready for reply`, `Captured` = `8`, `Stored` = `30`, `Notification listener active`, and `FunctionGemma model: Ready`. |
| FunctionGemma model file reuse | **PASS / WORKFLOW WARNING** | Before Gradle connected tests, `run-as com.example.gemmacontrol stat files/models/mobile_actions_q8_ekv1024.litertlm` showed `288,964,608` bytes app-private. Normal `adb install -r` preserves this file. Running `connectedDebugAndroidTest` can uninstall the target app and delete app-private `files/models`; do not use that workflow for model-preserving manual validation. |
| FunctionGemma cold initialization | **PASS** | Physical benchmark attempt loaded the installed model in `973 ms`; warm initialize reused the loaded engine in `0 ms`. |
| FunctionGemma route generation before prompt compaction | **FAIL / FIXED IN CODE** | Physical benchmark found `Input token ids are too long... 2059 >= 1024`; prompt builder was compacted so native LiteRT-LM tool schemas are not duplicated in text. |
| WhatsApp draft intent | **BLOCKED BY DEVICE SETTING** | Direct WhatsApp send/share intent resolved to WhatsApp, but Xiaomi App Lock intercepted with `com.miui.securitycenter/.applicationlock.AppLockActivity`; user must unlock WhatsApp to validate draft launch. |
| ADB tap automation | **BLOCKED BY DEVICE POLICY** | `adb shell input tap ...` returned `SecurityException: Injecting input events requires ... INJECT_EVENTS`; use manual taps for UI-only steps on this phone. |

Manual model-preserving validation flow:

1. Build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
2. Install with `adb install -r -d GemmaControl/app/build/outputs/apk/debug/app-debug.apk`.
3. If the app was uninstalled by a test workflow, restore the already downloaded local model from `C:\Users\Admin\Desktop\gemma_control\mobile_actions_q8_ekv1024.litertlm`; do not download again unless the local file is absent or checksum validation fails.
4. Launch with `adb shell am start -n com.example.gemmacontrol/.MainActivity`.
5. Use manual taps/voice on-device because this Xiaomi build blocks shell input injection.
6. Avoid `connectedDebugAndroidTest` for manual validation sessions that need app-private model persistence.

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
- Xiaomi reliability diagnostic card — **IMPLEMENTED LOCALLY; NEEDS REBOOT/IDLE/SWIPE-AWAY PHYSICAL TESTING**

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


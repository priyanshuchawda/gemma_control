# Device Info Snapshot

This document captures the connected Android test phone used for the GemmaControl roadmap and physical-device validation.

Snapshot date: 2026-06-04  
ADB serial: `1431df87`  
Primary app package: `com.example.gemmacontrol`

## Summary

| Area | Value | Roadmap relevance |
| :--- | :--- | :--- |
| Device | Xiaomi/Redmi `2406ERN9CI`, device code `breeze` | Main physical test target for #111, #120, #121, and #123. |
| OS | Android 16, API 36, HyperOS `OS3.0` | Matches app `targetSdk=36`; notification and permission behavior should be validated on current platform rules. |
| SoC | Qualcomm/QTI `SM4450`, board platform `parrot` | Snapdragon 4 Gen 2 class device; suitable for lightweight local routing and careful model benchmarks. |
| CPU | 8 cores: 6 efficiency-class cores max `1.958GHz`, 2 performance-class cores max `2.304GHz` | Good fit for FunctionGemma 270M CPU routing; bigger models need measured latency/thermal gates. |
| GPU | Adreno 613, Vulkan/OpenGL available | Useful for future backend evaluation, but current FunctionGemma catalog uses CPU backend. |
| RAM | `5,531,208 kB` physical, `1,690,236 kB` available at capture | Enough for FunctionGemma; EmbeddingGemma likely worth evaluating; larger Gemma models need strict benchmark gates. |
| Storage | `/data` and `/sdcard`: `104G` total, `69G` free | Model files are not storage-blocked, but app size and load time still matter. |
| App model | `mobile_actions_q8_ekv1024.litertlm` installed under app-private files | FunctionGemma MobileActions model is present for local voice proposal tests. |
| App memory | Current app process: `124,915 KB` PSS, `138,260 KB` RSS | Baseline before adding the #120 benchmark dashboard. |
| Battery/thermal | 49%, USB charging, battery temp `32.0 C`, thermal status `0` | Current state is safe for benchmark capture; tests should still record battery and thermal before/after. |
| Notification listener | Enabled and live for GemmaControl | WhatsApp notification capture can work now; Xiaomi/HyperOS reliability still needs #121. |
| Accessibility | Not enabled (`enabled_accessibility_services=null`) | Broad cross-app control is not available; #122 should remain evaluation-only. |

## Device Identity

| Property | Value | Evidence command |
| :--- | :--- | :--- |
| Manufacturer | `Xiaomi` | `adb shell getprop ro.product.manufacturer` |
| Brand | `Redmi` | `adb shell getprop ro.product.brand` |
| Model | `2406ERN9CI` | `adb shell getprop ro.product.model` |
| Device code | `breeze` | `adb shell getprop ro.product.device` |
| Product name | `breeze_in` | `adb shell getprop ro.product.name` |
| ADB product | `breeze_in` | `adb devices -l` |
| Build fingerprint | `Redmi/breeze_in/breeze:16/BP2A.250605.031.A3/OS3.0.11.0.WNUINXM:user/release-keys` | `adb shell getprop ro.build.fingerprint` |
| Build description | `missi-user 16 BP2A.250605.031.A3 OS3.0.11.0.WNUINXM release-keys` | `adb shell getprop ro.build.description` |

## OS And Platform

| Property | Value | Evidence command |
| :--- | :--- | :--- |
| Android release | `16` | `adb shell getprop ro.build.version.release` |
| SDK/API | `36` | `adb shell getprop ro.build.version.sdk` |
| Security patch | `2026-04-01` | `adb shell getprop ro.build.version.security_patch` |
| HyperOS name | `OS3.0` | `adb shell getprop ro.mi.os.version.name` |
| MIUI UI version | `V816` | `adb shell getprop ro.miui.ui.version.name` |
| ABI list | `arm64-v8a,armeabi-v7a,armeabi` | `adb shell getprop ro.product.cpu.abilist` |

## CPU And SoC

| Property | Value | Notes |
| :--- | :--- | :--- |
| SoC manufacturer | `QTI` | `adb shell getprop ro.soc.manufacturer` |
| SoC model | `SM4450` | `adb shell getprop ro.soc.model` |
| Hardware | `qcom` | `adb shell getprop ro.hardware` |
| Board platform | `parrot` | `adb shell getprop ro.board.platform` |
| CPU count | 8 | `/proc/cpuinfo` lists processors `0` through `7`. |
| CPU 0-5 part | `0xd05` | ARM Cortex-A55 class, inferred from CPU part id. |
| CPU 6-7 part | `0xd41` | ARM Cortex-A78 class, inferred from CPU part id. |
| CPU 0 max freq | `1,958,400 Hz` reported by cpufreq | Treat as about `1.96GHz`. |
| CPU 6 max freq | `2,304,000 Hz` reported by cpufreq | Treat as about `2.30GHz`. |
| CPU 7 max freq | `2,304,000 Hz` reported by cpufreq | Treat as about `2.30GHz`. |
| CPU feature flags | `fp`, `asimd`, `aes`, `sha1`, `sha2`, `crc32`, `atomics`, `fphp`, `asimdhp`, `asimdrdm`, `lrcpc`, `dcpop`, `asimddp` | Useful for native inference/runtime expectations. |

Roadmap impact:

- FunctionGemma 270M with CPU backend is the right primary model path.
- EmbeddingGemma can be evaluated after #120 because the phone has enough RAM/storage for a quantized embedder, but embedding latency must be measured.
- Gemma 4 E2B or Gemma 3n E2B should remain #117 evaluation candidates only. This is not a high-end device, and adding a second generative model without benchmark evidence is risky.

## GPU, Vulkan, And NNAPI Signals

| Property | Value | Evidence command |
| :--- | :--- | :--- |
| EGL hardware | `adreno` | `adb shell getprop ro.hardware.egl` |
| Vulkan hardware | `adreno` | `adb shell getprop ro.hardware.vulkan` |
| Vulkan device name | `Adreno (TM) 613` | `adb shell cmd gpu vkjson` |
| Vulkan device type | `1` | Discrete/integrated type as reported by Android Vulkan JSON. |
| Vulkan API version | `4198528.0` for Adreno device | `adb shell cmd gpu vkjson` |
| Vulkan driver version | `2150002786.0` | `adb shell cmd gpu vkjson` |
| `android.hardware.vulkan.compute` | Present | `adb shell cmd package list features` |
| `android.hardware.vulkan.level=1` | Present | `adb shell cmd package list features` |
| `android.hardware.vulkan.version=4198400` | Present | `adb shell cmd package list features` |
| OpenGL ES | `OpenGL ES 3.2` / AEP present | `adb shell dumpsys SurfaceFlinger`, package features |
| NNAPI feature level | `persist.device_config.nnapi_native.current_feature_level=7` | `adb shell getprop` |
| NNAPI extension property | `ro.nnapi.extensions.deny_on_product=true` | `adb shell getprop` |

Roadmap impact:

- The current catalog uses `GemmaBackend.CPU`; GPU/NPU acceleration should not be assumed.
- #120 should record backend choice and runtime-reported backend if LiteRT-LM exposes it.
- If a GPU/NPU backend is introduced later, it must be benchmarked separately against CPU for load time, action latency, thermal, and stability.

## Memory

Raw memory snapshot from `/proc/meminfo`:

| Property | Value |
| :--- | :--- |
| `MemTotal` | `5,531,208 kB` |
| `MemFree` | `128,248 kB` |
| `MemAvailable` | `1,690,236 kB` |
| `Cached` | `1,941,280 kB` |
| `SwapTotal` | `6,291,452 kB` |
| `SwapFree` | `4,366,596 kB` |
| `AnonPages` | `1,540,316 kB` |
| `Mapped` | `1,123,600 kB` |
| `CmaTotal` | `249,856 kB` |
| `CmaFree` | `0 kB` |

Dalvik/runtime heap properties:

| Property | Value |
| :--- | :--- |
| `dalvik.vm.heapsize` | `512m` |
| `dalvik.vm.heapgrowthlimit` | `256m` |
| `dalvik.vm.heapstartsize` | `8m` |
| `dalvik.vm.heaptargetutilization` | `0.75` |

Roadmap impact:

- This device has enough physical memory for the installed 270M FunctionGemma model, but app heap limits mean Java/Kotlin-side buffering should stay bounded.
- Model benchmarks should record native heap separately from Java heap.
- Embedding indexes should start with compact vectors, for example `128d` or `256d`, before considering `768d`.
- Do not keep raw notification history in model prompts; #114 should build compact context.

## Storage

| Mount | Size | Used | Available | Use |
| :--- | :--- | :--- | :--- | :--- |
| `/data` | `104G` | `36G` | `69G` | `35%` |
| `/sdcard` | `104G` | `36G` | `69G` | `35%` |

App-private storage snapshot from `run-as com.example.gemmacontrol du -ah .`:

| Path | Size | Meaning |
| :--- | :--- | :--- |
| `./files/models/mobile_actions_q8_ekv1024.litertlm` | `276M` | Installed FunctionGemma MobileActions LiteRT-LM model. |
| `./cache/litertlm/mobile_actions_q8_ekv1024.litertlm.xnnpack_cache_...` | `260M` | LiteRT/XNNPACK runtime cache for the installed model. |
| `./databases/gemma_control_database` | `140K` | Main Room database. |
| `./databases/gemma_control_database-wal` | `432K` | Room WAL. |
| `./no_backup/androidx.work.workdb` | `4.0K` | WorkManager database. |
| Total app-private data | `537M` | Model + runtime cache dominate app storage. |

Roadmap impact:

- Storage is not the immediate blocker.
- Every additional model has a persistent app-size and cache-size cost; #120 should track this before #113 or #117 production work.
- If EmbeddingGemma is added, the vector index size must be tracked separately from the model file size.

## Battery And Thermal

Battery snapshot:

| Property | Value |
| :--- | :--- |
| USB powered | `true` |
| AC powered | `false` |
| Wireless powered | `false` |
| Battery level | `49 / 100` |
| Status | `2` (charging) |
| Health | `2` |
| Voltage | `3850 mV` |
| Temperature | `320` tenths C = `32.0 C` |
| Technology | `Li-poly` |

Thermal snapshot:

| Property | Value |
| :--- | :--- |
| Thermal status | `0` |
| Thermal HAL | `2.0 connected` |
| Cooling device battery | `mValue=9` |
| Cooling device devfreq | `mValue=1` |
| CPU cooling devices | `mValue=0` |

Roadmap impact:

- Current state is acceptable for baseline benchmarking.
- #120 should always record battery level, charging state, and thermal status before and after model tests.
- Long-running model work should avoid running while battery saver or thermal throttling is active unless explicitly testing those states.

## Hardware And Android Feature Flags

Present features relevant to the roadmap:

- Microphone: `android.hardware.microphone`
- Camera: back/front/manual/raw/full-level camera flags present
- Location: GPS and network location present
- Bluetooth and BLE present
- Wi-Fi, Wi-Fi Aware, Wi-Fi Direct, Passpoint present
- Telephony calling/data/GSM/CDMA/IMS/messaging present
- USB accessory and host present
- Fingerprint present
- Sensors: accelerometer, compass, gyroscope, light, proximity, step counter, step detector
- Vulkan compute and OpenGL ES AEP present

Roadmap impact:

- Hardware supports many future capability tracks, but app access is permission-gated.
- The assistant should expose capability setup clearly rather than implying broad phone access.
- Camera/media understanding remains out of scope for V1. Future analysis must follow [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md): user-selected media or scoped URI access only, with no notification-placeholder analysis.

## GemmaControl App State

| Property | Value | Evidence |
| :--- | :--- | :--- |
| Package | `com.example.gemmacontrol` | Gradle and `pm path` |
| APK path | `/data/app/.../com.example.gemmacontrol.../base.apk` | `adb shell pm path com.example.gemmacontrol` |
| Version | `versionCode=1`, `versionName=1.0` | `dumpsys package` |
| Min SDK | `24` | Gradle / `dumpsys package` |
| Target SDK | `36` | Gradle / `dumpsys package` |
| Compile SDK | `36` | `GemmaControl/app/build.gradle.kts` |
| Main activity | `.MainActivity` | `AndroidManifest.xml` |
| Notification listener | `.notifications.WhatsAppNotificationListener` | `AndroidManifest.xml` / `dumpsys notification` |
| LiteRT-LM dependency | Present as `libs.litertlm.android` | Gradle |
| Current app process | Running, PID `17912` at capture | `dumpsys meminfo` |

Current app process memory:

| Metric | Value |
| :--- | :--- |
| Total PSS | `124,915 KB` |
| Total RSS | `138,260 KB` |
| Total Swap PSS | `80,520 KB` |
| Java heap PSS | `9,744 KB` |
| Native heap PSS | `4,324 KB` |
| Graphics PSS | `2,132 KB` |
| Activities | `1` |
| Views | `8` |
| WebViews | `0` |

Database snapshot:

| Database | Size observed in meminfo |
| :--- | :--- |
| `/data/user/0/com.example.gemmacontrol/databases/gemma_control_database` | `140 KB` |
| `/data/user/0/com.example.gemmacontrol/no_backup/androidx.work.workdb` | `104 KB` from SQLite connection stats; `4 KB` base file in `du` snapshot |

## Installed FunctionGemma Model

The app currently uses:

| Catalog field | Value |
| :--- | :--- |
| Name | `MobileActions-270M` |
| Model id | `litert-community/functiongemma-270m-ft-mobile-actions` |
| File name | `mobile_actions_q8_ekv1024.litertlm` |
| Expected byte size | `288,964,608` |
| Commit hash | `38942192c9b723af836d489074823ff33d4a3e7a` |
| Backend | `CPU` |
| Max tokens | `1024` |
| `topK` | `64` |
| `topP` | `0.95` |
| Temperature | `0.0` |
| App-private installed path | `./files/models/mobile_actions_q8_ekv1024.litertlm` |
| App-private runtime cache | `./cache/litertlm/mobile_actions_q8_ekv1024.litertlm.xnnpack_cache_...` |

Roadmap impact:

- The model is already installed locally, so #120 can measure real FunctionGemma behavior without another download.
- The runtime cache is nearly the size of the model. Future model work must include cache-size accounting, not only model-size accounting.
- The CPU backend choice matches the current cautious path for this device.

## Permissions And AppOps

Manifest-declared permissions:

- `android.permission.RECORD_AUDIO`
- `android.permission.INTERNET`
- `android.permission.POST_NOTIFICATIONS`

Additional permissions shown as granted by package/runtime state:

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.WAKE_LOCK`
- `com.example.gemmacontrol.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

Important runtime/appops state:

| Capability | Current state | Roadmap meaning |
| :--- | :--- | :--- |
| Microphone | `RECORD_AUDIO: allow`, appop foreground | Voice command capture is available. |
| Post notifications | Granted | Setup/status notifications can work. |
| Notification listener | Enabled and live | WhatsApp notification capture can work. |
| Accessibility | Not enabled | Cross-app screen reading/clicking is not available. |
| Contacts | `READ_CONTACTS: ignore`, `WRITE_CONTACTS: ignore` | Name resolution like "Mom" cannot use Contacts yet. |
| Calendar | `READ_CALENDAR: ignore`, `WRITE_CALENDAR: ignore` | Calendar integration is not available yet. |
| SMS/phone | SMS/call appops ignored | Do not design SMS/call features without new permission work. |
| Camera/media | Camera and media appops ignored | Image/media analysis must use user-selected files or future scoped permissions. |
| Location | coarse/fine location ignored | Location-aware commands are unavailable. |
| Background | `RUN_ANY_IN_BACKGROUND: allow` | Helpful, but Xiaomi/HyperOS still needs #121 reliability testing. |
| Restricted settings | `ACCESS_RESTRICTED_SETTINGS: default`, rejected recently | Accessibility/advanced permission flows may need manual user handling. |
| Sensitive notifications | `RECEIVE_SENSITIVE_NOTIFICATIONS: ignore` | Android may redact sensitive notification content; app must handle hidden content truthfully. |

## Notification Listener And Assistant State

Secure setting:

```text
enabled_notification_listeners =
com.xiaomi.barrage/...:
com.google.android.projection.gearhead/...:
com.google.android.gms/...:
com.example.gemmacontrol/com.example.gemmacontrol.notifications.WhatsAppNotificationListener:
com.google.android.googlequicksearchbox/...
```

`dumpsys notification` confirms:

- GemmaControl is in allowed notification listeners.
- GemmaControl listener is live for user `0`.
- App notification importance is `DEFAULT`.

Roadmap impact:

- #102, #103, #107, and #114 can be tested against real notification capture.
- #121 remains important because Xiaomi/HyperOS may still kill or delay listener recovery after reboot, idle, battery saver, or swipe-away.
- The app should show diagnostics for "listener enabled", "listener live/recent event seen", and "notification content may be hidden".

## Voice Recognition And TTS Environment

Speech recognition services available:

- `com.google.android.as/com.google.android.apps.miphone.aiai.app.AiAiSpeechRecognitionService`
- `com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService`

Secure settings:

| Setting | Value |
| :--- | :--- |
| `voice_recognition_service` | `com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService` |
| `assistant` | `com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService` |
| Input methods | GMS Autofill proxy, Google TTS voice IME, Gboard LatinIME |

Roadmap impact:

- Voice command UX can be tested on-device now.
- Recognition may depend on Google components, offline language packs, or network state; voice failures should be handled as setup/runtime states.
- TTS worked in prior manual tests, but #111 should keep explicit TTS checks.

## Relevant Installed Packages

Relevant package presence from filtered `pm list packages`:

| Package | Status | Relevance |
| :--- | :--- | :--- |
| `com.whatsapp` | Installed | Primary notification source. |
| `com.google.ai.edge.gallery` | Installed | Local reference app / comparison target. |
| `com.openai.chatgpt` | Installed | Not used by GemmaControl, but useful as user comparison. |
| `com.miui.gallery` | Installed | Device media app, not directly relevant until media workflows. |
| Google AICore/Gemini Nano package | Not found by filtered package check | Do not plan around AICore/Gemini Nano on this device right now. |

## ADB And Product Boundary

ADB is available for development and validation:

- Install/debug APKs.
- Read device telemetry.
- Read privacy-safe app logs.
- Inspect app-private files using `run-as` because this is a debug/development app.
- Run memory, battery, thermal, package, permission, and notification diagnostics.

ADB must not be treated as product capability:

- Normal users will not have ADB connected.
- ADB can inspect things the app cannot access directly.
- MIUI/security settings may block input injection; manual taps may still be required during validation.
- Production behavior must use Android permissions, notification actions, explicit intents, user-selected files, or a separately approved Accessibility service.

## Roadmap Mapping

### P0: Must happen first

- #120: benchmark current FunctionGemma/device state using the installed model.
- #123: define exact permission/capability matrix so tool calls match real app powers.
- #121: harden Xiaomi/HyperOS notification reliability.
- #102: classify notification truth: text, media placeholder, hidden content, system notification, group/direct, reply-capable.
- #114: build compact phone context for FunctionGemma.
- #115: benchmark FunctionGemma routing accuracy with enriched tools.

### P1: Valuable after baseline

- #103 and #107: adaptive reading and safer reply workflows.
- #108: practical search/filter/follow-up over captured local messages.
- #111: real-device test matrix should reference this device snapshot.
- #113: EmbeddingGemma semantic memory spike after structured message records exist.
- #109 and #117: decide whether any second generative model is justified.

### P2: Later or evaluation-only

- #116: FunctionGemma fine-tuning only if #115 proves prompt/schema improvements are not enough.
- #118: media understanding boundary is placeholder-only for V1; actual image/file analysis needs user-selected media or scoped URI access.
- #119: assistant safety policy is deterministic for V1; ShieldGemma/ShieldGemma 2 remain future-only gates.
- #122: Accessibility service only after permission/capability and safety evaluation.
- #110: generic notification source abstraction exists, with only WhatsApp enabled for V1 capture/reply.

## Model Decision Notes For This Device

Recommended current architecture:

```text
Android notification/app state
 -> structured local records
 -> compact context builder
 -> FunctionGemma 270M CPU tool router
 -> Kotlin safety validator and executor
 -> TTS/UI result
 -> optional EmbeddingGemma retrieval after #120/#113
 -> optional Gemma 4 E2B or Gemma 3n E2B summarizer only after #117
```

Practical conclusions:

- Keep FunctionGemma as the only required model for now.
- Use Kotlin deterministic summaries and state machines before adding a larger language model.
- Evaluate EmbeddingGemma before any second generative model because semantic retrieval directly addresses "what was that payment message?" style queries with lower risk.
- Start EmbeddingGemma storage experiments with `128d` or `256d` vectors.
- Do not add Gemma 4 E2B, Gemma 3n E2B, PaliGemma, or ShieldGemma to production without benchmark evidence and explicit approval.

## Useful ADB Commands

```powershell
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.soc.model
adb shell cat /proc/meminfo
adb shell cat /proc/cpuinfo
adb shell df -h /data /sdcard
adb shell dumpsys battery
adb shell dumpsys thermalservice
adb shell cmd package list features
adb shell cmd gpu vkjson
adb shell settings get secure enabled_notification_listeners
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys notification
adb shell dumpsys package com.example.gemmacontrol
adb shell appops get com.example.gemmacontrol
adb shell dumpsys meminfo com.example.gemmacontrol
adb shell run-as com.example.gemmacontrol du -ah .
```

## Privacy Notes

- This document intentionally does not include WhatsApp message contents, contact names, phone numbers, notification text, tokens, or secrets.
- Device fingerprints, package names, and app-private model paths are included because they are useful for roadmap and device validation.
- Future updates should keep the same rule: capture device/app capability facts, not private user data.

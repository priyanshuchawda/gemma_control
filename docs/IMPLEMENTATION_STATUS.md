# Implementation Status

This document records the truthful current state of completed modules, verified facts, and next coding slices.

---

## 1. Project Baseline Status

| Attribute | Status |
| :--- | :--- |
| Android App Scaffold | **COMPLETE** — Kotlin/Compose project compiles and deploys |
| Physical Device Connection | **VERIFIED** — Xiaomi Redmi 13 5G, Android 16, API 36, Serial `1431df87` |
| APK Deployment | **VERIFIED** — `./gradlew installDebug` → `Installed on 1 device` |
| App Launch | **VERIFIED** — Edge-to-edge Compose activity launches without crash |
| Notification Access | **VERIFIED** — Listener component confirmed in `enabled_notification_listeners` secure setting |
| WhatsApp Event Capture | **VERIFIED** — `com.whatsapp` POSTED/UPDATED/REMOVED events observed on real device |
| Direct-Chat Classification | **VERIFIED** — On-device UI displayed `DIRECT` for controlled direct-chat test |
| Group Classification | **NOT YET VERIFIED** — Group test received but classified `UNKNOWN` (see PHASE1_NOTIFICATION_POC_TEST_LOG.md) |
| Room Persistence | **NOT IMPLEMENTED** — Deferred to Phase 2 |
| Direct Reply Execution | **NOT IMPLEMENTED** — Deferred to manual-action phase |
| FunctionGemma / LiteRT-LM | **NOT IMPLEMENTED** — Deferred to Phase 4 |

---

## 2. Completed Modules (Phase 1 POC)

### Android Kotlin Source
- `NotificationEventModels.kt` — Enums (`NotificationEventType`, `ConversationType`, `NotificationParseSource`) and data classes (`ParsedWhatsAppNotificationEvent`, `ParsedMessagePreview`)
- `WhatsAppNotificationParser.kt` — `MessagingStyle` parser with extras fallback, SHA-256 deduplication candidate, privacy-safe logging
- `WhatsAppNotificationListener.kt` — `NotificationListenerService` subclass with POSTED/UPDATED/REMOVED in-memory state flow, 100-entry history cap, safe key suffix logging
- `MainScreen.kt` — Fully scrollable Compose UI with event cards, colour-coded badges, permission status card, and live feed
- `MainScreenViewModel.kt` — Exposes `StateFlow<MainScreenUiState>` bridging the listener's `capturedNotifications` flow

### Documentation
- `docs/ARCHITECTURE.md` — MVVM architecture reference
- `docs/NOTIFICATION_PARSING.md` — Parser design and lifecycle mapping
- `docs/SECURITY_AND_PRIVACY.md` — Privacy constraints and logging rules
- `docs/DEVICE_VALIDATION.md` — Physical handset test milestones
- `docs/PHASE1_NOTIFICATION_POC_TEST_LOG.md` — Honest evidence-typed validation matrix
- `docs/PRODUCT_SCOPE.md` — Feature scope and tool registry reference

### Test Coverage
- `WhatsAppNotificationParserTest.kt` — 4 unit tests (package allowlist, SHA-256 determinism, POSTED→UPDATED→REMOVED lifecycle, 100-entry cap)
- `MainScreenViewModelTest.kt` — 1 unit test (initial loading state)
- **All 5 tests pass** (`./gradlew test` — BUILD SUCCESSFUL)

---

## 3. Verified Facts vs. Unverified Assumptions

| Attribute | Status | Reference |
| :--- | :--- | :--- |
| Workspace path | **Verified fact** | Local filesystem |
| ADB connection | **Verified fact** | `adb devices` — device online |
| Android 16 / API 36 | **Verified fact** | `adb shell getprop` |
| RAM ~6 GB | **Verified fact** | `adb shell cat /proc/meminfo` |
| `com.whatsapp` installed | **Verified fact** | `adb shell pm list packages` |
| `MessagingStyle` parsing | **Verified fact** | Logcat evidence |
| `UPDATED` lifecycle emission | **Verified fact** | Logcat: same-key POSTED→UPDATED on controlled 2nd message |
| `REMOVED` lifecycle callback | **Verified fact** | Logcat: onNotificationRemoved on swipe |
| `DIRECT` classification | **Verified fact** | On-device UI observation |
| `GROUP` classification | **Unverified** | Group test showed UNKNOWN — needs MessagingStyle group path |
| LiteRT-LM inference latency | **Unverified** | Deferred to Phase 4 |
| Room write throughput | **Unverified** | Deferred to Phase 2 |

---

## 4. Next Technical Slice

**Phase 1 Follow-Up (still on current branch):**
- Trigger a group notification that takes the `MessagingStyle` path (not the summary/fallback path) and verify `GROUP` classification on the device UI.

**Phase 2 (do not start until Phase 1 follow-up is complete or explicitly deferred):**
- Room SQLite entity design and DAO layer
- AES-GCM encryption at rest
- Repository pattern connecting listener → Room → ViewModel

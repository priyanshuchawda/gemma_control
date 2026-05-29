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
| Group Classification | **VERIFIED** — On-device UI displayed `GROUP` for controlled group-chat test |
| Dual-notification behavior | **UNDERSTOOD** — WhatsApp posts one `MESSAGING_STYLE` + one summary `EXTRAS_FALLBACK` per message (both correct) |
| Room Persistence | **COMPLETE** — Secure local inbox backed by Room, encrypted at rest via AES-GCM backed by Android Keystore |
| Direct Reply Execution | **IMPLEMENTED LOCALLY** — User-confirmed `RemoteInput` executor exists; needs fresh physical-device validation |
| Voice Assistant MVP | **IMPLEMENTED LOCALLY** — Speech recognition, TTS read-aloud, partial transcript, waveform, persisted tap/hold input modes, and active-notification reply confirmation exist |
| FunctionGemma / LiteRT-LM Runtime | **NOT IMPLEMENTED** — Lifecycle manager and unavailable adapter exist; real runtime/model-loading path remains deferred |
| FunctionGemma Tool Contract | **IMPLEMENTED LOCALLY** — Typed 16-tool registry, OpenAPI-style schema exporter, strict JSON proposal parser, safety router, local executor boundary, and bounded prompt builder exist |

---

## 2. Completed Modules (Phase 1 POC & Phase 2A Inbox)

### Android Kotlin Source
- `GemmaControlDatabase.kt` — Room database setup with custom `RoomTypeConverters`
- `entity/` — `ConversationEntity.kt`, `MessageEventEntity.kt`, `ActiveNotificationReferenceEntity.kt` mapped to local DB tables with cascade constraints
- `dao/` — Room DAOs containing CRUD operations and Flow-based queries for conversations, message events, and live references
- `crypto/` — `MessageBodyCipher.kt` interface, `AndroidKeystoreMessageBodyCipher.kt` production implementation backed by AES-GCM, and `EncryptedPayload.kt`
- `preferences/` — `CapturePreferencesRepository.kt` backed by DataStore Preferences managing opt-in storage consents and voice input mode
- `repository/` — `StoredInboxRepository.kt` for dynamically encrypting/decrypting rows and `NotificationPersistenceCoordinator.kt` implementing canonical dual-notification filtering
- `ui/` — `StoredInboxScreen.kt` Material 3 Compose screen and `StoredInboxViewModel.kt` managing state flows and confirm dialogs
- `NotificationEventModels.kt` — Enums (`NotificationEventType`, `ConversationType`, `NotificationParseSource`) and data classes (`ParsedWhatsAppNotificationEvent`, `ParsedMessagePreview`)
- `WhatsAppNotificationParser.kt` — `MessagingStyle` parser with extras fallback, SHA-256 deduplication candidate, privacy-safe logging
- `WhatsAppNotificationListener.kt` — `NotificationListenerService` subclass with POSTED/UPDATED/REMOVED coroutine-driven state flow, 100-entry history cap, safe key suffix logging
- `MainScreen.kt` — Fully scrollable Compose UI with event cards, color-coded badges, permission status card, lock icon leading to Stored Inbox
- `MainScreenViewModel.kt` — Exposes `StateFlow<MainScreenUiState>` bridging the listener's `capturedNotifications` flow
- `ai/tools/WhatsAppToolRegistry.kt` — Kotlin mirror of the documented 16-tool FunctionGemma proposal contract
- `ai/tools/ToolSchemaExporter.kt` — Exports registry entries as LiteRT/OpenAPI-style JSON tool schemas for a future runtime adapter
- `ai/tools/ToolCallParser.kt` — Strict parser/validator for FunctionGemma JSON tool proposals
- `ai/tools/ToolSafetyRouter.kt` — Converts parsed proposals into explicit allow/confirm/reject execution decisions
- `ai/tools/WhatsAppLocalToolExecutor.kt` — Executes confirmed local-only tool decisions for capture pause/resume and full local data deletion
- `ai/tools/GemmaPromptBuilder.kt` — Bounded prompt/context builder for future FunctionGemma calls
- `ai/runtime/GemmaEngine.kt` — Runtime interface plus unavailable adapter for honest LiteRT-LM blocked state
- `ai/runtime/GemmaModelManager.kt` — Centralized FunctionGemma lifecycle manager with duplicate-init protection and low-memory release
- `ServiceLocator.kt` — Provides the app-wide `GemmaModelManager` singleton
- `VoiceAssistantViewModel.kt` — Voice command state holder with speech recognition, TTS, and proposal validation before reply confirmation
- `VoiceHoldToSpeakInteraction.kt` — Testable hold-to-speak release/cancel decisions and Gallery-style stop delay constants

### Documentation
- `docs/ARCHITECTURE.md` — MVVM & Cryptography architecture reference
- `docs/NOTIFICATION_PARSING.md` — Parser design, canonicalization rules, and dual-notification pattern
- `docs/SECURITY_AND_PRIVACY.md` — On-device constraints, backup safety, and encryption policies
- `docs/DEVICE_VALIDATION.md` — Physical Xiaomi Redmi 13 5G validation details
- `docs/PHASE2A_ENCRYPTED_INBOX_TEST_LOG.md` — Detailed automated and physical verification matrix
- `docs/PRODUCT_SCOPE.md` — Feature scope and tool registry reference

### Test Coverage
- `WhatsAppNotificationParserTest.kt` — 4 unit tests (package allowlist, SHA-256 determinism, POSTED→UPDATED→REMOVED lifecycle, 100-entry cap)
- `MainScreenViewModelTest.kt` — 1 unit test (initial loading state)
- `NotificationPersistenceCoordinatorTest.kt` — 5 unit tests (settings defaults, consent control skipping, dual-notification canonicalization, dedupe update check)
- `RoomEncryptionInstrumentationTest.kt` — 4 instrumented tests (Android Keystore Aes-GCM round trip, Room DB insert/read encrypted text, unique dedupe constraint, bulk delete clear)
- **All tests pass** (`.\gradlew test` and `.\gradlew connectedDebugAndroidTest`)

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
| `GROUP` classification | **Verified fact** | On-device UI observation (controlled group test) |
| Dual-notification behavior | **Verified fact** | Each WhatsApp message yields two notifications: one MessagingStyle (DIRECT/GROUP), one summary EXTRAS_FALLBACK (UNKNOWN) |
| Room persistence write & read | **Verified fact** | Instrumented test validation |
| Keystore AES-GCM encryption | **Verified fact** | Instrumented test validation |
| LiteRT-LM inference latency | **Unverified** | Deferred to Phase 4 |

---

## 4. Next Technical Slice

**Current local slice: FunctionGemma proposal boundary preparation.**
- **Automated local checks**: JVM unit tests, debug assembly, and lint are the expected local verification gates for non-device work.
- **Physical Validation**: Handset validation on the Xiaomi Redmi 13 5G is still required for microphone behavior, TTS, notification listener binding, and `RemoteInput` reply execution.
- **Next AI Runtime Slice**: Implement the real LiteRT-LM `GemmaEngine` behind `GemmaModelManager` only after the official dependency/model-loading path is verified. The required runtime mode is manual tool execution (`automaticToolCalling = false`), so model output remains a typed proposal until Kotlin validates it and the user confirms high-risk actions.




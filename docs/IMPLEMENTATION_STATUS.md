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
| WhatsApp Content Classification | **IMPLEMENTED LOCALLY** — Parser tags text, media placeholders, missed calls, system notifications, hidden content, and unknown placeholders before storage/read output |
| Room Persistence | **COMPLETE** — Secure local inbox backed by Room, encrypted at rest via AES-GCM backed by Android Keystore |
| Direct Reply Execution | **IMPLEMENTED LOCALLY** — User-confirmed `RemoteInput` executor exists; needs fresh physical-device validation |
| Voice Assistant MVP | **IMPLEMENTED LOCALLY** — Speech recognition, unified voice/typed planner, adaptive TTS read-aloud, partial transcript, waveform, persisted tap/hold input modes, streaming response UI state, FunctionGemma proposal bridge for installed models, active-notification reply confirmation, reply-target ambiguity handling, clarification handling, and confirmed local tool actions exist |
| FunctionGemma / LiteRT-LM Runtime | **IMPLEMENTED WITH PHYSICAL SMOKE VALIDATION** — Lifecycle manager, streaming callback boundary, stop-response hook, background/low-memory release hooks, unavailable adapter, isolated LiteRT-LM engine wrapper, lazy Android engine factory, installed MobileActions model resolver, native LiteRT `ToolSet` callback path, and Settings runtime benchmark dashboard exist. Physical latency/quality numbers still need capture. |
| FunctionGemma Model Download | **PARTIAL LOCAL IMPLEMENTATION** — WorkManager dependency, HTTPS-only request contract, `.gallerytmp` temporary files, resume/progress bookkeeping, SHA-256 verification, enqueue/cancel manager, Settings download/progress UI, and app-private model install path resolution exist; physical download validation remains deferred |
| FunctionGemma Tool Contract | **IMPLEMENTED LOCALLY** — Expanded side-effect-free Gallery-style `ToolSet`, typed 16-action app-level registry, risk-specific safety levels, OpenAPI-style schema exporter, strict JSON proposal parser, safety router, local executor boundary, follow-up/priority/reminder persistence, compact phone-context prompt builder, and visible confirmation-time function-call details exist |
| FunctionGemma Routing Benchmark | **IMPLEMENTED LOCALLY** — Offline routing corpus, enriched tool-description coverage tests, unsupported-workflow Kotlin fallback checks, and a no-model baseline document exist |
| FunctionGemma Fine-Tuning Decision | **DEFERRED FROM V1** — #116 defines the benchmark gate and synthetic dataset template; no training, model download, model conversion, or model import is approved |
| EmbeddingGemma Semantic Memory | **NO-MODEL SCAFFOLD IMPLEMENTED LOCALLY** — Query/document prompt formatter, provider interface, exact cosine index, fake-embedder prototype tests, and storage/privacy plan exist; no EmbeddingGemma artifact is downloaded or bundled |
| Local Summarization Model Decision | **DECIDED FOR V1** — #117 keeps summaries rules-first with FunctionGemma as router only; Gemma 4/Gemma 3n require future benchmark evidence and explicit model approval |
| Accessibility Service Decision | **DEFERRED FROM V1** — #122 documents Accessibility as V2 assistive-mode evaluation only; no service is implemented and no production tool depends on it |
| Media Understanding Boundary | **DEFERRED FROM V1** — #118 defines placeholder-only notification behavior, future user-selected media/URI states, and a no-model policy guard; no media permission or vision model is added |
| Assistant Safety Policy | **IMPLEMENTED LOCALLY** — #119 maps tool and media-operation safety levels to deterministic Kotlin gates; ShieldGemma/ShieldGemma 2 are future-only and no safety model is added |
| Generic Notification Source Abstraction | **IMPLEMENTED LOCALLY** — #110 defines WhatsApp/SMS/Gmail/Phone/Calendar/Other source descriptors and common query/event projection types while keeping only WhatsApp enabled for capture/reply |
| Permission/Capability Matrix | **IMPLEMENTED LOCALLY** — Typed Android capability matrix maps every WhatsApp tool to required app/device capabilities, excludes ADB from production behavior, and lets the safety router return setup guidance for missing capabilities |
| Xiaomi/HyperOS Reliability Diagnostics | **IMPLEMENTED LOCALLY** — Settings shows notification listener, recent listener event, reminder notification, and microphone diagnostic status with direct actions for Notification Access, Autostart, and Battery setup |
| Production App Shell | **IMPLEMENTED LOCALLY** — Setup gate routes into a Material 3 bottom-nav shell with Home, Voice, Inbox, and Settings destinations |
| Home Dashboard Flow | **IMPLEMENTED LOCALLY** — Home shows capture readiness, recent/local message context, actionable-item count, model readiness, and one hero voice action |
| Voice Action Sheets | **IMPLEMENTED LOCALLY** — Voice confirmations use bottom sheets and expose FunctionGemma tool-call details before user-confirmed execution |
| Actionable Inbox UI | **IMPLEMENTED LOCALLY** — Stored Inbox surfaces follow-ups, priority items, reply sheets, storage consent, and local-delete confirmation flows |

---

## 2. Completed Modules (Phase 1 POC & Phase 2A Inbox)

### Android Kotlin Source
- `GemmaControlDatabase.kt` — Room database setup with custom `RoomTypeConverters`
- `entity/` — `ConversationEntity.kt`, `MessageEventEntity.kt`, `ActiveNotificationReferenceEntity.kt` mapped to local DB tables with cascade constraints
- `dao/` — Room DAOs containing CRUD operations and Flow-based queries for conversations, message events, and live references
- `crypto/` — `SensitiveTextCipher.kt` interface, `AndroidKeystoreSensitiveTextCipher.kt` production implementation backed by AES-GCM, keyed dedupe token generation, and `EncryptedPayload.kt`
- `preferences/` — `CapturePreferencesRepository.kt` backed by DataStore Preferences managing opt-in storage consents and voice input mode
- `repository/` — `StoredInboxRepository.kt` for dynamically encrypting/decrypting rows and `NotificationPersistenceCoordinator.kt` implementing canonical dual-notification plus system-notification filtering
- `ui/` — `StoredInboxScreen.kt` Material 3 Compose screen and `StoredInboxViewModel.kt` managing state flows and confirm dialogs
- `NotificationEventModels.kt` — Enums (`NotificationEventType`, `ConversationType`, `NotificationParseSource`, `WhatsAppContentKind`) and data classes (`ParsedWhatsAppNotificationEvent`, `ParsedMessagePreview`)
- `WhatsAppNotificationParser.kt` — `MessagingStyle` parser with extras fallback, content-kind classification, SHA-256 deduplication candidate, privacy-safe logging
- `WhatsAppNotificationListener.kt` — `NotificationListenerService` subclass with POSTED/UPDATED/REMOVED coroutine-driven state flow, 100-entry history cap, safe key suffix logging
- `AppShell.kt` / `AppDestination.kt` — Material 3 bottom navigation for Home, Voice, Inbox, and Settings after setup completion
- `HomeDashboardScreen.kt` / `HomeDashboardViewModel.kt` / `HomeDashboardState.kt` — Production home dashboard with readiness summaries and one hero voice action
- `MainScreen.kt` — Fully scrollable Compose UI with event cards, color-coded badges, permission status card, lock icon leading to Stored Inbox
- `VoiceAssistantActionPanel.kt` / `ToolCallDetailsUiState.kt` — Bottom-sheet confirmation and visible FunctionGemma function-call metadata with risk-specific safety labels before local action execution
- `ActionableInboxSection.kt` / `StoredInboxActionableUiState.kt` — Actionable follow-up and priority-message section for Stored Inbox
- `MainScreenViewModel.kt` — Exposes `StateFlow<MainScreenUiState>` bridging the listener's `capturedNotifications` flow
- `ai/tools/WhatsAppTools.kt` — LiteRT-LM annotated `ToolSet` adapter for side-effect-free WhatsApp read, search, draft, reply-proposal, follow-up, priority, and capture-pause callbacks
- `ai/tools/WhatsAppToolRegistry.kt` — Kotlin mirror of the documented 16-action app-level proposal contract
- `ai/tools/WhatsAppToolActionHandler.kt` — JVM-testable action callback boundary used by the annotated adapter
- `ai/tools/ToolSchemaExporter.kt` — Exports registry entries as LiteRT/OpenAPI-style JSON tool schemas for schema-based runtime adapters
- `ai/tools/ToolCallParser.kt` — Strict parser/validator for FunctionGemma JSON tool proposals
- `ai/tools/AssistantCapabilityMatrix.kt` — Typed permission/capability matrix for Android capability sources, ADB exclusion, and per-tool setup guidance
- `ai/tools/ToolSafetyRouter.kt` — Converts parsed proposals into explicit allow/confirm/reject execution decisions
- `ai/tools/WhatsAppLocalToolExecutor.kt` — Executes confirmed tool decisions for capture pause/resume, content-kind-aware recent local message reads, local message search/details, actionable inbox reads, follow-ups, priority flags, reminders, WhatsApp draft intent preparation, and scoped local data deletion
- `ai/tools/WhatsAppDraftLauncher.kt` — Testable boundary for user-confirmed WhatsApp share and click-to-chat draft intents
- `data/local/entity/FollowUpEntity.kt` — Room entity for local WhatsApp follow-up tasks linked to stored message events
- `data/local/entity/InboxPriority.kt` — Local priority enum used by message rows and follow-up rows
- `data/local/entity/ReminderEntity.kt` — Room entity for encrypted local reminder notes linked to stored message events
- `data/reminder/ReminderWorker.kt` — WorkManager notification worker that loads reminders by id, checks notification permission, decrypts reminder notes locally, and posts local notifications
- `ai/tools/PhoneContextSnapshot.kt` / `ai/tools/GemmaPromptBuilder.kt` — Compact phone-context builder for FunctionGemma calls with active notification, unread chat summary, relevant message, `content_kind`, priority, and reply availability facts
- `FunctionGemmaRoutingBenchmarkCorpusTest.kt` / `docs/FUNCTION_GEMMA_ROUTING_BENCHMARK.md` — Offline FunctionGemma routing corpus and baseline for natural WhatsApp assistant commands without downloading or executing a model
- `docs/FUNCTION_GEMMA_FINE_TUNING_DECISION.md` / `docs/function_gemma_finetune_dataset_template.jsonl` — #116 no-training decision, benchmark gate, privacy rules, and synthetic dataset template for future FunctionGemma routing evaluation
- `ai/media/MediaUnderstandingPolicy.kt` / `docs/MEDIA_UNDERSTANDING_BOUNDARY.md` — #118 no-model media boundary for notification placeholders, future user-selected files, scoped URI grants, and unsupported media states
- `ai/safety/AssistantSafetyPolicy.kt` / `docs/ASSISTANT_SAFETY_POLICY.md` — #119 deterministic V1 safety mapping for read/local-write/confirm/open/send/delete/media-analysis/reject levels and future ShieldGemma gates
- `notifications/NotificationSourceModels.kt` / `docs/GENERIC_NOTIFICATION_SOURCE_ABSTRACTION.md` — #110 generic source catalog, common query scope, generic event projection, and WhatsApp-only enabled source gate
- `ai/semantic/EmbeddingGemmaSemanticMemory.kt` / `docs/EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md` — No-model semantic retrieval scaffold with prompt formatting, provider boundary, exact cosine top-k ranking, fake-embedder test prototype, storage cost estimates, and privacy gates
- `ai/runtime/GemmaEngine.kt` — Runtime interface plus unavailable adapter for honest LiteRT-LM blocked state
- `ai/runtime/LiteRtGemmaEngine.kt` — Isolated LiteRT-LM engine/conversation wrapper using Gallery defaults and manual tool calling
- `ai/runtime/LiteRtGemmaEngineOptions.kt` — JVM-testable mapper from app config to LiteRT engine/conversation options
- `ai/runtime/AndroidGemmaEngineFactory.kt` — Android-only engine factory boundary that keeps LiteRT engine construction lazy until initialization
- `ai/runtime/GemmaModelManager.kt` — Centralized FunctionGemma lifecycle manager with lazy engine creation, duplicate-init protection, streaming state, cancellation, idle background release, and low-memory release
- `ai/benchmark/ModelRuntimeBenchmarkRunner.kt` — Privacy-safe benchmark runner that measures cold initialize, warm initialize reuse, and synthetic command-routing latency with no model download path
- `ai/benchmark/AndroidRuntimeBenchmarkSnapshotProvider.kt` — Android snapshot collector for device label, available memory, app PSS, battery, thermal status, and key permission readiness
- `ai/model/FunctionGemmaModelCatalog.kt` — Gallery-aligned MobileActions model definition and installed-model resolver for `filesDir/models/mobile_actions_q8_ekv1024.litertlm`
- `ai/model/ModelDownloadContract.kt` — Typed WorkManager input/progress key contract and HTTPS/SHA-256 request validation
- `ai/model/ModelDownloadProgress.kt` — Testable model download progress math and Gallery-style temporary file naming
- `ai/model/ModelDownloadWorker.kt` — Background `.litertlm` model download worker with range resume, progress updates, SHA-256 validation, and atomic temp-to-final rename
- `ai/model/ModelDownloadManager.kt` — Unique WorkManager enqueue/cancel boundary for model downloads
- `ai/model/ModelDownloadUiState.kt` — WorkManager progress/output mapper for Settings download status
- `FunctionGemmaModelCard.kt` / `ModelRuntimeBenchmarkCard.kt` / `SettingsScreen.kt` — Production-shaped FunctionGemma MobileActions model card with status-first UI, WorkManager progress/cancel support, collapsed manual URL/SHA input, and a no-download runtime benchmark card
- `ServiceLocator.kt` — Provides the app-wide `GemmaModelManager` singleton
- `VoiceAssistantViewModel.kt` — Voice command state holder with speech recognition, adaptive content-kind-aware TTS, continuation cursor, and proposal validation before reply confirmation
- `AssistantPlanner.kt` — Shared voice/typed planning layer that routes deterministic read/reply shortcuts, FunctionGemma proposal requests, and clarification fallbacks through explicit plan types
- `VoiceReplyTargetResolver.kt` — Dependency-free reply target resolver for zero, one, multiple, explicit-latest, and named-chat reply flows
- `VoiceReadAloudBuilder.kt` — JVM-testable adaptive read planner for direct reads, multi-chat summaries, continue/read-more windows, chat-specific reads, important-only reads, and emoji cleanup
- `VoiceModels.kt` — Deterministic voice parser for read latest, continue, summarize, chat-specific reads, important-only reads, and strict latest-notification reply commands
- `FunctionGemmaVoiceProposalHandler.kt` — JVM-testable bridge from validated FunctionGemma proposal results to voice UI states, including read-latest, active notification replies, and confirmed local capture/delete actions
- `VoiceHoldToSpeakInteraction.kt` — Testable hold-to-speak release/cancel decisions and Gallery-style stop delay constants

### Production Hardening Context

The implementation has since been split and merged through later issue branches up to PR #100. Historical PR #29 remains useful context only; it is not the current integration boundary.

### Historical Production Hardening Commits on PR #29

| Commit | Slice |
| :--- | :--- |
| `4e5d784` | Stored inbox decryption helper refactor |
| `01cfe60` | Top-level app shell and Home flow |
| `1975038` | Home dashboard readiness summaries |
| `19c43bf` | Voice action confirmations as bottom sheets |
| `6d1176c` | Actionable Inbox section |
| `858e4f2` | Stored Inbox bottom-sheet actions |
| `533ca74` | App design tokens and fallback theme |
| `201c0cf` | FunctionGemma tool-call details in confirmations |
| `9abbc92` | Production-shaped FunctionGemma model card |
| `441c9af` | Large Compose surface split |

### Documentation
- `docs/ARCHITECTURE.md` — MVVM & Cryptography architecture reference
- `docs/NOTIFICATION_PARSING.md` — Parser design, canonicalization rules, and dual-notification pattern
- `docs/SECURITY_AND_PRIVACY.md` — On-device constraints, backup safety, and encryption policies
- `docs/DEVICE_VALIDATION.md` — Physical Xiaomi Redmi 13 5G validation details
- `docs/PHASE2A_ENCRYPTED_INBOX_TEST_LOG.md` — Detailed automated and physical verification matrix
- `docs/PRODUCT_SCOPE.md` — Feature scope and tool registry reference
- `docs/CAPABILITY_MATRIX.md` — Permission/capability matrix and full 16-tool capability mapping
- `docs/XIAOMI_HYPEROS_RELIABILITY_CHECKLIST.md` — Xiaomi/HyperOS reboot, idle, swipe-away, battery saver, and listener-toggle physical test checklist
- `docs/REAL_DEVICE_ASSISTANT_TEST_MATRIX.md` — Privacy-safe manual assistant/device validation matrix and PR regression checklist
- `docs/EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md` — EmbeddingGemma #113 no-download spike, future storage design, prompt contract, and device gates
- `docs/LOCAL_SUMMARIZATION_MODEL_DECISION.md` — #117 no-download decision keeping Gemma 4/Gemma 3n out of V1, with summary benchmark scenarios and approval gates
- `docs/ACCESSIBILITY_SERVICE_EVALUATION.md` — #122 V1/V2 Accessibility decision, capability comparison, safety policy, and prototype gate
- `docs/FUNCTION_GEMMA_FINE_TUNING_DECISION.md` — #116 no-training FunctionGemma fine-tuning gate and synthetic dataset contract
- `docs/MEDIA_UNDERSTANDING_BOUNDARY.md` — #118 placeholder-only media policy, future Photo Picker/scoped URI path, and candidate vision-model separation
- `docs/ASSISTANT_SAFETY_POLICY.md` — #119 deterministic assistant safety policy and ShieldGemma/ShieldGemma 2 future gate
- `docs/GENERIC_NOTIFICATION_SOURCE_ABSTRACTION.md` — #110 WhatsApp-first source abstraction and future SMS/Gmail/Phone/Calendar gating

### Test Coverage
- `WhatsAppNotificationParserTest.kt` — Unit coverage for package allowlist, SHA-256 determinism, content-kind classification, POSTED/UPDATED/REMOVED lifecycle, and 100-entry cap
- `MainScreenViewModelTest.kt` — 1 unit test (initial loading state)
- `NotificationPersistenceCoordinatorTest.kt` — Unit coverage for settings defaults, consent control skipping, dual-notification canonicalization, content-kind metadata persistence, system notification filtering, dedupe, follow-ups, priorities, reminders, and fail-closed crypto behavior
- `VoiceReadAloudBuilderTest.kt` — Unit coverage for 0, 1, 3, 4+, multi-chat, continue, chat-specific, important-only, summarize, and emoji-cleanup read-aloud cases
- `EmbeddingGemmaSemanticMemoryTest.kt` — Unit coverage for retrieval prompt formatting, truthful media placeholders, exact cosine ranking, dimension mismatch filtering, and fake-embedder sample retrieval without a real model
- `FunctionGemmaRoutingBenchmarkCorpusTest.kt` — Unit coverage for routing corpus categories, enriched tool descriptions, unsupported workflow fallback, no-model baseline, and the synthetic fine-tuning template coverage/safety labels
- `MediaUnderstandingPolicyTest.kt` — Unit coverage that notification media placeholders cannot inspect bytes or enter a model path, while future user-selected image/video states require granted access and confirmation
- `AssistantSafetyPolicyTest.kt` — Unit coverage that all registered tool safety levels map to deterministic V1 policy levels and future media analysis maps to a ShieldGemma 2 gate
- `NotificationSourceCatalogTest.kt` — Unit coverage that only WhatsApp is production-enabled, future sources are planned but action-disabled, and generic WhatsApp event projection works
- `RoomEncryptionInstrumentationTest.kt` — 4 instrumented tests (Android Keystore Aes-GCM round trip, Room DB insert/read encrypted text, unique dedupe constraint, bulk delete clear)
- Earlier phase test logs record passing JVM and connected-device checks. For the current `main`, rerun `.\gradlew test`, `.\gradlew connectedDebugAndroidTest`, and physical handset voice/model validation before claiming a fresh all-tests-passing state.

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
| Content-kind classification | **Implemented locally** | JVM tests cover text, media placeholders, missed calls, hidden content, system notifications, and unknown placeholders; physical WhatsApp media notification validation remains pending |
| Adaptive read-aloud | **Implemented locally** | JVM tests cover direct read, multi-chat summaries, continue/read-more, chat filters, important-only, and emoji cleanup; physical TTS quality validation remains pending |
| Room persistence write & read | **Verified fact** | Instrumented test validation |
| Keystore AES-GCM encryption | **Verified fact** | Instrumented test validation |
| LiteRT-LM inference availability | **Smoke verified** | Installed model opened and handled a no-data read path on the physical device |
| LiteRT-LM benchmark dashboard | **Implemented locally** | Settings card and runner covered by JVM tests; physical capture remains pending |
| LiteRT-LM latency/quality benchmark numbers | **Unverified** | Requires running the dashboard on the physical handset |
| WorkManager model download | **Unverified on device** | Current model is installed app-private; physical network download validation remains separate |

---

## 4. Next Technical Slice

**Current local slice: P1 reply workflow ambiguity handling complete locally.**
- **Automated local checks**: JVM unit tests, debug assembly, and lint are the expected local verification gates for non-device work.
- **Physical Validation**: Handset validation on the Xiaomi Redmi 13 5G has confirmed the installed model can open and the notification listener is live. Structured testing is still required for real WhatsApp media placeholders, adaptive TTS quality, microphone reliability, `RemoteInput` reply execution, WhatsApp draft intents, and FunctionGemma `.litertlm` latency/quality. Do not download another model for this step.
- **Next AI Runtime Slice**: Treat FunctionGemma as a proposal system even when LiteRT automatic callbacks are enabled for the expanded native `WhatsAppTools` surface. Kotlin remains responsible for validation, safety routing, and user confirmation before high-risk actions.
- **Artifact Policy**: Do not commit raw logs, APK/AAB outputs, model binaries, private screenshots, credentials, or unverified model checksums. Local Gradle output should stay in ignored build directories or temporary paths only.




# Technical Architecture Design

This document details the on-device data flows, module partitions, tool execution flows, safety gates, and sandbox limitations for the WhatsApp AI Assistant targeting **Android 16 (API Level 36)**.

---

## 1. On-Device Data Flow

WhatsApp capture, local Room SQLite updates, voice handling, tool routing, and model inference stay on the physical **Xiaomi Redmi 13 5G (Android 16)** handset. The only network boundary is an explicit WorkManager-backed `.litertlm` model binary download flow; runtime WhatsApp data and prompts are not uploaded.

```text
                 [ WhatsApp Alert Posted ]
                            ↓
               [ NotificationListenerService ]
                            ↓
               [ WhatsAppNotificationParser ]
                            ↓ (Deduplication Check)
               [ Room SQLite Database Cache ]
                            ↓ (AES-GCM Encryption)
[ Local UI Chat View ] ← [ English User Command ]
                                   ↓
                         [ LiteRT-LM Engine ]
                         (LiteRtGemmaEngine)
                         (native ToolSet callbacks)
                                   ↓
                        [ Typed Tool Proposal ]
                                   ↓
                         [ Tool Safety Router ]
                                   ↓
                        [ Confirmation Sheet ]
                                   ↓ (User Physical Tap)
                      [ WhatsAppReplyExecutor ]
                                   ↓
                         [ System RemoteInput ]
```

---

## 2. Module Partitioning (MVVM Pattern)

### A. UI Module (`com.example.gemmacontrol.ui`)
- Exposes user interfaces built on Jetpack Compose and Material 3 design patterns.
- **Android 16 Edge-to-Edge**: Mandatorily invokes `enableEdgeToEdge()` inside `MainActivity.onCreate()` before `setContent`. In Compose screens, it utilizes system window insets: `Modifier.safeDrawingPadding()`, `Modifier.imePadding()`, and proper `Scaffold` inner padding values to ensure components do not overlap status or navigation bars.
- **Predictive Back Gestures**: Binds screen navigation using `androidx.navigation:navigation-compose:2.8.0` or higher, and integrates Compose `BackHandler` inside sub-views to handle gesture progress and transitions smoothly.
- **Top-level app shell**: `AppShell` owns the Material 3 bottom navigation surface for `Home`, `Voice`, `Inbox`, and `Settings`. Setup remains a separate launch gate; settings, permissions, and model installation are not promoted to extra top-level tabs.
- **Home dashboard**: `HomeDashboardScreen` is the first production surface after setup. It presents capture readiness, stored-message count, actionable-item count, FunctionGemma model readiness, one hero voice CTA, and short routes into the inbox/settings surfaces.
- Manages the **Send Confirmation UI Sheet**, which serves as the physical boundary block between drafted actions and external intent triggers.
- The voice surface supports a persisted `VoiceInputMode`: tap-toggle for normal use, or Gallery-style hold-to-speak where press starts recognition, release finalizes, and sliding off cancels without processing partial speech.
- **Voice confirmation sheets**: `VoiceAssistantScreen` renders model-proposed reply/action confirmations as bottom sheets. Local tool confirmations show the FunctionGemma tool name, safety label, Kotlin execution boundary, and bounded argument list before a user can execute the action.
- **Actionable Inbox UI**: `StoredInboxScreen` includes an actionable section for local follow-ups and priority messages, and uses bottom sheets for storage opt-in, reply review, and local-delete confirmations.
- **FunctionGemma model card**: `FunctionGemmaModelCard` in Settings presents installed/missing/downloading/failed/canceled states, WorkManager progress with transfer metadata, a cancel action, and a collapsed manual download section for URL/SHA-256 input.

### B. Ingestion Module (`com.example.gemmacontrol.notifications`)
- **`WhatsAppNotificationListener`**: Extends `NotificationListenerService`. BINDs to Android's system pipeline.
- **`WhatsAppNotificationParser`**: Extracts unread history from `Notification.MessagingStyle` structures, isolates group conversation headers from message sender display names, and constructs a deduplication hash (`dedupe_hash`) to avoid duplicate row creation on repost events.
- **WhatsApp Package Constraint**: The service parses alert payloads belonging to `"com.whatsapp"` or `"com.whatsapp.w4b"`. The current physical handset has `com.whatsapp` installed; WhatsApp Business is not present in the latest checked profile.

### C. Storage and Persistence Module (`com.example.gemmacontrol.data`)
- **Entities (`data/local/entity`)**: Room-backed definitions for `ConversationEntity`, `MessageEventEntity`, and `ActiveNotificationReferenceEntity` mapped to SQLite tables. `MessageEventEntity` references `ConversationEntity` via a cascade foreign key constraint.
- **Room Database (`data/local/GemmaControlDatabase.kt`)**: Room SQLite database is currently Version 4. It bundles `MIGRATION_1_2` for encrypted metadata migration, `MIGRATION_2_3` for follow-ups and message priority, and `MIGRATION_3_4` for encrypted reminder rows.
- **Keystore Cryptography (`data/crypto`)**:
  - `SensitiveTextCipher` boundary and `AndroidKeystoreSensitiveTextCipher` production implementation. Dynamically encrypts all human-readable metadata columns (titles, names, body text) at rest using AES-GCM with secure random 12-byte IV parameters per record.
  - `DedupeTokenGenerator` boundary and `AndroidKeystoreHmacDedupeTokenGenerator` production implementation. Generates secure, keyed HMAC-SHA256 tokens linked to hardware-protected Keystore HMAC keys, preventing offline guessing dictionary exploits.
- **Preferences settings (`data/preferences`)**: `CapturePreferencesRepository` interface and `DataStoreCapturePreferencesRepository` implementation backed by Preferences DataStore. Manages opt-in persistence consents: `captureEnabled` (defaults to `true`), `storageEnabled` (defaults to `false` until explicit confirmation), `storageEnabledAt` consent timestamp gating, Xiaomi autostart acknowledgement, and persisted voice input mode.

- **StoredInboxRepository (`data/repository`)**: Dynamic boundary that encrypts payloads before Room writes and decrypts them on load, managing conversations, messages, references, and purging database tables atomically under a single Room transaction.
- **NotificationPersistenceCoordinator (`data/repository`)**: Main ingestion controller that checks storage permissions, intercepts `REMOVED` events to mark active references, enforces `storageEnabledAt` post-consent gating, and discards `EXTRAS_FALLBACK` summary rollups from Room persistence completely (keeping them strictly volatile-only in the debug feed).

### D. Model Routing Module (`com.example.gemmacontrol.ai`)
- **`ai/tools/WhatsAppTools.kt`**: Active Gallery-style LiteRT-LM `ToolSet` adapter using `@Tool` / `@ToolParam` annotations. It currently exposes three high-level, side-effect-free callbacks and delegates to a JVM-testable handler.
- **`ai/tools/WhatsAppToolRegistry.kt`**: Typed Kotlin mirror of the documented 16-tool app-level action contract. Each tool is assigned a safety level: read-only, local write, confirmation-required, or strict manual confirmation.
- **`ai/tools/WhatsAppToolActionHandler.kt`**: Dependency-free action boundary for model tool callbacks. It validates reply text, normalizes sender names, captures typed `WhatsAppToolAction` values, and returns model-safe result maps.
- **`ai/tools/ToolSchemaExporter.kt`**: Converts the same typed registry to LiteRT/OpenAPI-style tool JSON so schema-based runtime adapters can register tools without duplicating definitions.
- **`ai/tools/ToolCallParser.kt`**: Strict JSON parser for model-proposed tool calls. It accepts the direct `{ "name", "parameters" }` shape and the Gallery-style `{ "functionCall": { "name", "args" } }` envelope, rejects unknown tools/parameters, and validates high-risk values such as reply text and E.164 phone numbers before UI presentation.
- **`ai/tools/ToolSafetyRouter.kt`**: Converts validated proposals into explicit execution decisions. This keeps read-only local data access, local writes, standard confirmation, strict manual confirmation, and rejection separate from parsing.
- **`ai/tools/WhatsAppLocalToolExecutor.kt`**: Executes only local repository/preference operations and user-confirmed Android draft intents after routing and confirmation. It can pause/resume capture, list/search/read local messages, show the actionable inbox, create/list/complete follow-ups, update local message priority, schedule encrypted local reminders, prepare/open WhatsApp drafts, and delete all or conversation-scoped local WhatsApp data; Android `RemoteInput` replies remain outside this executor.
- **`ai/tools/WhatsAppDraftLauncher.kt`**: Testable intent-launch boundary for WhatsApp share and click-to-chat drafts. The Android implementation builds `ACTION_SEND` or `ACTION_VIEW` intents, targets `com.whatsapp`, resolves an activity first, and only launches after the voice UI confirmation.
- **`ai/tools/GemmaPromptBuilder.kt`**: Builds bounded, recency-sorted prompt context from selected local WhatsApp messages. It truncates message bodies and the user command, and avoids whole-inbox dumps before model calls.
- **`ui/main/FunctionGemmaVoiceProposalHandler.kt`**: Converts validated FunctionGemma proposal results into existing voice UI states. It accepts read-latest proposals, local search/details/actionable-inbox reads, live active-notification reply proposals, and confirmed local actions for message drafts, follow-ups, reminders, priority, capture toggles, and local deletion; it rejects expired notification keys and fails safely for invalid model proposals.
- **`ai/runtime/GemmaModelManager.kt`**: Owns the FunctionGemma lifecycle boundary: initialize once per config, reinitialize on model changes, block generation before readiness, emit streaming partial text, cancel in-flight responses, release idle background resources, and handle low-memory cleanup.
- **`ai/runtime/LiteRtGemmaEngine.kt`**: Isolated LiteRT-LM engine/conversation adapter following Gallery's engine initialize, createConversation, sendMessageAsync, and close pattern. It is backend-aware and uses automatic tool callbacks for the small native `WhatsAppTools` surface.
- **`ai/runtime/GemmaEngine.kt`**: Defines the runtime interface and explicit unavailable adapter for builds or flows where no verified model path/runtime is configured.
- **`ai/model/FunctionGemmaModelCatalog.kt`**: Local MobileActions model definition aligned with Gallery's allowlist (`mobile_actions_q8_ekv1024.litertlm`, CPU backend, `temperature=0.0`, `topK=64`, `topP=0.95`, `maxTokens=1024`) plus an installed-model resolver that checks the app-private model path and LiteRT cache directory before initialization.
- **`ai/model/ModelDownloadWorker.kt`**: WorkManager-backed model download worker with HTTPS-only request validation, Gallery-style `.gallerytmp` partial files, resume support through HTTP range requests, SHA-256 verification, and progress data for future UI wiring.
- **`ai/model/ModelDownloadManager.kt`**: Unique-work enqueue/cancel boundary for model downloads, constrained to connected-network execution.
- **`ai/model/ModelDownloadUiState.kt`**: Maps WorkManager progress/output data into stable UI states for progress, transfer rate, ETA, verified output path, and safe errors.
- **`ui/main/SettingsScreen.kt`**: Hosts the FunctionGemma MobileActions model section. The default card is production-shaped and status-first; manual URL/SHA-256 entry remains available only in the advanced section.
- **`ServiceLocator.getGemmaModelManager(context)`**: Exposes one app-wide Android model manager instance with lazy LiteRT engine creation, keeping model lifecycle separate from Room and notification ingestion singletons.

---

## 3. Premium App Flow & UI Safety Surface

### Top-Level App Shell

The app shell follows a four-tab production flow:

```text
Home | Voice | Inbox | Settings
```

`Home` is the default return point. `Voice` is the primary action surface. `Inbox` is the local encrypted message/action workspace. `Settings` keeps permissions, voice mode, and model installation controls off the main task path.

### Home Dashboard

The dashboard answers three questions before the user acts:

```text
Is capture ready?
Is there local WhatsApp context?
Is FunctionGemma installed?
```

It keeps one prominent CTA: speak to GemmaControl. Secondary routes open the inbox or settings only when the user needs context or setup work.

### Voice Confirmation Sheets

Voice actions are never sent directly from model text. FunctionGemma output becomes a Kotlin `ToolProposal`, then the UI shows a sheet with:

- proposal summary
- tool name
- safety label
- bounded argument preview
- local execution boundary text
- explicit confirm/cancel actions

Success and failure states return the user to the voice surface without hiding the safety result.

### Actionable Inbox UI

The inbox separates passive stored messages from actionable local items. Follow-ups, priority marks, reminders, and prepared drafts are visible as local app state before any Android intent or `RemoteInput` execution is attempted.

### FunctionGemma Model Card

Model installation is explicit and auditable:

- the official MobileActions model URL is derived from the catalog entry
- the app still requires a user-supplied SHA-256 before download
- downloads run through WorkManager with HTTPS-only validation, partial-file resume, and checksum verification
- `.litertlm` model binaries are app-private runtime artifacts and must never be committed
- physical model runtime quality and latency remain unverified until device validation

---

## 4. Strict Safety Routing Flow

To prevent untrusted actions or silent auto-sends, all execution triggers obey this Kotlin check:

```text
Proposed Tool Call (from FunctionGemma)
            ↓
  GemmaModelManager (Must be Ready)
            ↓
    ToolCallParser (Rejects malformed/unknown params)
            ↓
   Typed ToolProposal (Validates type mappings)
            ↓
    ToolSafetyRouter (Allow / Confirm / Reject)
            ↓
  Is Confirmation Needed? (Tool 10, 11, 12, 13, 14, 15, 16)
   ├── YES:
   │    Show Modal Compose Sheet
   │    Wait for User Click
   │    Retrieve active Notification via getActiveNotifications(keys)
   │    Is Notification still active?
   │       ├── YES: Execute RemoteInput Reply or Intent Share
   │       └── NO: Fail safely → Offer open_whatsapp_share_draft fallback
   └── NO:
        Retrieve SQLite records / Update locally inside Room
```

---

## 5. Operational & OS Limitations

- **Strict Sandbox Bounds**: The assistant cannot access WhatsApp's private sandbox (`/data/data/com.whatsapp/`). It only accesses text captured from active, visible system notifications.
- **Scoped Network Permission**: The manifest declares `android.permission.INTERNET` only for explicit model binary downloads. Runtime WhatsApp capture, prompts, tool proposals, local database content, and confirmed replies remain on device.
- **Scoped Notification Permission**: The manifest declares `android.permission.POST_NOTIFICATIONS` only for user-confirmed local reminders on Android 13+.
- **Dynamic Action Caching**: Executable `RemoteInput` and `PendingIntent` objects are never written to Room. The app caches only the system metadata `notification_key`. The listener retrieves active references on-the-fly at reply confirmation time, avoiding system memory reference leaks or dead intent executions.

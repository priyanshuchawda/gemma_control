# On-Device WhatsApp AI Assistant: High-Level Plan

## 1. Product Definition
An English-only, private, on-device Android productivity agent for WhatsApp notification workflows. The app captures new WhatsApp notifications after user permission, organises them into a local actionable inbox, lets FunctionGemma propose approved tool calls, enables reminders and follow-ups, and supports safe user-confirmed WhatsApp replies.

---

## 2. Technical Stack & Stack Restrictions (Android 16 / API 36)

### Stack Elements
- **Client Application**: Native Kotlin Android App targeting **Android 16 (API Level 36)**.
- **UI Framework**: Jetpack Compose + Material 3 (with mandatory **Android 16 edge-to-edge enforcement** and **predictive back navigation** support).
- **Local Storage Subsystem**: Room SQLite Database.
- **Offline Inference Engine**: LiteRT-LM Android SDK.
- **Local Routing Model**: FunctionGemma 270M (compiled as `.litertlm` file).
- **Physical Test Device**:
  - **Manufacturer/Model**: Xiaomi Redmi 13 5G (2406ERN9CI)
  - **Android Version**: Android 16
  - **API Level**: 36
  - **ABI (CPU Architecture)**: arm64-v8a
  - **Total Physical RAM**: approximately 6 GB (5,531,208 kB)
  - **Connection Status**: Connected via USB (Serial: `1431df87`)
  - **Application Package Dependency**: WhatsApp (`com.whatsapp` or WhatsApp Business `com.whatsapp.w4b`). *Constraint: Until WhatsApp or WhatsApp Business is visible on the phone/profile being tested, real notification capture or direct reply checks cannot run.*

### Strict Scope Constraints
- **No PC Companion Services**: No desktop apps, Streamlit servers, or Python dashboards.
- **No Multilingual Support**: Strictly English-only commands, schemas, UI texts, and datasets. No Hindi or Hinglish.
- **No Headless Automation**: No AccessibilityService UI scripts, Baileys, whatsapp-web.js, or unofficial automation engines.
- **No Silent Auto-Replies**: Auto-responses are structurally blocked; every outgoing transaction must be triggered by physical user interaction.
- **No Cloud Footprint**: Zero remote database engines, API endpoints, or Firebase modules.
- **No Semantic Vector Search**: EmbeddingGemma integration is deferred to V2.
- **No Heavy General Reasoners**: Gemma 4 integration is deferred to V3.

---

## 3. Core Safety & Execution Boundaries

### Non-Negotiable Safety Rules
1. **Explicit Manual Confirmation**: No outgoing message is sent from the phone without a physical user click on the confirmation screen of the UI.
2. **LiteRT Manual Execution**: `automaticToolCalling` inside LiteRT-LM's `ConversationConfig` is explicitly disabled (`false`). Model tool calls are treated as proposals. Kotlin safety logic validates schemas and executes tasks.
3. **Parameter Type Validation**: No hallucinated or unsupported tools can be executed. All parameters are parsed and strictly typed before routing.
4. **Ambiguity Resolution**: If a query targets a contact name matching multiple local conversations, execution halts, and a Compose choice sheet is shown.
5. **No Direct RemoteInput Caching**: The app does not save system `PendingIntent` or `RemoteInput` bindings to SQLite. It stores only `notification_key` metadata. During replies, it retrieves active notifications in real-time. If expired, it triggers a draft fallback instead of failing silently.
6. **Cascade Message Purging**: Deleting message events instantly wipes all associated follow-ups, reminders, and draft entries from local SQLite.

---

## 4. Database Schema Architectural Blocks

The local SQLite Room Database utilizes seven dedicated tables to store structured states:
1. **`ConversationEntity`**: Tracks direct vs group chats, display names, and E.164 phone mappings.
2. **`MessageEventEntity`**: Caches message previews, timestamps, notification keys, and deduplication hashes. Message bodies are encrypted at rest using AES-GCM backed by Android Keystore.
3. **`ActiveNotificationReferenceEntity`**: Caches the live system `notification_key` to query state at reply confirmation time.
4. **`FollowUpEntity`**: Models actionable items, priority flags, and task completions.
5. **`ReminderEntity`**: Manages scheduled reminders powered by standard system `WorkManager`.
6. **`DraftReplyEntity`**: Holds drafted text responses.
7. **`AssistantActionEntity`**: Serves as a local audit logger, storing tool requests and safety outcomes, omitting plaintext body payloads.

---

## 5. Development Pipeline

- **Phase 0A**: Verified device documentation (Xiaomi Redmi 13 5G, Android 16, API Level 36, arm64-v8a, 6 GB RAM, ADB Serial `1431df87`).
- **Phase 0B**: Diagnostics & physical model latency / thermal benchmarks (using AI Edge Gallery or equivalent tool).
- **Phase 1**: Minimal Android 16 Compose sandbox & `NotificationListenerService` proof-of-concept displaying raw parsed notifications.
- **Phase 2**: Room database models, deduplication parsing, and privacy setup.
- **Phase 3**: Non-AI tool executions (Intents, RemoteInput live lookup, WorkManager reminders).
- **Phase 4**: LiteRT-LM FunctionGemma on-device routing implementation (`automaticToolCalling = false`).
- **Phase 5**: Benchmark safety suites against 100+ English queries.
- **Phase 6**: Future fine-tuning dataset generation (English-only).
- **Phase 7**: Future semantic search integrations.

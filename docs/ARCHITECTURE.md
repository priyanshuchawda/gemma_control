# Technical Architecture Design

This document details the on-device data flows, module partitions, tool execution flows, safety gates, and sandbox limitations for the WhatsApp AI Assistant targeting **Android 16 (API Level 36)**.

---

## 1. On-Device Data Flow

Every transaction, local Room SQLite update, and model inference occurs offline on the physical **Xiaomi Redmi 13 5G (Android 16)** handset.

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
                         (automaticToolCalling = false)
                                   ↓
                        [ Proposes JSON Tool ]
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
- Manages the **Send Confirmation UI Sheet**, which serves as the physical boundary block between drafted actions and external intent triggers.

### B. Ingestion Module (`com.example.gemmacontrol.notifications`)
- **`WhatsAppNotificationListener`**: Extends `NotificationListenerService`. BINDs to Android's system pipeline.
- **`WhatsAppNotificationParser`**: Extracts unread history from `Notification.MessagingStyle` structures, isolates group conversation headers from message sender display names, and constructs a deduplication hash (`dedupe_hash`) to avoid duplicate row creation on repost events.
- **WhatsApp Package Constraint**: The service parses alert payloads belonging to `"com.whatsapp"` or `"com.whatsapp.w4b"`. Since WhatsApp is currently not detected on the device profile checked through ADB, physical notification intercepts require installing and activating WhatsApp on the handset.

### C. Storage Module (`com.example.gemmacontrol.data`)
- **Entities**: Room-backed definitions for `ConversationEntity`, `MessageEventEntity`, `ActiveNotificationReferenceEntity`, `FollowUpEntity`, `ReminderEntity`, `DraftReplyEntity`, and `AssistantActionEntity`.
- **Database Wrapper**: Configured with a cascade delete logic structure (e.g. wiping a message event automatically deletes its follow-ups and reminders).
- **Keystore Cipher**: A local cipher that generates an AES-GCM 256-bit key protected by Android Keystore. Plaintext is decrypted purely in memory when presenting screen lists.

### D. Model Routing Module (`com.example.gemmacontrol.ai`)
- **`FunctionGemmaEngine`**: Orchestrates offline inputs through Google's **LiteRT-LM Android SDK**.
- **Inference Configuration**: Manually binds the 16-tool registry prompts. Crucially sets `automaticToolCalling = false` inside `ConversationConfig` to block model auto-executions.
- **`ToolCallParser`**: Typed JSON parser that converts proposed model tool choices into local structured objects.

---

## 3. Strict Safety Routing Flow

To prevent untrusted actions or silent auto-sends, all execution triggers obey this Kotlin check:

```text
Proposed Tool Call (from FunctionGemma)
            ↓
    JSON Schema Check (Rejects malformed params)
            ↓
   Resolve Parameters (Validates type mappings)
            ↓
  Is Confirmation Needed? (Tool 10, 11, 12, 13, 16)
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

## 4. Operational & OS Limitations

- **Strict Sandbox Bounds**: The assistant cannot access WhatsApp's private sandbox (`/data/data/com.whatsapp/`). It only accesses text captured from active, visible system notifications.
- **No Internet Access**: The application's `AndroidManifest.xml` does not declare `android.permission.INTERNET`, verifying that data is locked within local memory and Room SQLite storage.
- **Dynamic Action Caching**: Executable `RemoteInput` and `PendingIntent` objects are never written to Room. The app caches only the system metadata `notification_key`. The listener retrieves active references on-the-fly at reply confirmation time, avoiding system memory reference leaks or dead intent executions.

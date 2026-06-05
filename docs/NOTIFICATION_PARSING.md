# Notification Ingestion & Parsing Subsystem

Design reference for the `WhatsApp Notification Listener` and `WhatsApp Notification Parser` components.

---

## 1. Ingestion Pipeline

```text
[ WhatsApp posts notification to Android system ]
              ↓
[ BIND_NOTIFICATION_LISTENER_SERVICE (granted) ]
              ↓
[ WhatsAppNotificationListener.onNotificationPosted ]
              ↓
[ Package allowlist check — com.whatsapp / com.whatsapp.w4b ]
              ↓
[ Determine POSTED vs UPDATED via activeKeys set ]
              ↓
[ WhatsAppNotificationParser.parse() ]
              ↓  (MessagingStyle or Extras fallback)
[ Classify content kind: text/media/call/system/hidden/unknown ]
              ↓
[ Deterministic volatile dedupeCandidate hash generated ]
              ↓
[ Prepend to in-memory MutableStateFlow (cap: 100) ]
              ↓
[ Compose UI collects StateFlow and re-renders ]
```

On swipe/dismiss:
```text
[ onNotificationRemoved called ]
              ↓
[ Key removed from activeKeys set ]
              ↓
[ REMOVED event prepended; prior events marked isCurrentlyActive = false ]
```

---

## 2. Event Types

| Type | Trigger |
| :--- | :--- |
| `POSTED` | First time a notification key appears in `onNotificationPosted` |
| `UPDATED` | Subsequent `onNotificationPosted` call for a key already in `activeKeys` |
| `REMOVED` | `onNotificationRemoved` for an active key |

> **Physical validation**: POSTED → UPDATED lifecycle was observed on the Redmi 13 5G (same key, message count incremented 1→4 over controlled test).

---

## 3. Parser Architecture

### A. MessagingStyle Parsing (Primary Path)
WhatsApp bundles unread message histories in `Notification.MessagingStyle`. The parser iterates both `messages` and `historicMessages` arrays:

```kotlin
val style = Notification.Builder.recoverBuilder(context, notification).style
if (style is Notification.MessagingStyle) {
    val isGroup = style.isGroupConversation
    val messages = style.messages          // current unread stack
    val historic = style.historicMessages  // older dismissed messages
    // ...
}
```

### B. Extras Fallback Path (Summary / Legacy)
When `MessagingStyle` is unavailable (e.g. group summaries, older WhatsApp channels), the parser falls back to `EXTRA_TITLE` and `EXTRA_TEXT`:

```kotlin
val title = extras.getString(Notification.EXTRA_TITLE)
val text  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
```

**Limitation**: the fallback path does not expose `isGroupConversation`. Events parsed this way receive `ConversationType.UNKNOWN`.

### C. Conversation Classification Logic

| Condition | Classification |
| :--- | :--- |
| `MessagingStyle` && `isGroupConversation == true` | `GROUP` |
| `MessagingStyle` && title == sender name (or title is blank) | `DIRECT` |
| `MessagingStyle` && ambiguous title | `UNKNOWN` |
| Extras fallback | `UNKNOWN` |
| No content at all | `UNKNOWN`; content kind becomes `HIDDEN` with `isContentUnavailable = true` |

> **Physical validation**: `DIRECT` and `GROUP` classification were confirmed on-device in controlled chat tests on the Xiaomi Redmi 13 5G.

### D. Content Classification Logic

Every parsed message preview carries a non-sensitive `WhatsAppContentKind`:

| Kind | Meaning |
| :--- | :--- |
| `TEXT` | Ordinary nonblank message text exposed by the notification |
| `PHOTO`, `VIDEO`, `STICKER`, `AUDIO`, `DOCUMENT` | WhatsApp media placeholder text only; the app does not have media bytes or media contents |
| `MISSED_CALL` | Missed WhatsApp call placeholder |
| `SYSTEM` | WhatsApp maintenance/status notification such as "Checking for new messages" |
| `HIDDEN` | Blank, redacted, or explicitly hidden notification content |
| `UNKNOWN` | Unsupported placeholder text that should not be treated as ordinary message body text |

The local read/TTS/prompt paths use this kind directly. For example, a `PHOTO` notification is described as a photo attachment with contents not inspected; the assistant must not claim what the image contains. The future media-analysis boundary is documented in [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md).

---

## 4. Keyed Deduplication & Room Persistence Policy

When WhatsApp reposts or refreshes a notification card with the same unread stack contents, the system prevents duplicate message rows in the secure database using a privacy-safe, keyed token:
1. The parser generates a deterministic SHA-256 `dedupeCandidate` string in memory for volatile logging and lifecycle tracking.
2. The `StoredInboxRepository` translates this candidate into a secure database `dedupeToken` using `AndroidKeystoreHmacDedupeTokenGenerator`, which hashes the candidate using a hardware-locked Keystore HMAC key.
3. The `MessageEventEntity` stores this `dedupeToken` inside a unique database column backed by a **UNIQUE SQLite index**.
4. The `MessageEventEntity` also stores the non-sensitive `contentKind` enum beside the encrypted sender/text fields. Existing v4 databases migrate to schema v5 with `contentKind = TEXT` for legacy rows.
5. The `MessageEventDao.insert()` operation utilizes `OnConflictStrategy.IGNORE` which silently ignores insertions with duplicate tokens. This ensures that repeating notifications do not leak plaintext-derived fingerprints or create duplicate historical rows on disk.

### Dual-Notification Normalization Policy
During controlled tests on the Redmi 13 5G handset, we observed that each incoming WhatsApp message triggered two separate notifications:
1. A canonical `MESSAGING_STYLE` individual chat notification.
2. A paired `EXTRAS_FALLBACK` rollup summary notification.

To normalize this pattern and prevent duplicate inbox entries:
- **Debug volatile feed** continues to show both raw captures to maintain visibility.
- **Persistent Secure Inbox** implements a strict canonicalization policy inside `NotificationPersistenceCoordinator`:
  - `MESSAGING_STYLE` events are treated as primary canonical sources and persisted directly.
  - Canonical `SYSTEM` content is filtered before storage so maintenance notifications do not pollute summaries.
  - Canonical `HIDDEN` content can persist as metadata without message text so read/summarize output can truthfully say content is hidden.
  - `EXTRAS_FALLBACK` events are **never** persisted into the canonical Room database. They remain strictly volatile-only in the debug feed. This minimal policy prevents duplicate stored rows independently of notification-key relationships. Fallback-only persistence is deferred until a separately validated correlation/review-inbox design exists.

> [!WARNING]
> **Scope Warning**: This dual-notification pattern was observed during controlled physical tests on the Xiaomi Redmi 13 5G with the specific installed WhatsApp version. It must **not** be generalized as guaranteed baseline behavior across every Android version or WhatsApp build.

---

## 5. Privacy Constraints

- **No plaintext message body text, sender names, or group names are logged to Logcat or written to disk.**
- Logcat output is limited to: package name, key suffix (last 8 chars), parse source label, message count.
- Encrypted data at rest: `messageText`, `senderName`, and `conversationTitle` are encrypted using Android Keystore-backed AES-GCM before writing to the database, ensuring absolute privacy at rest.
- Media placeholders are treated as metadata only. The notification parser does not receive image, video, sticker, audio, or document bytes, so downstream AI/TTS output must not invent media contents. Actual media analysis requires a separate user-selected file or scoped URI flow.

---

## 6. Execution Constraints

- **No AI inference inside listener callbacks.** LiteRT-LM / FunctionGemma calls are strictly deferred to user-triggered flows.
- **Room writes require explicit storage consent.** The volatile `MutableStateFlow` remains available for the live capture feed even when storage is off.
- Listener state mutations happen through a coroutine-backed flow update path and are capped to the latest 100 events.

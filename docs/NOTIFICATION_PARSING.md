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
[ SHA-256 dedupeCandidate generated ]
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
| No content at all | `UNKNOWN` + `isContentUnavailable = true` |

> **Physical validation**: `DIRECT` classification was confirmed on-device for a controlled direct-chat test. `GROUP` classification has **not yet** been confirmed — the group test notification arrived via the fallback path and was classified `UNKNOWN`.

---

## 4. Deduplication Candidate & Room Persistence Policy

In Phase 2A, the deduplication candidate is actively used to prevent duplicate message rows in the secure database. When WhatsApp reposts or refreshes a notification card with the same unread stack contents:
1. The parser generates the deterministic SHA-256 `dedupeCandidate` hash.
2. The `MessageEventEntity` stores this hash in `dedupeHash` which is backed by a **UNIQUE SQLite index**.
3. The `MessageEventDao.insert()` operation utilizes `OnConflictStrategy.IGNORE` which silently ignores insertions with duplicate hashes. This ensures that repeating or updated notifications do not create duplicate historical rows on disk.

### Dual-Notification Normalization Policy
During controlled tests on the Redmi 13 5G handset, we observed that each incoming WhatsApp message triggered two separate notifications:
1. A canonical `MESSAGING_STYLE` individual chat notification.
2. A paired `EXTRAS_FALLBACK` rollup summary notification.

To normalize this pattern and prevent two duplicate inbox entries:
- **Debug volatile feed** continues to show both raw captures to maintain visibility.
- **Persistent Secure Inbox** implements a strict canonicalization policy inside `NotificationPersistenceCoordinator`:
  - `MESSAGING_STYLE` events are treated as primary canonical sources and persisted directly.
  - An `EXTRAS_FALLBACK` summary rollup is discarded if a corresponding canonical `MESSAGING_STYLE` event has already been handled for that active notification key.
  - If a fallback-only notification arrives with no matching canonical event, it is saved conservatively in an unclassified state (`ConversationType.UNKNOWN`) without silent deletion.

> [!WARNING]
> **Scope Warning**: This dual-notification pattern was observed during controlled physical tests on the Xiaomi Redmi 13 5G with the specific installed WhatsApp version. It must **not** be generalized as guaranteed baseline behavior across every Android version or WhatsApp build.

---

## 5. Privacy Constraints

- **No message body text, sender names, or group names are logged to Logcat.**
- Logcat output is limited to: package name, key suffix (last 8 chars), parse source label, message count.
- The `ParsedMessagePreview.senderName` and `messageText` fields are held **in volatile in-memory only** and never written to disk or logged.

---

## 6. Execution Constraints

- **No AI inference inside listener callbacks.** LiteRT-LM / FunctionGemma calls are strictly deferred to user-triggered flows in Phase 4.
- **No Room writes in Phase 1.** The `MutableStateFlow` is the sole storage mechanism.
- All state mutations happen on the calling thread (Android system notification binder thread). The flow update is thread-safe via `update {}`.

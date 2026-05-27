# Notification Ingestion & Parsing Subsystem

This document outlines the software design, classes, deduplication logic, and OS lifecycle mappings for the `WhatsAppNotificationParser` component of the application.

---

## 1. System Ingestion Pipeline

To capture incoming text alerts, the application BINDs a native `NotificationListenerService` to Android's notification pipeline.

```text
       [ System Notification Post ]
                    ↓
     [ BIND_NOTIFICATION_LISTENER ]
                    ↓
[ WhatsAppNotificationListener.onNotificationPosted ]
                    ↓
     [ Filter com.whatsapp package ]
                    ↓
      [ WhatsAppNotificationParser ]
                    ↓ (Parse MessagingStyle)
       [ Deduplication Hash Check ]
                    ↓
        [ Commit to Room SQLite ]
```

---

## 2. Ingestion Lifecycles & Events

The system maps three primary lifecycle events:
1. **`onNotificationPosted`**: Fired when a new WhatsApp alert is generated or an existing notification is updated (e.g. a new message arrives in an existing group chat, causing a repost).
2. **`onNotificationRemoved`**: Fired when the user swipes away, clears, or clicks the notification banner. The parser updates the database, setting `is_active_last_known = false` on `ActiveNotificationReferenceEntity` and recording the `removed_at` timestamp.
3. **Reboot Persistence**: System services are decoupled. When the physical phone restarts, the listener automatically binds again once unlocked. The UI checks active system notifications on launch via `getActiveNotifications()` to reconcile states.

---

## 3. WhatsAppNotificationParser Architecture

The parser class decodes raw notification data structures:

### A. MessagingStyle Parsing
Standard WhatsApp notifications package unread histories using `Notification.MessagingStyle`. Rather than scraping the topmost display text, the parser iterates through both current and historic message arrays (when the platform exposes them) to extract each specific message block into a structured `ParsedWhatsAppNotificationEvent`:

```kotlin
object WhatsAppNotificationParser {
    fun parse(
        context: Context,
        sbn: StatusBarNotification,
        eventType: NotificationEventType,
        isCurrentlyActive: Boolean
    ): ParsedWhatsAppNotificationEvent? {
        // ... (See actual implementation in project files for exact code)
    }
}
```

### B. Separating Senders from Group Conversations
- **Group Notifications**: Exposes `ConversationType.GROUP`. `conversationTitle` matches the group name, and individual message elements expose the unique author's name inside `sender` parameters.
- **Direct Notifications**: Exposes `ConversationType.DIRECT` only when classification is reliable (e.g. `conversationTitle` is empty or identical to the sender name).
- **Fallback / Ambiguous Cases**: Exposes `ConversationType.UNKNOWN` to prevent misleading classification.

### C. Content Availability Handling
If notification body strings are null, blank, or unavailable, the parser flags `isContentUnavailable = true` and exposes `NotificationParseSource.UNAVAILABLE` rather than fabricating mock sender/text data.

---

## 4. Deduplication & Repost Logic

WhatsApp commonly updates notification panels by reposting active banners as new messages arrive. Scraping the text values raw leads to duplicate rows.

### Deduplication Candidate (`dedupeCandidate`)
To prevent duplicates, the parser creates a deterministic SHA-256 hash containing all primary identity elements:
```text
dedupeCandidate = SHA-256(packageName + notificationKey + timestamp + conversationTitle + senderName + messageText)
```

> [!WARNING]
> Final deduplication candidate correctness is considered **unverified** until real stacked/reposted WhatsApp notifications are observed and validated directly on the physical phone during live validation testing.


---

## 5. System Execution Constraints

- **Strict Main Thread Protection**: Message parsing, deduplication matching, and database writes run on a background thread (`Dispatchers.IO`) inside a `SupervisorJob` context.
- **Model Decoupling**: The service does *not* execute LiteRT-LM prompts or invoke FunctionGemma within listener callbacks. This guarantees minimal CPU and memory footprints, preventing background thread lockups or thermal throttling during rapid notification intervals. AI operations are strictly passive, executing only when triggered by user input.

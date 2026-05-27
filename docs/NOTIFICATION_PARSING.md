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
Standard WhatsApp notifications package unread histories using `Notification.MessagingStyle`. Rather than scraping the topmost display text, the parser iterates through the message array to extract each specific message block:

```kotlin
object WhatsAppNotificationParser {
    fun parse(sbn: StatusBarNotification): ParsedNotificationResult? {
        val notification = sbn.notification
        val extras = notification.extras
        
        // Retrieve MessagingStyle bundle
        val messagingStyle = Notification.Builder.recoverBuilder(context, notification)
            .style as? Notification.MessagingStyle ?: return parseFallback(sbn)

        val conversationTitle = messagingStyle.conversationTitle?.toString() ?: ""
        val isGroup = messagingStyle.isGroupConversation
        val messagesList = mutableListOf<ParsedMessageItem>()

        for (message in messagingStyle.messages) {
            val text = message.text?.toString() ?: continue
            val sender = message.sender?.name?.toString() ?: ""
            val timestamp = message.timestamp
            messagesList.add(ParsedMessageItem(sender, text, timestamp))
        }

        return ParsedNotificationResult(
            notificationKey = sbn.key,
            conversationTitle = conversationTitle,
            isGroup = isGroup,
            messages = messagesList,
            hasReplyAction = sbn.notification.actions?.any { it.remoteInputs != null } == true
        )
    }

    private fun parseFallback(sbn: StatusBarNotification): ParsedNotificationResult {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
        val timestamp = sbn.postTime

        return ParsedNotificationResult(
            notificationKey = sbn.key,
            conversationTitle = title,
            isGroup = isGroup,
            messages = listOf(ParsedMessageItem(title, text, timestamp)),
            hasReplyAction = sbn.notification.actions?.any { it.remoteInputs != null } == true
        )
    }
}
```

### B. Separating Senders from Group Conversations
- **Group Notifications**: `conversationTitle` matches the group name (e.g. `"Project Team"`), and individual message elements expose the unique author's name inside `sender` parameters (e.g. `"Rahul"`, `"Amit"`).
- **Direct Notifications**: `conversationTitle` and `sender` are identical, representing the individual contact.

### C. Redaction Handling (Android 15)
Target API Level 35 enforces sensitive notification redaction for background services. If notification body strings are null, blank, or flag exceptions, the parser flags `isRedacted = true` and saves empty message bodies rather than fabricating mock data.

---

## 4. Deduplication & Repost Logic

WhatsApp commonly updates notification panels by reposting active banners as new messages arrive. Scraping the text values raw leads to duplicate rows.

### Deduplication Hash (`dedupe_hash`)
To prevent duplicates, the parser creates a deterministic SHA-256 deduplication hash for every parsed sub-message:
```text
dedupe_hash = SHA-256(conversationTitle + senderName + messageText + postedAtTimestamp)
```
Before committing to the `message_events` Room table, the repository checks the database for matching deduplication hashes. If a hash already exists, the row insertion is ignored, avoiding duplicate inbox noise.

---

## 5. System Execution Constraints

- **Strict Main Thread Protection**: Message parsing, deduplication matching, and database writes run on a background thread (`Dispatchers.IO`) inside a `SupervisorJob` context.
- **Model Decoupling**: The service does *not* execute LiteRT-LM prompts or invoke FunctionGemma within listener callbacks. This guarantees minimal CPU and memory footprints, preventing background thread lockups or thermal throttling during rapid notification intervals. AI operations are strictly passive, executing only when triggered by user input.

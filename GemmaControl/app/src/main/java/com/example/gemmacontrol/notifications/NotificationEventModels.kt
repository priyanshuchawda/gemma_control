package com.example.gemmacontrol.notifications

enum class NotificationEventType {
    POSTED,
    UPDATED,
    REMOVED
}

enum class NotificationParseSource {
    MESSAGING_STYLE,
    EXTRAS_FALLBACK,
    UNAVAILABLE
}

enum class ConversationType {
    DIRECT,
    GROUP,
    UNKNOWN
}

enum class WhatsAppContentKind {
    TEXT,
    PHOTO,
    VIDEO,
    STICKER,
    AUDIO,
    DOCUMENT,
    MISSED_CALL,
    SYSTEM,
    HIDDEN,
    UNKNOWN
}

data class ParsedMessagePreview(
    val senderName: String?,
    val messageText: String?,
    val timestamp: Long?,
    val contentKind: WhatsAppContentKind = WhatsAppContentKind.TEXT
)

data class ParsedWhatsAppNotificationEvent(
    val eventType: NotificationEventType,
    val notificationKey: String,
    val packageName: String,
    val observedAt: Long,
    val notificationPostedAt: Long?,
    val conversationTitle: String?,
    val conversationType: ConversationType,
    val messages: List<ParsedMessagePreview>,
    val currentMessageCount: Int,
    val historicMessageCount: Int,
    val hasReplyActionAtCaptureTime: Boolean,
    val parseSource: NotificationParseSource,
    val isContentUnavailable: Boolean,
    val dedupeCandidate: String?,
    val isCurrentlyActive: Boolean
)

val ParsedWhatsAppNotificationEvent.latestContentKind: WhatsAppContentKind
    get() = messages.lastOrNull()?.contentKind
        ?: if (isContentUnavailable) WhatsAppContentKind.HIDDEN else WhatsAppContentKind.UNKNOWN

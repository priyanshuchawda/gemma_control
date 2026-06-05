package com.example.gemmacontrol.notifications

enum class NotificationSourceType {
    WHATSAPP,
    SMS,
    GMAIL,
    PHONE,
    CALENDAR,
    OTHER
}

enum class NotificationSourceImplementationStatus {
    ENABLED,
    PLANNED,
    UNSUPPORTED
}

enum class NotificationCommonAction {
    CAPTURE_NOTIFICATIONS,
    READ_CAPTURED,
    SUMMARIZE_CAPTURED,
    SEARCH_CAPTURED,
    ACTIVE_NOTIFICATION_REPLY,
    OPEN_EXTERNAL_DRAFT,
    SOURCE_SPECIFIC_WRITE
}

enum class NotificationContentState {
    TEXT,
    MEDIA_PLACEHOLDER,
    HIDDEN,
    SYSTEM,
    UNKNOWN
}

enum class NotificationQueryMode {
    READ,
    SUMMARIZE,
    SEARCH
}

data class NotificationSourceDescriptor(
    val type: NotificationSourceType,
    val sourceId: String,
    val displayName: String,
    val packageNames: Set<String>,
    val implementationStatus: NotificationSourceImplementationStatus,
    val enabledActions: Set<NotificationCommonAction>,
    val plannedActions: Set<NotificationCommonAction>
) {
    fun isEnabledFor(action: NotificationCommonAction): Boolean {
        return implementationStatus == NotificationSourceImplementationStatus.ENABLED &&
            enabledActions.contains(action)
    }
}

data class NotificationQueryScope(
    val mode: NotificationQueryMode,
    val sourceTypes: Set<NotificationSourceType>,
    val conversationName: String? = null,
    val query: String? = null,
    val sinceMinutes: Int? = null,
    val limit: Int = 10
)

data class ParsedNotificationMessage(
    val senderName: String?,
    val body: String?,
    val timestamp: Long?,
    val contentState: NotificationContentState
)

data class ParsedNotificationEvent(
    val source: NotificationSourceDescriptor,
    val eventType: NotificationEventType,
    val notificationKey: String,
    val packageName: String,
    val observedAt: Long,
    val notificationPostedAt: Long?,
    val conversationTitle: String?,
    val conversationType: ConversationType,
    val messages: List<ParsedNotificationMessage>,
    val hasReplyActionAtCaptureTime: Boolean,
    val parseSource: NotificationParseSource,
    val isContentUnavailable: Boolean,
    val isCurrentlyActive: Boolean
)

object NotificationSourceCatalog {
    private val readSummarizeSearch = setOf(
        NotificationCommonAction.READ_CAPTURED,
        NotificationCommonAction.SUMMARIZE_CAPTURED,
        NotificationCommonAction.SEARCH_CAPTURED
    )

    val whatsappPackageNames: Set<String> = setOf("com.whatsapp", "com.whatsapp.w4b")

    private val descriptors = listOf(
        NotificationSourceDescriptor(
            type = NotificationSourceType.WHATSAPP,
            sourceId = "whatsapp",
            displayName = "WhatsApp",
            packageNames = whatsappPackageNames,
            implementationStatus = NotificationSourceImplementationStatus.ENABLED,
            enabledActions = readSummarizeSearch + setOf(
                NotificationCommonAction.CAPTURE_NOTIFICATIONS,
                NotificationCommonAction.ACTIVE_NOTIFICATION_REPLY,
                NotificationCommonAction.OPEN_EXTERNAL_DRAFT,
                NotificationCommonAction.SOURCE_SPECIFIC_WRITE
            ),
            plannedActions = emptySet()
        ),
        plannedSource(
            type = NotificationSourceType.SMS,
            sourceId = "sms",
            displayName = "SMS",
            packageNames = setOf(
                "com.google.android.apps.messaging",
                "com.android.mms"
            ),
            plannedActions = readSummarizeSearch
        ),
        plannedSource(
            type = NotificationSourceType.GMAIL,
            sourceId = "gmail",
            displayName = "Gmail",
            packageNames = setOf("com.google.android.gm"),
            plannedActions = readSummarizeSearch
        ),
        plannedSource(
            type = NotificationSourceType.PHONE,
            sourceId = "phone",
            displayName = "Phone",
            packageNames = setOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.android.server.telecom"
            ),
            plannedActions = setOf(
                NotificationCommonAction.CAPTURE_NOTIFICATIONS,
                NotificationCommonAction.READ_CAPTURED,
                NotificationCommonAction.SUMMARIZE_CAPTURED
            )
        ),
        plannedSource(
            type = NotificationSourceType.CALENDAR,
            sourceId = "calendar",
            displayName = "Calendar",
            packageNames = setOf("com.google.android.calendar"),
            plannedActions = readSummarizeSearch
        )
    )

    fun classifyPackage(packageName: String?): NotificationSourceDescriptor {
        if (packageName.isNullOrBlank()) {
            return otherSource("")
        }
        return descriptors.firstOrNull { descriptor ->
            descriptor.packageNames.contains(packageName)
        } ?: otherSource(packageName)
    }

    fun isProductionCaptureEnabled(packageName: String?): Boolean {
        return classifyPackage(packageName).isEnabledFor(NotificationCommonAction.CAPTURE_NOTIFICATIONS)
    }

    fun canUseActiveNotificationReply(packageName: String?): Boolean {
        return classifyPackage(packageName).isEnabledFor(NotificationCommonAction.ACTIVE_NOTIFICATION_REPLY)
    }

    fun enabledSourcesFor(mode: NotificationQueryMode): Set<NotificationSourceType> {
        val requiredAction = when (mode) {
            NotificationQueryMode.READ -> NotificationCommonAction.READ_CAPTURED
            NotificationQueryMode.SUMMARIZE -> NotificationCommonAction.SUMMARIZE_CAPTURED
            NotificationQueryMode.SEARCH -> NotificationCommonAction.SEARCH_CAPTURED
        }
        return descriptors
            .filter { it.isEnabledFor(requiredAction) }
            .map { it.type }
            .toSet()
    }

    private fun plannedSource(
        type: NotificationSourceType,
        sourceId: String,
        displayName: String,
        packageNames: Set<String>,
        plannedActions: Set<NotificationCommonAction>
    ): NotificationSourceDescriptor {
        return NotificationSourceDescriptor(
            type = type,
            sourceId = sourceId,
            displayName = displayName,
            packageNames = packageNames,
            implementationStatus = NotificationSourceImplementationStatus.PLANNED,
            enabledActions = emptySet(),
            plannedActions = plannedActions
        )
    }

    private fun otherSource(packageName: String): NotificationSourceDescriptor {
        return NotificationSourceDescriptor(
            type = NotificationSourceType.OTHER,
            sourceId = if (packageName.isBlank()) "other" else "other:$packageName",
            displayName = "Other",
            packageNames = if (packageName.isBlank()) emptySet() else setOf(packageName),
            implementationStatus = NotificationSourceImplementationStatus.UNSUPPORTED,
            enabledActions = emptySet(),
            plannedActions = emptySet()
        )
    }
}

fun ParsedWhatsAppNotificationEvent.toGenericNotificationEvent(): ParsedNotificationEvent {
    val source = NotificationSourceCatalog.classifyPackage(packageName)
    return ParsedNotificationEvent(
        source = source,
        eventType = eventType,
        notificationKey = notificationKey,
        packageName = packageName,
        observedAt = observedAt,
        notificationPostedAt = notificationPostedAt,
        conversationTitle = conversationTitle,
        conversationType = conversationType,
        messages = messages.map { it.toGenericNotificationMessage() },
        hasReplyActionAtCaptureTime = hasReplyActionAtCaptureTime &&
            NotificationSourceCatalog.canUseActiveNotificationReply(packageName),
        parseSource = parseSource,
        isContentUnavailable = isContentUnavailable,
        isCurrentlyActive = isCurrentlyActive
    )
}

private fun ParsedMessagePreview.toGenericNotificationMessage(): ParsedNotificationMessage {
    return ParsedNotificationMessage(
        senderName = senderName,
        body = messageText,
        timestamp = timestamp,
        contentState = contentKind.toNotificationContentState()
    )
}

fun WhatsAppContentKind.toNotificationContentState(): NotificationContentState {
    return when (this) {
        WhatsAppContentKind.TEXT -> NotificationContentState.TEXT
        WhatsAppContentKind.PHOTO,
        WhatsAppContentKind.VIDEO,
        WhatsAppContentKind.STICKER,
        WhatsAppContentKind.AUDIO,
        WhatsAppContentKind.DOCUMENT -> NotificationContentState.MEDIA_PLACEHOLDER
        WhatsAppContentKind.HIDDEN -> NotificationContentState.HIDDEN
        WhatsAppContentKind.SYSTEM,
        WhatsAppContentKind.MISSED_CALL -> NotificationContentState.SYSTEM
        WhatsAppContentKind.UNKNOWN -> NotificationContentState.UNKNOWN
    }
}

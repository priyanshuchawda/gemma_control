package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import com.example.gemmacontrol.notifications.WhatsAppContentKind

internal object LiveNotificationReadMapper {
    fun toReadableMessages(
        events: List<ParsedWhatsAppNotificationEvent>
    ): List<StoredInboxRepository.DecryptedMessage> {
        return events
            .asSequence()
            .filter { event ->
                event.isCurrentlyActive &&
                    event.eventType != NotificationEventType.REMOVED &&
                    event.parseSource == NotificationParseSource.MESSAGING_STYLE
            }
            .distinctBy { it.notificationKey }
            .flatMap { event ->
                val previews = event.messages.ifEmpty {
                    if (event.isContentUnavailable) {
                        listOf(
                            ParsedMessagePreview(
                                senderName = event.conversationTitle,
                                messageText = null,
                                timestamp = event.notificationPostedAt ?: event.observedAt,
                                contentKind = WhatsAppContentKind.HIDDEN
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
                previews
                    .asSequence()
                    .filterNot { it.contentKind == WhatsAppContentKind.SYSTEM }
                    .mapIndexed { index, preview ->
                        preview.toReadableMessage(event, index)
                    }
            }
            .toList()
    }

    private fun ParsedMessagePreview.toReadableMessage(
        event: ParsedWhatsAppNotificationEvent,
        index: Int
    ): StoredInboxRepository.DecryptedMessage {
        val postedAt = timestamp ?: event.notificationPostedAt ?: event.observedAt
        val conversationTitle = event.conversationTitle
            ?.takeIf { it.isNotBlank() }
            ?: senderName?.takeIf { it.isNotBlank() }
            ?: DefaultConversationTitle

        return StoredInboxRepository.DecryptedMessage(
            id = "live-${event.notificationKey.hashCode()}-$postedAt-$index",
            conversationId = conversationTitle,
            senderName = senderName,
            decryptedText = messageText,
            postedAt = postedAt,
            notificationKey = event.notificationKey,
            sourcePackage = event.packageName,
            parseSource = event.parseSource,
            isContentUnavailable = event.isContentUnavailable || contentKind == WhatsAppContentKind.HIDDEN,
            createdAt = event.observedAt,
            priority = DefaultPriority,
            contentKind = contentKind
        )
    }

    private const val DefaultConversationTitle = "WhatsApp Chat"
    private const val DefaultPriority = "NORMAL"
}

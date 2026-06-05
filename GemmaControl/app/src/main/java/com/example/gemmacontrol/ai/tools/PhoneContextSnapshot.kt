package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.notifications.WhatsAppContentKind

data class PhoneContextSnapshot(
    val activeNotifications: List<PhoneContextActiveNotification>,
    val unreadChats: List<PhoneContextChatSummary>,
    val relevantMessages: List<GemmaMessageContext>
)

data class PhoneContextActiveNotification(
    val notificationKey: String,
    val conversationName: String,
    val unreadCount: Int,
    val latestMessageEventId: String?,
    val latestSnippet: String,
    val latestContentKind: WhatsAppContentKind,
    val replyAvailable: Boolean,
    val latestReplyTarget: Boolean
)

data class PhoneContextChatSummary(
    val conversationName: String,
    val storedCount: Int,
    val activeCount: Int,
    val latestAt: Long,
    val latestContentKind: WhatsAppContentKind,
    val latestSnippet: String,
    val highPriorityCount: Int,
    val replyAvailable: Boolean
)

class PhoneContextSnapshotBuilder {
    fun build(
        messages: List<GemmaMessageContext>,
        activeNotificationKeys: Set<String>,
        latestActiveNotificationKey: String? = null,
        maxChats: Int = DEFAULT_MAX_CHATS,
        maxMessages: Int = DEFAULT_MAX_MESSAGES,
        maxSnippetChars: Int = DEFAULT_MAX_SNIPPET_CHARS
    ): PhoneContextSnapshot {
        val safeMaxChats = maxChats.coerceIn(1, DEFAULT_MAX_CHATS)
        val safeMaxMessages = maxMessages.coerceIn(1, DEFAULT_MAX_MESSAGES)
        val safeMaxSnippetChars = maxSnippetChars.coerceAtLeast(1)
        val recentMessages = messages.sortedByDescending { it.postedAt }

        return PhoneContextSnapshot(
            activeNotifications = activeNotifications(
                messages = recentMessages,
                activeNotificationKeys = activeNotificationKeys,
                latestActiveNotificationKey = latestActiveNotificationKey,
                maxSnippetChars = safeMaxSnippetChars
            ),
            unreadChats = chatSummaries(
                messages = recentMessages,
                activeNotificationKeys = activeNotificationKeys,
                maxChats = safeMaxChats,
                maxSnippetChars = safeMaxSnippetChars
            ),
            relevantMessages = recentMessages
                .take(safeMaxMessages)
                .map { it.withBoundedBody(safeMaxSnippetChars) }
        )
    }

    private fun activeNotifications(
        messages: List<GemmaMessageContext>,
        activeNotificationKeys: Set<String>,
        latestActiveNotificationKey: String?,
        maxSnippetChars: Int
    ): List<PhoneContextActiveNotification> {
        val messagesByKey = messages.groupBy { it.notificationKey }
        return activeNotificationKeys
            .map { key ->
                val keyMessages = messagesByKey[key].orEmpty().sortedByDescending { it.postedAt }
                val latest = keyMessages.firstOrNull()
                PhoneContextActiveNotification(
                    notificationKey = key,
                    conversationName = latest?.conversationName ?: "Unknown WhatsApp chat",
                    unreadCount = keyMessages.size,
                    latestMessageEventId = latest?.messageEventId,
                    latestSnippet = latest?.body?.bounded(maxSnippetChars)
                        ?: "Active notification contents unavailable",
                    latestContentKind = latest?.contentKind ?: WhatsAppContentKind.UNKNOWN,
                    replyAvailable = true,
                    latestReplyTarget = key == latestActiveNotificationKey
                )
            }
            .sortedWith(
                compareByDescending<PhoneContextActiveNotification> { it.latestReplyTarget }
                    .thenByDescending { active ->
                        messagesByKey[active.notificationKey]
                            .orEmpty()
                            .maxOfOrNull { it.postedAt } ?: Long.MIN_VALUE
                    }
            )
    }

    private fun chatSummaries(
        messages: List<GemmaMessageContext>,
        activeNotificationKeys: Set<String>,
        maxChats: Int,
        maxSnippetChars: Int
    ): List<PhoneContextChatSummary> {
        return messages
            .groupBy { it.conversationName }
            .map { (conversationName, chatMessages) ->
                val latest = chatMessages.maxBy { it.postedAt }
                val activeCount = chatMessages.count { it.notificationKey in activeNotificationKeys }
                PhoneContextChatSummary(
                    conversationName = conversationName,
                    storedCount = chatMessages.size,
                    activeCount = activeCount,
                    latestAt = latest.postedAt,
                    latestContentKind = latest.contentKind,
                    latestSnippet = latest.body.bounded(maxSnippetChars),
                    highPriorityCount = chatMessages.count { it.priority.equals("HIGH", ignoreCase = true) },
                    replyAvailable = activeCount > 0
                )
            }
            .sortedByDescending { it.latestAt }
            .take(maxChats)
    }

    private fun GemmaMessageContext.withBoundedBody(maxSnippetChars: Int): GemmaMessageContext {
        return copy(body = body.bounded(maxSnippetChars))
    }

    private fun String.bounded(maxChars: Int): String {
        val singleLine = replace(Regex("\\s+"), " ").trim()
        return if (singleLine.length <= maxChars) singleLine else singleLine.take(maxChars) + "..."
    }

    private companion object {
        const val DEFAULT_MAX_CHATS = 6
        const val DEFAULT_MAX_MESSAGES = 8
        const val DEFAULT_MAX_SNIPPET_CHARS = 160
    }
}

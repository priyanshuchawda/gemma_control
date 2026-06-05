package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.notifications.WhatsAppContentKind
import com.example.gemmacontrol.notifications.promptBodyText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PromptClockSnapshot(
    val currentDateTimeIso: String,
    val dayOfWeek: String
)

data class GemmaMessageContext(
    val messageEventId: String,
    val notificationKey: String,
    val conversationName: String,
    val senderName: String,
    val body: String,
    val postedAt: Long,
    val priority: String = "NORMAL",
    val replyAvailable: Boolean = false,
    val contentKind: WhatsAppContentKind = WhatsAppContentKind.TEXT
) {
    companion object {
        fun fromDecryptedMessage(
            messageEventId: String,
            notificationKey: String,
            conversationName: String,
            senderName: String?,
            decryptedText: String?,
            postedAt: Long,
            priority: String = "NORMAL",
            replyAvailable: Boolean = false,
            contentKind: WhatsAppContentKind = WhatsAppContentKind.TEXT
        ): GemmaMessageContext {
            return GemmaMessageContext(
                messageEventId = messageEventId,
                notificationKey = notificationKey,
                conversationName = conversationName.ifBlank { "WhatsApp Chat" },
                senderName = senderName?.takeIf { it.isNotBlank() } ?: conversationName.ifBlank { "Unknown sender" },
                body = contentKind.promptBodyText(decryptedText),
                postedAt = postedAt,
                priority = priority.ifBlank { "NORMAL" },
                replyAvailable = replyAvailable,
                contentKind = contentKind
            )
        }
    }
}

class GemmaPromptBuilder(
    private val registry: WhatsAppToolRegistry = WhatsAppToolRegistry.default(),
    private val clock: () -> PromptClockSnapshot = { systemClockSnapshot() }
) {
    fun buildForUserCommand(
        userCommand: String,
        messages: List<GemmaMessageContext>,
        maxMessages: Int = DEFAULT_MAX_MESSAGES,
        maxBodyChars: Int = DEFAULT_MAX_BODY_CHARS,
        maxUserCommandChars: Int = DEFAULT_MAX_USER_COMMAND_CHARS
    ): String {
        val phoneContext = PhoneContextSnapshotBuilder().build(
            messages = messages,
            activeNotificationKeys = emptySet(),
            maxMessages = maxMessages,
            maxSnippetChars = maxBodyChars
        )
        return buildForUserCommand(
            userCommand = userCommand,
            phoneContext = phoneContext,
            maxBodyChars = maxBodyChars,
            maxUserCommandChars = maxUserCommandChars
        )
    }

    fun buildForUserCommand(
        userCommand: String,
        phoneContext: PhoneContextSnapshot,
        maxBodyChars: Int = DEFAULT_MAX_BODY_CHARS,
        maxUserCommandChars: Int = DEFAULT_MAX_USER_COMMAND_CHARS
    ): String {
        val clockSnapshot = clock()
        val systemPrompt = registry.buildSystemPrompt(
            currentDateTimeIso = clockSnapshot.currentDateTimeIso,
            dayOfWeek = clockSnapshot.dayOfWeek
        )

        return """
            $systemPrompt

            Compact phone context:
            Active WhatsApp notifications:
            ${activeNotificationBlock(phoneContext.activeNotifications, maxBodyChars)}

            Unread chat summaries:
            ${chatSummaryBlock(phoneContext.unreadChats, maxBodyChars)}

            Relevant stored messages:
            ${messageBlock(phoneContext.relevantMessages, maxBodyChars)}

            User command: ${userCommand.oneLine().truncate(maxUserCommandChars)}

            Call one native WhatsApp tool when a supported safe action applies. If no safe tool applies, respond briefly without claiming that any tool ran.
        """.trimIndent()
    }

    private fun activeNotificationBlock(
        activeNotifications: List<PhoneContextActiveNotification>,
        maxBodyChars: Int
    ): String {
        return if (activeNotifications.isEmpty()) {
            "- none"
        } else {
            activeNotifications.joinToString(separator = "\n") { active ->
                listOf(
                    "notification_key=${active.notificationKey}",
                    "conversation=${active.conversationName.oneLine()}",
                    "unread_count=${active.unreadCount}",
                    "latest_message_event_id=${active.latestMessageEventId ?: "unknown"}",
                    "latest_content_kind=${active.latestContentKind.name}",
                    "latest_snippet=${active.latestSnippet.oneLine().truncate(maxBodyChars)}",
                    "reply_available=${active.replyAvailable}",
                    "latest_reply_target=${active.latestReplyTarget}"
                ).joinToString(prefix = "- ", separator = "; ")
            }
        }
    }

    private fun chatSummaryBlock(
        unreadChats: List<PhoneContextChatSummary>,
        maxBodyChars: Int
    ): String {
        return if (unreadChats.isEmpty()) {
            "- none"
        } else {
            unreadChats.joinToString(separator = "\n") { chat ->
                listOf(
                    "conversation=${chat.conversationName.oneLine()}",
                    "stored_count=${chat.storedCount}",
                    "active_count=${chat.activeCount}",
                    "latest_at=${chat.latestAt}",
                    "latest_content_kind=${chat.latestContentKind.name}",
                    "latest_snippet=${chat.latestSnippet.oneLine().truncate(maxBodyChars)}",
                    "high_priority_count=${chat.highPriorityCount}",
                    "reply_available=${chat.replyAvailable}"
                ).joinToString(prefix = "- ", separator = "; ")
            }
        }
    }

    private fun messageBlock(
        messages: List<GemmaMessageContext>,
        maxBodyChars: Int
    ): String {
        return if (messages.isEmpty()) {
            "- none"
        } else {
            messages.joinToString(separator = "\n") { message ->
                listOf(
                    "message_event_id=${message.messageEventId}",
                    "notification_key=${message.notificationKey}",
                    "conversation=${message.conversationName.oneLine()}",
                    "sender=${message.senderName.oneLine()}",
                    "posted_at=${message.postedAt}",
                    "content_kind=${message.contentKind.name}",
                    "priority=${message.priority.oneLine()}",
                    "reply_available=${message.replyAvailable}",
                    "body=${message.body.oneLine().truncate(maxBodyChars)}"
                ).joinToString(prefix = "- ", separator = "; ")
            }
        }
    }

    private fun String.oneLine(): String = replace(Regex("\\s+"), " ").trim()

    private fun String.truncate(maxChars: Int): String {
        val safeMax = maxChars.coerceAtLeast(1)
        return if (length <= safeMax) this else take(safeMax) + "..."
    }

    private companion object {
        const val DEFAULT_MAX_MESSAGES = 5
        const val DEFAULT_MAX_BODY_CHARS = 500
        const val DEFAULT_MAX_USER_COMMAND_CHARS = 500

        fun systemClockSnapshot(): PromptClockSnapshot {
            val now = Date()
            return PromptClockSnapshot(
                currentDateTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(now),
                dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(now)
            )
        }
    }
}

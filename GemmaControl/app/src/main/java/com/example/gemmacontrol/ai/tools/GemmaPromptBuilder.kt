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

            Context:
            Active:
            ${activeNotificationBlock(phoneContext.activeNotifications, maxBodyChars)}

            Chats:
            ${chatSummaryBlock(phoneContext.unreadChats, maxBodyChars)}

            Messages:
            ${messageBlock(phoneContext.relevantMessages, maxBodyChars)}

            User: ${userCommand.oneLine().truncate(maxUserCommandChars)}

            Call one native tool if useful. Otherwise answer briefly.
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
                    "key=${active.notificationKey}",
                    "chat=${active.conversationName.oneLine()}",
                    "unread=${active.unreadCount}",
                    "msg=${active.latestMessageEventId ?: "unknown"}",
                    "kind=${active.latestContentKind.name}",
                    "text=${active.latestSnippet.oneLine().truncate(maxBodyChars)}",
                    "reply=${active.replyAvailable}"
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
                    "chat=${chat.conversationName.oneLine()}",
                    "stored=${chat.storedCount}",
                    "active=${chat.activeCount}",
                    "kind=${chat.latestContentKind.name}",
                    "text=${chat.latestSnippet.oneLine().truncate(maxBodyChars)}",
                    "important=${chat.highPriorityCount}",
                    "reply=${chat.replyAvailable}"
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
                    "msg=${message.messageEventId}",
                    "key=${message.notificationKey}",
                    "chat=${message.conversationName.oneLine()}",
                    "sender=${message.senderName.oneLine()}",
                    "at=${message.postedAt}",
                    "kind=${message.contentKind.name}",
                    "priority=${message.priority.oneLine()}",
                    "reply=${message.replyAvailable}",
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
        const val DEFAULT_MAX_MESSAGES = 3
        const val DEFAULT_MAX_BODY_CHARS = 160
        const val DEFAULT_MAX_USER_COMMAND_CHARS = 220

        fun systemClockSnapshot(): PromptClockSnapshot {
            val now = Date()
            return PromptClockSnapshot(
                currentDateTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(now),
                dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(now)
            )
        }
    }
}

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
            contentKind: WhatsAppContentKind = WhatsAppContentKind.TEXT
        ): GemmaMessageContext {
            return GemmaMessageContext(
                messageEventId = messageEventId,
                notificationKey = notificationKey,
                conversationName = conversationName.ifBlank { "WhatsApp Chat" },
                senderName = senderName?.takeIf { it.isNotBlank() } ?: conversationName.ifBlank { "Unknown sender" },
                body = contentKind.promptBodyText(decryptedText),
                postedAt = postedAt,
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
        val clockSnapshot = clock()
        val systemPrompt = registry.buildSystemPrompt(
            currentDateTimeIso = clockSnapshot.currentDateTimeIso,
            dayOfWeek = clockSnapshot.dayOfWeek
        )
        val boundedMessages = messages
            .sortedByDescending { it.postedAt }
            .take(maxMessages.coerceIn(1, DEFAULT_MAX_MESSAGES))

        val messageBlock = if (boundedMessages.isEmpty()) {
            "No selected WhatsApp messages are available."
        } else {
            boundedMessages.joinToString(separator = "\n") { message ->
                listOf(
                    "message_event_id=${message.messageEventId}",
                    "notification_key=${message.notificationKey}",
                    "conversation=${message.conversationName.oneLine()}",
                    "sender=${message.senderName.oneLine()}",
                    "posted_at=${message.postedAt}",
                    "content_kind=${message.contentKind.name}",
                    "body=${message.body.oneLine().truncate(maxBodyChars)}"
                ).joinToString(prefix = "- ", separator = "; ")
            }
        }

        return """
            $systemPrompt

            Selected local WhatsApp context:
            $messageBlock

            User command: ${userCommand.oneLine().truncate(maxUserCommandChars)}

            Call one native WhatsApp tool when a supported safe action applies. If no safe tool applies, respond briefly without claiming that any tool ran.
        """.trimIndent()
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

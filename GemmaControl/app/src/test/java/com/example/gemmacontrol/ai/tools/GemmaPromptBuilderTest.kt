package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaPromptBuilderTest {

    private val builder = GemmaPromptBuilder(
        registry = WhatsAppToolRegistry.default(),
        clock = { PromptClockSnapshot("2026-05-28T21:00:00", "Thursday") }
    )

    @Test
    fun buildsBoundedPromptWithRecentMessagesOnly() {
        val messages = (1..8).map { index ->
            GemmaMessageContext(
                messageEventId = "message-$index",
                notificationKey = "notification-key-$index",
                conversationName = "Contact $index",
                senderName = "Sender $index",
                body = "Body $index",
                postedAt = 1_000L + index
            )
        }

        val prompt = builder.buildForUserCommand(
            userCommand = "Reply to the latest message saying ok",
            messages = messages,
            maxMessages = 3
        )

        assertTrue(prompt.contains("Current date and time: 2026-05-28T21:00:00"))
        assertTrue(prompt.contains("User command: Reply to the latest message saying ok"))
        assertTrue(prompt.contains("message-8"))
        assertTrue(prompt.contains("message-7"))
        assertTrue(prompt.contains("message-6"))
        assertFalse(prompt.contains("message-5"))
    }

    @Test
    fun truncatesLongMessageBodiesWithoutRemovingIds() {
        val prompt = builder.buildForUserCommand(
            userCommand = "Read latest",
            messages = listOf(
                GemmaMessageContext(
                    messageEventId = "message-1",
                    notificationKey = "notification-key-1",
                    conversationName = "Mom",
                    senderName = "Mom",
                    body = "a".repeat(240),
                    postedAt = 1_000L
                )
            ),
            maxBodyChars = 40
        )

        assertTrue(prompt.contains("message-1"))
        assertTrue(prompt.contains("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa..."))
        assertFalse(prompt.contains("a".repeat(80)))
    }

    @Test
    fun mapsRepositoryMessagesToPromptContextWithoutBlankBody() {
        val context = GemmaMessageContext.fromDecryptedMessage(
            messageEventId = "message-1",
            notificationKey = "notification-key-1",
            conversationName = "Mom",
            senderName = null,
            decryptedText = null,
            postedAt = 1_000L
        )

        assertEquals("Mom", context.senderName)
        assertEquals("[content unavailable]", context.body)
    }
}

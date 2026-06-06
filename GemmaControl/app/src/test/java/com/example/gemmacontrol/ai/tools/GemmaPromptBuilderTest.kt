package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.notifications.WhatsAppContentKind
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

        assertTrue(prompt.contains("Time: 2026-05-28T21:00:00 (Thursday)"))
        assertTrue(prompt.contains("User: Reply to the latest message saying ok"))
        assertTrue(prompt.contains("message-8"))
        assertTrue(prompt.contains("message-7"))
        assertTrue(prompt.contains("message-6"))
        assertFalse(prompt.contains("message-5"))
        assertTrue(prompt.contains("Call one native tool"))
        assertFalse(prompt.contains("Return only one JSON object"))
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
    fun truncatesLongUserCommand() {
        val prompt = builder.buildForUserCommand(
            userCommand = "reply ".repeat(100),
            messages = emptyList(),
            maxUserCommandChars = 30
        )

        assertTrue(prompt.contains("User: reply reply reply reply reply ..."))
        assertFalse(prompt.contains("reply ".repeat(20)))
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

    @Test
    fun promptContextCarriesContentKindForMediaWithoutInventingContents() {
        val context = GemmaMessageContext.fromDecryptedMessage(
            messageEventId = "message-photo",
            notificationKey = "notification-key-photo",
            conversationName = "Mom",
            senderName = "Mom",
            decryptedText = "Photo",
            postedAt = 1_000L,
            contentKind = WhatsAppContentKind.PHOTO
        )

        val prompt = builder.buildForUserCommand(
            userCommand = "Show latest WhatsApp messages",
            messages = listOf(context)
        )

        assertEquals(WhatsAppContentKind.PHOTO, context.contentKind)
        assertEquals("Photo attachment (contents not inspected)", context.body)
        assertTrue(prompt.contains("kind=PHOTO"))
        assertTrue(prompt.contains("body=Photo attachment (contents not inspected)"))
    }

    @Test
    fun emptyContextPromptFitsMobileActionsInputWindow() {
        val prompt = builder.buildForUserCommand(
            userCommand = "Read my latest WhatsApp messages",
            messages = emptyList()
        )

        assertTrue(prompt.length < 1_200)
        assertFalse(prompt.contains("Tool registry:"))
    }
}

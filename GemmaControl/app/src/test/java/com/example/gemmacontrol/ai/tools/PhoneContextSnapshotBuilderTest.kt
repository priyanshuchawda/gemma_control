package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.notifications.WhatsAppContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneContextSnapshotBuilderTest {

    private val builder = PhoneContextSnapshotBuilder()

    @Test
    fun buildsEmptySnapshotWithStableSections() {
        val snapshot = builder.build(
            messages = emptyList(),
            activeNotificationKeys = emptySet()
        )

        assertTrue(snapshot.activeNotifications.isEmpty())
        assertTrue(snapshot.unreadChats.isEmpty())
        assertTrue(snapshot.relevantMessages.isEmpty())
    }

    @Test
    fun summarizesActiveChatsAndReplyTargetsWithoutRawHistoryDump() {
        val snapshot = builder.build(
            messages = listOf(
                message(id = "mom-new", chat = "Mom", text = "Call me", key = "active-mom", postedAt = 300),
                message(id = "mom-old", chat = "Mom", text = "Earlier", key = "old-mom", postedAt = 100),
                message(
                    id = "office-photo",
                    chat = "Office",
                    text = "Photo",
                    key = "active-office",
                    postedAt = 250,
                    kind = WhatsAppContentKind.PHOTO,
                    priority = "HIGH"
                )
            ),
            activeNotificationKeys = setOf("active-mom", "active-office"),
            latestActiveNotificationKey = "active-mom",
            maxChats = 2,
            maxMessages = 2
        )

        assertEquals(2, snapshot.activeNotifications.size)
        assertEquals(
            PhoneContextActiveNotification(
                notificationKey = "active-mom",
                conversationName = "Mom",
                unreadCount = 1,
                latestMessageEventId = "mom-new",
                latestSnippet = "Call me",
                latestContentKind = WhatsAppContentKind.TEXT,
                replyAvailable = true,
                latestReplyTarget = true
            ),
            snapshot.activeNotifications.first()
        )
        assertEquals(
            PhoneContextChatSummary(
                conversationName = "Mom",
                storedCount = 2,
                activeCount = 1,
                latestAt = 300,
                latestContentKind = WhatsAppContentKind.TEXT,
                latestSnippet = "Call me",
                highPriorityCount = 0,
                replyAvailable = true
            ),
            snapshot.unreadChats.first()
        )
        assertEquals(listOf("mom-new", "office-photo"), snapshot.relevantMessages.map { it.messageEventId })
        assertFalse(snapshot.relevantMessages.any { it.messageEventId == "mom-old" })
    }

    @Test
    fun preservesMediaAndHiddenTruthfulnessInSnapshotSnippets() {
        val snapshot = builder.build(
            messages = listOf(
                message(id = "photo", chat = "Mom", text = "Photo", kind = WhatsAppContentKind.PHOTO, postedAt = 2),
                message(id = "hidden", chat = "Dad", text = null, kind = WhatsAppContentKind.HIDDEN, postedAt = 1)
            ),
            activeNotificationKeys = emptySet()
        )

        assertEquals("Photo attachment (contents not inspected)", snapshot.relevantMessages[0].body)
        assertEquals("Content hidden or unavailable", snapshot.relevantMessages[1].body)
    }

    @Test
    fun promptUsesCompactPhoneContextSectionsAndBoundsLongText() {
        val promptBuilder = GemmaPromptBuilder(
            registry = WhatsAppToolRegistry.default(),
            clock = { PromptClockSnapshot("2026-06-05T06:20:00", "Friday") }
        )
        val snapshot = builder.build(
            messages = listOf(
                message(id = "long", chat = "Mom", text = "x".repeat(240), key = "active", postedAt = 1)
            ),
            activeNotificationKeys = setOf("active"),
            latestActiveNotificationKey = "active",
            maxSnippetChars = 24
        )

        val prompt = promptBuilder.buildForUserCommand(
            userCommand = "what did I miss",
            phoneContext = snapshot,
            maxBodyChars = 24
        )

        assertTrue(prompt.contains("Compact phone context:"))
        assertTrue(prompt.contains("Active WhatsApp notifications:"))
        assertTrue(prompt.contains("Unread chat summaries:"))
        assertTrue(prompt.contains("Relevant stored messages:"))
        assertTrue(prompt.contains("reply_available=true"))
        assertTrue(prompt.contains("latest_reply_target=true"))
        assertTrue(prompt.contains("body=xxxxxxxxxxxxxxxxxxxxxxxx..."))
        assertFalse(prompt.contains("x".repeat(80)))
    }

    private fun message(
        id: String,
        chat: String,
        text: String?,
        key: String = "key-$id",
        postedAt: Long,
        kind: WhatsAppContentKind = WhatsAppContentKind.TEXT,
        priority: String = "NORMAL"
    ): GemmaMessageContext {
        return GemmaMessageContext.fromDecryptedMessage(
            messageEventId = id,
            notificationKey = key,
            conversationName = chat,
            senderName = chat,
            decryptedText = text,
            postedAt = postedAt,
            priority = priority,
            contentKind = kind
        )
    }
}

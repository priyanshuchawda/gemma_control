package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import com.example.gemmacontrol.notifications.WhatsAppContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNotificationReadMapperTest {

    @Test
    fun toReadableMessages_keepsOnlyCurrentlyActiveNotificationMessages() {
        val active = event(
            key = "key-active",
            title = "Mom",
            messages = listOf(
                ParsedMessagePreview("Mom", "First live message", 1000L),
                ParsedMessagePreview("Mom", "Second live message", 2000L)
            ),
            isCurrentlyActive = true
        )
        val inactiveOlderEventForSameKey = active.copy(
            messages = listOf(ParsedMessagePreview("Mom", "Old inactive message", 500L)),
            isCurrentlyActive = false
        )
        val removed = event(
            key = "key-removed",
            title = "Office",
            messages = listOf(ParsedMessagePreview("Office", "Removed message", 3000L)),
            eventType = NotificationEventType.REMOVED,
            isCurrentlyActive = false
        )

        val messages = LiveNotificationReadMapper.toReadableMessages(
            listOf(active, inactiveOlderEventForSameKey, removed)
        )

        assertEquals(2, messages.size)
        assertEquals(listOf("First live message", "Second live message"), messages.map { it.decryptedText })
        assertTrue(messages.all { it.notificationKey == "key-active" })
        assertFalse(messages.any { it.decryptedText == "Old inactive message" })
        assertFalse(messages.any { it.decryptedText == "Removed message" })
    }

    @Test
    fun toReadableMessages_preservesMediaContentKindForSpeech() {
        val active = event(
            key = "key-photo",
            title = "Family",
            messages = listOf(
                ParsedMessagePreview(
                    senderName = "Mom",
                    messageText = "Photo",
                    timestamp = 4000L,
                    contentKind = WhatsAppContentKind.PHOTO
                )
            )
        )

        val messages = LiveNotificationReadMapper.toReadableMessages(listOf(active))

        assertEquals(1, messages.size)
        assertEquals("Family", messages.single().conversationId)
        assertEquals("Mom", messages.single().senderName)
        assertEquals("Photo", messages.single().decryptedText)
        assertEquals(WhatsAppContentKind.PHOTO, messages.single().contentKind)
    }

    @Test
    fun toReadableMessages_skipsSystemNotifications() {
        val system = event(
            key = "key-system",
            title = "WhatsApp",
            messages = listOf(
                ParsedMessagePreview(
                    senderName = "WhatsApp",
                    messageText = "Checking for new messages",
                    timestamp = 5000L,
                    contentKind = WhatsAppContentKind.SYSTEM
                )
            )
        )

        val messages = LiveNotificationReadMapper.toReadableMessages(listOf(system))

        assertTrue(messages.isEmpty())
    }

    @Test
    fun toReadableMessages_skipsExtrasFallbackSummaryWrappers() {
        val summaryWrapper = event(
            key = "key-summary",
            title = "WhatsApp",
            messages = listOf(ParsedMessagePreview("WhatsApp", "3 messages from 1 chat", 7000L)),
            parseSource = NotificationParseSource.EXTRAS_FALLBACK
        )

        val messages = LiveNotificationReadMapper.toReadableMessages(listOf(summaryWrapper))

        assertTrue(messages.isEmpty())
    }

    private fun event(
        key: String,
        title: String,
        messages: List<ParsedMessagePreview>,
        eventType: NotificationEventType = NotificationEventType.UPDATED,
        isCurrentlyActive: Boolean = true,
        parseSource: NotificationParseSource = NotificationParseSource.MESSAGING_STYLE
    ): ParsedWhatsAppNotificationEvent {
        return ParsedWhatsAppNotificationEvent(
            eventType = eventType,
            notificationKey = key,
            packageName = "com.whatsapp",
            observedAt = 6000L,
            notificationPostedAt = 6000L,
            conversationTitle = title,
            conversationType = ConversationType.DIRECT,
            messages = messages,
            currentMessageCount = messages.size,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = parseSource,
            isContentUnavailable = false,
            dedupeCandidate = "dedupe-$key",
            isCurrentlyActive = isCurrentlyActive
        )
    }
}

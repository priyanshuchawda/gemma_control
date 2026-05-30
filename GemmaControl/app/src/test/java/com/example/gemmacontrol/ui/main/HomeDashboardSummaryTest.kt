package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import junit.framework.TestCase.assertEquals
import org.junit.Test

class HomeDashboardSummaryTest {
    @Test
    fun buildHomeDashboardSummary_prioritizesPermissionRecoveryWhenAccessIsMissing() {
        val summary = buildHomeDashboardSummary(
            notifications = listOf(notification(conversationTitle = "Asha", isActive = true)),
            isPermissionGranted = false
        )

        assertEquals(HomeHeroAction.RequestNotificationAccess, summary.heroAction)
        assertEquals("Grant notification access", summary.heroActionLabel)
        assertEquals("Setup needs attention", summary.statusTitle)
        assertEquals(1, summary.capturedEventsCount)
        assertEquals(1, summary.activeNotificationsCount)
        assertEquals("Asha", summary.latestConversationTitle)
    }

    @Test
    fun buildHomeDashboardSummary_usesVoiceHeroWhenAppIsReady() {
        val summary = buildHomeDashboardSummary(
            notifications = listOf(
                notification(conversationTitle = "Mom", isActive = false),
                notification(conversationTitle = "Team", isActive = true)
            ),
            isPermissionGranted = true
        )

        assertEquals(HomeHeroAction.OpenVoiceAssistant, summary.heroAction)
        assertEquals("Speak to GemmaControl", summary.heroActionLabel)
        assertEquals("Ready for voice actions", summary.statusTitle)
        assertEquals(2, summary.capturedEventsCount)
        assertEquals(1, summary.activeNotificationsCount)
        assertEquals("Mom", summary.latestConversationTitle)
    }

    @Test
    fun buildHomeDashboardSummary_hasGuidedEmptyState() {
        val summary = buildHomeDashboardSummary(
            notifications = emptyList(),
            isPermissionGranted = true
        )

        assertEquals("Waiting for WhatsApp", summary.emptyTitle)
        assertEquals("Open WhatsApp or ask someone to send a message. New notifications appear here instantly.", summary.emptySubtitle)
    }

    @Test
    fun buildHomeDashboardSummary_usesPluralCopyForMultipleActiveNotifications() {
        val summary = buildHomeDashboardSummary(
            notifications = listOf(
                notification(conversationTitle = "Mom", isActive = true),
                notification(conversationTitle = "Team", isActive = true)
            ),
            isPermissionGranted = true
        )

        assertEquals("2 active WhatsApp notifications ready for reply.", summary.statusSubtitle)
    }

    private fun notification(
        conversationTitle: String,
        isActive: Boolean
    ): ParsedWhatsAppNotificationEvent {
        return ParsedWhatsAppNotificationEvent(
            eventType = NotificationEventType.POSTED,
            notificationKey = "key_$conversationTitle",
            packageName = "com.whatsapp",
            observedAt = 1_716_900_000_000L,
            notificationPostedAt = 1_716_900_000_000L,
            conversationTitle = conversationTitle,
            conversationType = ConversationType.DIRECT,
            messages = listOf(
                ParsedMessagePreview(
                    senderName = conversationTitle,
                    messageText = "Ping",
                    timestamp = 1_716_900_000_000L
                )
            ),
            currentMessageCount = 1,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            isContentUnavailable = false,
            dedupeCandidate = "dedupe_$conversationTitle",
            isCurrentlyActive = isActive
        )
    }
}

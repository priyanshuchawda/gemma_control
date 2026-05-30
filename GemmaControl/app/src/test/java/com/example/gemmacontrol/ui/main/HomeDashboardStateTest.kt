package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import junit.framework.TestCase.assertEquals
import org.junit.Test

class HomeDashboardStateTest {
    @Test
    fun buildHomeDashboardReadyState_keepsRepositoryAndModelSummariesTogether() {
        val state = buildHomeDashboardReadyState(
            HomeDashboardData(
                notifications = listOf(notification(conversationTitle = "Team", isActive = true)),
                isPermissionGranted = true,
                storedMessageCount = 12,
                actionableItemCount = 3,
                modelReadiness = HomeModelReadiness.Missing
            )
        )

        assertEquals(12, state.storedMessageCount)
        assertEquals(3, state.actionableItemCount)
        assertEquals(HomeModelReadiness.Missing, state.modelReadiness)
        assertEquals("Missing", state.modelReadinessLabel)
        assertEquals(HomeHeroAction.OpenVoiceAssistant, state.summary.heroAction)
        assertEquals(1, state.summary.activeNotificationsCount)
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
            messages = emptyList(),
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

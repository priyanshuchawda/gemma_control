package com.example.gemmacontrol.notifications

import com.example.gemmacontrol.ui.main.MainScreenUiState
import com.example.gemmacontrol.ui.main.MainScreenViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class WhatsAppNotificationParserTest {

    @Before
    fun setUp() {
        WhatsAppNotificationListener.clearList()
    }

    @Test
    fun testPackageAllowList_acceptsSupportedPackages() {
        // Supported WhatsApp Variants
        assertTrue(WhatsAppNotificationParser.isPackageSupported("com.whatsapp"))
        assertTrue(WhatsAppNotificationParser.isPackageSupported("com.whatsapp.w4b"))

        // Unrelated applications
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.facebook.orca"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.google.android.talk"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported(null))
        assertFalse(WhatsAppNotificationParser.isPackageSupported(""))
    }

    @Test
    fun testDedupeCandidate_isDeterministic() {
        val packageName = "com.whatsapp"
        val key = "key_123"
        val timestamp = 1716900000000L
        val title = "Mom"
        val sender = "Mom"
        val text = "Are you home?"

        val candidate1 = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName, key, timestamp, title, sender, text
        )
        val candidate2 = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName, key, timestamp, title, sender, text
        )

        // Same inputs must yield identical candidates
        assertEquals(candidate1, candidate2)

        // Differs when key differs
        val candidateDiffKey = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName, "key_456", timestamp, title, sender, text
        )
        assertFalse(candidate1 == candidateDiffKey)

        // Differs when message text differs
        val candidateDiffText = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName, key, timestamp, title, sender, "Heading out!"
        )
        assertFalse(candidate1 == candidateDiffText)

        // Differs when timestamp differs
        val candidateDiffTime = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName, key, timestamp + 1000, title, sender, text
        )
        assertFalse(candidate1 == candidateDiffTime)
    }

    @Test
    fun testInMemoryStateTransitions() = runTest {
        val viewModel = MainScreenViewModel()

        // Initially Success state with empty list
        var state = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
        assertTrue(state.notifications.isEmpty())

        // 1. Post a notification event (first time -> POSTED)
        val event1 = ParsedWhatsAppNotificationEvent(
            eventType = NotificationEventType.POSTED,
            notificationKey = "key_mom",
            packageName = "com.whatsapp",
            observedAt = System.currentTimeMillis(),
            notificationPostedAt = System.currentTimeMillis(),
            conversationTitle = "Mom",
            conversationType = ConversationType.DIRECT,
            messages = emptyList(),
            currentMessageCount = 0,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = NotificationParseSource.UNAVAILABLE,
            isContentUnavailable = true,
            dedupeCandidate = "dedupe_1",
            isCurrentlyActive = true
        )
        WhatsAppNotificationListener.postNotificationForTest(event1)

        state = viewModel.uiState.first { it is MainScreenUiState.Success && it.notifications.isNotEmpty() } as MainScreenUiState.Success
        assertEquals(1, state.notifications.size)
        val captured1 = state.notifications.first()
        assertEquals(NotificationEventType.POSTED, captured1.eventType)
        assertEquals("key_mom", captured1.notificationKey)
        assertTrue(captured1.isCurrentlyActive)

        // 2. Post subsequent update using same active key -> UPDATED
        val event2 = event1.copy(
            eventType = NotificationEventType.UPDATED,
            isContentUnavailable = false,
            messages = listOf(ParsedMessagePreview("Mom", "Hello!", System.currentTimeMillis())),
            currentMessageCount = 1
        )
        WhatsAppNotificationListener.postNotificationForTest(event2)

        state = viewModel.uiState.first { it is MainScreenUiState.Success && it.notifications.size == 2 } as MainScreenUiState.Success
        assertEquals(2, state.notifications.size)
        
        // Latest event is at index 0 (prepended)
        val latest = state.notifications[0]
        assertEquals(NotificationEventType.UPDATED, latest.eventType)
        assertEquals("key_mom", latest.notificationKey)
        assertTrue(latest.isCurrentlyActive)

        // Previous event at index 1 is now marked inactive
        val previous = state.notifications[1]
        assertEquals(NotificationEventType.POSTED, previous.eventType)
        assertFalse(previous.isCurrentlyActive)

        // 3. Remove the active notification key -> REMOVED and active state is updated
        WhatsAppNotificationListener.removeNotificationForTest("key_mom")

        state = viewModel.uiState.first { it is MainScreenUiState.Success && it.notifications.size == 3 } as MainScreenUiState.Success
        assertEquals(3, state.notifications.size)

        // REMOVED event is at the top
        val removed = state.notifications[0]
        assertEquals(NotificationEventType.REMOVED, removed.eventType)
        assertEquals("key_mom", removed.notificationKey)
        assertFalse(removed.isCurrentlyActive)

        // All previous ones are marked inactive
        assertFalse(state.notifications[1].isCurrentlyActive)
        assertFalse(state.notifications[2].isCurrentlyActive)
    }

    @Test
    fun testEventHistoryCappedTo100() = runTest {
        // Clear listener state first
        WhatsAppNotificationListener.clearList()

        // Push 105 events
        for (i in 1..105) {
            val event = ParsedWhatsAppNotificationEvent(
                eventType = NotificationEventType.POSTED,
                notificationKey = "key_$i",
                packageName = "com.whatsapp",
                observedAt = System.currentTimeMillis() + i, // unique timestamp for stable ordering
                notificationPostedAt = System.currentTimeMillis(),
                conversationTitle = "Contact_$i",
                conversationType = ConversationType.UNKNOWN,
                messages = emptyList(),
                currentMessageCount = 0,
                historicMessageCount = 0,
                hasReplyActionAtCaptureTime = false,
                parseSource = NotificationParseSource.UNAVAILABLE,
                isContentUnavailable = true,
                dedupeCandidate = "dedupe_$i",
                isCurrentlyActive = true
            )
            WhatsAppNotificationListener.postNotificationForTest(event)
        }

        val viewModel = MainScreenViewModel()
        val state = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
        
        // Assert that history size is bounded/capped strictly to 100
        assertEquals(100, state.notifications.size)
    }
}

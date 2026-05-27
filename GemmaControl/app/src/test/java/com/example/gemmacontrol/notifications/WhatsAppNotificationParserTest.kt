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
        // Supported
        assertTrue(WhatsAppNotificationParser.isPackageSupported("com.whatsapp"))
        assertTrue(WhatsAppNotificationParser.isPackageSupported("com.whatsapp.w4b"))

        // Unrelated/Unsupported
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.facebook.orca"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.google.android.talk"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.telegram.messenger"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported(null))
        assertFalse(WhatsAppNotificationParser.isPackageSupported(""))
    }

    @Test
    fun testDedupeHash_isDeterministic() {
        val title = "Mom"
        val sender = "Mom"
        val text = "Are you home?"
        val timestamp = 1716900000000L

        val hash1 = WhatsAppNotificationParser.generateDedupeHash(title, sender, text, timestamp)
        val hash2 = WhatsAppNotificationParser.generateDedupeHash(title, sender, text, timestamp)

        // Same inputs must yield same hash
        assertEquals(hash1, hash2)

        // Different sender
        val hashDifferentSender = WhatsAppNotificationParser.generateDedupeHash(title, "Dad", text, timestamp)
        assertFalse(hash1 == hashDifferentSender)

        // Different text
        val hashDifferentText = WhatsAppNotificationParser.generateDedupeHash(title, sender, "Heading out", timestamp)
        assertFalse(hash1 == hashDifferentText)

        // Different timestamp
        val hashDifferentTime = WhatsAppNotificationParser.generateDedupeHash(title, sender, text, timestamp + 1000)
        assertFalse(hash1 == hashDifferentTime)
    }

    @Test
    fun testViewModelCapturedNotifications_correctlyUpdates() = runTest {
        val viewModel = MainScreenViewModel()

        // Wait until it emits a Success state (which happens immediately when flow collects)
        var currentState = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
        assertTrue(currentState.notifications.isEmpty())

        // Post a notification
        val parsed = ParsedNotification(
            notificationKey = "key_1",
            senderName = "Amit",
            conversationTitle = "Project Group",
            messageText = "Let's meet tomorrow.",
            postedAt = System.currentTimeMillis(),
            isGroup = true,
            hasReplyAction = true,
            isRedacted = false,
            dedupeHash = "dummy_hash_1"
        )
        WhatsAppNotificationListener.postNotificationForTest(parsed)

        // Wait for next Success state
        currentState = viewModel.uiState.first { it is MainScreenUiState.Success && it.notifications.isNotEmpty() } as MainScreenUiState.Success
        val notifications = currentState.notifications
        assertEquals(1, notifications.size)
        assertEquals("key_1", notifications[0].notificationKey)
        assertEquals("Amit", notifications[0].senderName)
        assertEquals("Project Group", notifications[0].conversationTitle)
        assertEquals("Let's meet tomorrow.", notifications[0].messageText)
        assertTrue(notifications[0].isGroup)
        assertTrue(notifications[0].isActive)

        // Remove/Expire the notification
        WhatsAppNotificationListener.removeNotificationForTest("key_1")

        // Wait for state to reflect inactive
        currentState = viewModel.uiState.first { it is MainScreenUiState.Success && !it.notifications[0].isActive } as MainScreenUiState.Success
        val updatedNotifications = currentState.notifications
        assertEquals(1, updatedNotifications.size)
        assertFalse(updatedNotifications[0].isActive)
        assertTrue(updatedNotifications[0].removedAt != null)
    }
}

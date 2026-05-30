package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.NotificationParseSource
import junit.framework.TestCase.assertEquals
import org.junit.Test

class StoredInboxSheetStateTest {
    @Test
    fun storedInboxSheetTitle_namesEachBottomSheet() {
        val message = decryptedMessage()

        assertEquals("Enable Local Storage?", storedInboxSheetTitle(StoredInboxSheet.EnableStorage))
        assertEquals("Delete All Messages?", storedInboxSheetTitle(StoredInboxSheet.DeleteAll))
        assertEquals("Reply to Mom", storedInboxSheetTitle(StoredInboxSheet.ComposeReply(message)))
        assertEquals("Confirm Send?", storedInboxSheetTitle(StoredInboxSheet.ConfirmReply(message)))
    }

    private fun decryptedMessage(): StoredInboxRepository.DecryptedMessage {
        return StoredInboxRepository.DecryptedMessage(
            id = "message-1",
            conversationId = "Mom",
            senderName = "Mom",
            decryptedText = "Call me tomorrow",
            postedAt = 1_716_900_000_000L,
            notificationKey = "key-1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            isContentUnavailable = false,
            createdAt = 1_716_900_000_000L,
            priority = "NORMAL"
        )
    }
}

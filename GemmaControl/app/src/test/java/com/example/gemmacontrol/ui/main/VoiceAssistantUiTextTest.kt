package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAssistantUiTextTest {

    @Test
    fun statusTitleNamesPrimaryVoiceStates() {
        assertEquals("Speak commands for WhatsApp notifications", voiceAssistantStatusTitle(VoiceAssistantState.Idle))
        assertEquals("Listening... Speak now", voiceAssistantStatusTitle(VoiceAssistantState.Listening))
        assertEquals("Review Dictated Reply", voiceAssistantStatusTitle(VoiceAssistantState.ConfirmationRequired(
            PendingVoiceReply(
                notificationKey = "key",
                replyText = "ok",
                conversationTitle = "Chat"
            )
        )))
    }

    @Test
    fun subtitleKeepsOfflineAndSystemRecognitionPrivacyBoundariesSeparate() {
        val offlineSubtitle = voiceAssistantSubtitle(VoiceAssistantState.Listening, isOffline = true)
        val systemSubtitle = voiceAssistantSubtitle(VoiceAssistantState.Listening, isOffline = false)

        assertTrue(offlineSubtitle.contains("Private on-device speech recognition active."))
        assertTrue(systemSubtitle.contains("speech may be processed outside this device"))
    }

    @Test
    fun failureSubtitleUsesSafeReason() {
        val subtitle = voiceAssistantSubtitle(
            VoiceAssistantState.Failure("No active WhatsApp notification is available for reply."),
            isOffline = true
        )

        assertEquals("No active WhatsApp notification is available for reply.", subtitle)
    }
}

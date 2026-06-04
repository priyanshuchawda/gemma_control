package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTypedCommandInputTest {

    @Test
    fun idleStateAllowsNonBlankTypedCommand() {
        assertTrue(
            canSubmitTypedVoiceCommand(
                text = "show my latest WhatsApp messages",
                state = VoiceAssistantState.Idle
            )
        )
    }

    @Test
    fun blankTypedCommandCannotSubmit() {
        assertFalse(canSubmitTypedVoiceCommand("   ", VoiceAssistantState.Idle))
    }

    @Test
    fun activeRuntimeStatesDisableTypedCommand() {
        assertFalse(canSubmitTypedVoiceCommand("read messages", VoiceAssistantState.Listening))
        assertFalse(canSubmitTypedVoiceCommand("read messages", VoiceAssistantState.Streaming("")))
        assertFalse(canSubmitTypedVoiceCommand("read messages", VoiceAssistantState.SpeakingMessages(1)))
    }

    @Test
    fun pendingConfirmationDisablesTypedCommand() {
        assertFalse(
            canSubmitTypedVoiceCommand(
                text = "read messages",
                state = VoiceAssistantState.ConfirmationRequired(
                    PendingVoiceReply(
                        notificationKey = "key",
                        replyText = "ok",
                        conversationTitle = "Mom"
                    )
                )
            )
        )
    }
}

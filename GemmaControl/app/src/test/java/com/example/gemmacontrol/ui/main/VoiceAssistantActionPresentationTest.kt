package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolDefinition
import com.example.gemmacontrol.ai.tools.ToolProposal
import com.example.gemmacontrol.ai.tools.ToolExecutionScope
import com.example.gemmacontrol.ai.tools.ToolSafetyLevel
import com.example.gemmacontrol.ai.tools.WhatsAppToolName
import junit.framework.TestCase.assertEquals
import org.junit.Test

class VoiceAssistantActionPresentationTest {
    @Test
    fun voiceAssistantActionPresentation_movesConfirmationsAndResultsToBottomSheet() {
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(
                VoiceAssistantState.CommandReady(VoiceCommand.ReadLatestMessages)
            )
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(
                VoiceAssistantState.CommandReady(VoiceCommand.ReadMessagesFromConversation("Mom"))
            )
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(
                VoiceAssistantState.ConfirmationRequired(
                    PendingVoiceReply(
                        notificationKey = "key",
                        replyText = "ok",
                        conversationTitle = "Mom"
                    )
                )
            )
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(
                VoiceAssistantState.LocalToolConfirmationRequired(localToolAction())
            )
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(VoiceAssistantState.LocalToolSucceeded("Done"))
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(VoiceAssistantState.Failure("No active notification."))
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(VoiceAssistantState.LanguagePackMissingError)
        )
        assertEquals(
            VoiceActionPresentation.BottomSheet,
            voiceAssistantActionPresentation(VoiceAssistantState.ConfirmSystemRecognitionConsent)
        )
    }

    @Test
    fun voiceAssistantActionPresentation_keepsAmbientVoiceStatesInline() {
        assertEquals(VoiceActionPresentation.Inline, voiceAssistantActionPresentation(VoiceAssistantState.Idle))
        assertEquals(VoiceActionPresentation.Inline, voiceAssistantActionPresentation(VoiceAssistantState.Streaming("Drafting")))
        assertEquals(VoiceActionPresentation.Inline, voiceAssistantActionPresentation(VoiceAssistantState.SpeakingMessages(1)))
        assertEquals(VoiceActionPresentation.None, voiceAssistantActionPresentation(VoiceAssistantState.Listening))
    }

    private fun localToolAction(): PendingLocalToolAction {
        val definition = ToolDefinition(
            name = WhatsAppToolName.ListRecentWhatsAppMessages,
            description = "List recent local messages.",
            safetyLevel = ToolSafetyLevel.ReadOnly
        )
        val proposal = ToolProposal(
            name = WhatsAppToolName.ListRecentWhatsAppMessages,
            arguments = emptyMap(),
            definition = definition
        )
        return PendingLocalToolAction(
            title = "List recent messages",
            description = "Read recent local messages.",
            confirmText = "Read",
            proposal = proposal,
            decision = ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.ReadOnlyLocalData
            )
        )
    }
}

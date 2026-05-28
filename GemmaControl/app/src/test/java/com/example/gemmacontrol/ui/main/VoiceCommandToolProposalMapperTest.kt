package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolSafetyLevel
import com.example.gemmacontrol.ai.tools.ToolConfirmationMode
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolExecutionScope
import com.example.gemmacontrol.ai.tools.WhatsAppToolName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandToolProposalMapperTest {

    private val mapper = VoiceCommandToolProposalMapper()

    @Test
    fun mapsReadLatestMessagesToReadOnlyToolProposal() {
        val result = mapper.mapReadLatestMessages(limit = 3)

        assertTrue(result is VoiceToolProposalResult.Valid)
        val proposal = (result as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.ListRecentWhatsAppMessages, proposal.name)
        assertEquals(3, proposal.integer("limit"))
        assertEquals(ToolSafetyLevel.ReadOnly, proposal.definition.safetyLevel)
        assertEquals(
            ToolExecutionDecision.AllowLocalExecution(proposal, ToolExecutionScope.ReadOnlyLocalData),
            result.decision
        )
    }

    @Test
    fun mapsActiveReplyToStrictManualConfirmationProposal() {
        val result = mapper.mapActiveNotificationReply(
            notificationKey = "active-key-1",
            replyText = "I am in a meeting"
        )

        assertTrue(result is VoiceToolProposalResult.Valid)
        val proposal = (result as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.SendReplyToActiveWhatsAppNotification, proposal.name)
        assertEquals("active-key-1", proposal.string("notification_key"))
        assertEquals("I am in a meeting", proposal.string("message_text"))
        assertEquals(ToolSafetyLevel.StrictManualConfirmation, proposal.definition.safetyLevel)
        val decision = result.decision
        assertTrue(decision is ToolExecutionDecision.RequireUserConfirmation)
        assertEquals(ToolConfirmationMode.StrictManual, (decision as ToolExecutionDecision.RequireUserConfirmation).requirement.mode)
        assertEquals(true, decision.requirement.requiresActiveNotification)
    }

    @Test
    fun rejectsInvalidVoiceReplyProposalBeforeUiConfirmation() {
        val result = mapper.mapActiveNotificationReply(
            notificationKey = "active-key-1",
            replyText = "   "
        )

        assertEquals(
            VoiceToolProposalResult.Invalid("Invalid message_text: must not be blank"),
            result
        )
    }

    @Test
    fun rejectsVoiceReplyWhenSafetyRouterRejectsProposal() {
        val result = mapper.mapActiveNotificationReply(
            notificationKey = "",
            replyText = "ok"
        )

        assertEquals(
            VoiceToolProposalResult.Invalid("Active notification reply requires notification_key."),
            result
        )
    }
}

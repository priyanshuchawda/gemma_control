package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolSafetyLevel
import com.example.gemmacontrol.ai.tools.ToolConfirmationMode
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolExecutionScope
import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
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
    fun mapsNativeReadLatestActionToReadOnlyToolProposal() {
        val result = mapper.mapNativeToolAction(
            WhatsAppToolAction.ReadLatestNotifications(limit = 3)
        )

        assertTrue(result is VoiceToolProposalResult.Valid)
        val proposal = (result as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.ListRecentWhatsAppMessages, proposal.name)
        assertEquals(3, proposal.integer("limit"))
        assertEquals(ToolExecutionDecision.AllowLocalExecution(proposal, ToolExecutionScope.ReadOnlyLocalData), result.decision)
    }

    @Test
    fun mapsNativeSenderLookupActionToFilteredReadOnlyToolProposal() {
        val result = mapper.mapNativeToolAction(
            WhatsAppToolAction.GetNotificationFrom(senderName = "Mom", limit = 3)
        )

        assertTrue(result is VoiceToolProposalResult.Valid)
        val proposal = (result as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.ListRecentWhatsAppMessages, proposal.name)
        assertEquals("Mom", proposal.string("conversation_name"))
        assertEquals(3, proposal.integer("limit"))
    }

    @Test
    fun mapsNativeReplyActionToPendingLatestReplyProposal() {
        val result = mapper.mapNativeToolAction(
            WhatsAppToolAction.ReplyToLatestNotification("I am in a meeting")
        )

        assertTrue(result is VoiceToolProposalResult.Valid)
        val proposal = (result as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.SendReplyToActiveWhatsAppNotification, proposal.name)
        assertEquals("__latest_active_notification__", proposal.string("notification_key"))
        assertEquals("I am in a meeting", proposal.string("message_text"))
        assertTrue(result.decision is ToolExecutionDecision.RequireUserConfirmation)
    }

    @Test
    fun mapsExpandedReadNativeActionsToReadProposals() {
        val summarize = mapper.mapNativeToolAction(WhatsAppToolAction.SummarizeMessages(limit = 8))
        assertTrue(summarize is VoiceToolProposalResult.Valid)
        val summarizeProposal = (summarize as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.ListRecentWhatsAppMessages, summarizeProposal.name)
        assertEquals("summarize", summarizeProposal.string("read_mode"))
        assertEquals(8, summarizeProposal.integer("limit"))

        val chat = mapper.mapNativeToolAction(WhatsAppToolAction.GetChatMessages(conversationName = "Mom", limit = 6))
        assertTrue(chat is VoiceToolProposalResult.Valid)
        val chatProposal = (chat as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.ListRecentWhatsAppMessages, chatProposal.name)
        assertEquals("Mom", chatProposal.string("conversation_name"))
        assertEquals("chat", chatProposal.string("read_mode"))
    }

    @Test
    fun mapsExpandedMutationNativeActionsToConfirmationProposals() {
        val draft = mapper.mapNativeToolAction(
            WhatsAppToolAction.DraftReply(conversationName = "Mom", messageText = "On my way")
        )
        assertTrue(draft is VoiceToolProposalResult.Valid)
        assertEquals(WhatsAppToolName.DraftWhatsAppReply, (draft as VoiceToolProposalResult.Valid).proposal.name)
        assertTrue(draft.decision is ToolExecutionDecision.RequireUserConfirmation)

        val followUp = mapper.mapNativeToolAction(
            WhatsAppToolAction.CreateFollowUp(messageEventId = "message-1", followUpTitle = "Call back")
        )
        assertTrue(followUp is VoiceToolProposalResult.Valid)
        val followUpProposal = (followUp as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.CreateFollowUpFromMessage, followUpProposal.name)
        assertEquals(ToolExecutionScope.LocalDataWrite, (followUp.decision as ToolExecutionDecision.AllowLocalExecution).scope)

        val important = mapper.mapNativeToolAction(WhatsAppToolAction.MarkImportant(messageEventId = "message-1"))
        assertTrue(important is VoiceToolProposalResult.Valid)
        val importantProposal = (important as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.MarkMessagePriority, importantProposal.name)
        assertEquals("HIGH", importantProposal.string("priority"))
    }

    @Test
    fun mapsExplicitNativeReplyAndPauseActions() {
        val reply = mapper.mapNativeToolAction(
            WhatsAppToolAction.ReplyActiveNotification(notificationKey = "active-key", messageText = "Ok")
        )
        assertTrue(reply is VoiceToolProposalResult.Valid)
        val replyProposal = (reply as VoiceToolProposalResult.Valid).proposal
        assertEquals(WhatsAppToolName.SendReplyToActiveWhatsAppNotification, replyProposal.name)
        assertEquals("active-key", replyProposal.string("notification_key"))

        val pause = mapper.mapNativeToolAction(WhatsAppToolAction.PauseCapture)
        assertTrue(pause is VoiceToolProposalResult.Valid)
        assertEquals(WhatsAppToolName.PauseWhatsAppCapture, (pause as VoiceToolProposalResult.Valid).proposal.name)
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
        assertEquals(ToolSafetyLevel.SendMessage, proposal.definition.safetyLevel)
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

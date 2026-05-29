package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.tools.ToolProposal
import com.example.gemmacontrol.ai.tools.WhatsAppToolName

data class FunctionGemmaVoiceProposalContext(
    val activeNotificationKeys: Set<String>,
    val conversationTitleByNotificationKey: Map<String, String> = emptyMap()
)

class FunctionGemmaVoiceProposalHandler(
    private val mapper: VoiceCommandToolProposalMapper = VoiceCommandToolProposalMapper()
) {
    fun resolve(
        result: GemmaEngineResult,
        context: FunctionGemmaVoiceProposalContext
    ): VoiceAssistantState {
        return when (result) {
            is GemmaEngineResult.ProposalText -> resolveProposal(result, context)
            is GemmaEngineResult.Blocked -> VoiceAssistantState.Failure(result.reason)
            is GemmaEngineResult.Failure -> VoiceAssistantState.Failure(result.safeReason)
            GemmaEngineResult.Ready -> VoiceAssistantState.Failure("FunctionGemma did not return a tool proposal.")
        }
    }

    private fun resolveProposal(
        result: GemmaEngineResult.ProposalText,
        context: FunctionGemmaVoiceProposalContext
    ): VoiceAssistantState {
        return when (val mapped = mapper.mapParseResult(result.parseResult)) {
            is VoiceToolProposalResult.Invalid -> VoiceAssistantState.Failure(mapped.reason)
            is VoiceToolProposalResult.Valid -> mapped.toVoiceState(context)
        }
    }

    private fun VoiceToolProposalResult.Valid.toVoiceState(
        context: FunctionGemmaVoiceProposalContext
    ): VoiceAssistantState {
        return when (proposal.name) {
            WhatsAppToolName.ListRecentWhatsAppMessages -> {
                VoiceAssistantState.CommandReady(VoiceCommand.ReadLatestMessages)
            }
            WhatsAppToolName.SendReplyToActiveWhatsAppNotification -> {
                proposal.toActiveReplyState(context)
            }
            WhatsAppToolName.PauseWhatsAppCapture,
            WhatsAppToolName.ResumeWhatsAppCapture,
            WhatsAppToolName.CreateFollowUpFromMessage,
            WhatsAppToolName.MarkFollowUpCompleted,
            WhatsAppToolName.ScheduleReminderForMessage,
            WhatsAppToolName.MarkMessagePriority,
            WhatsAppToolName.DeleteLocalWhatsAppData -> {
                toLocalToolConfirmationState()
            }
            else -> unsupportedProposalState(proposal)
        }
    }

    private fun VoiceToolProposalResult.Valid.toLocalToolConfirmationState(): VoiceAssistantState {
        val action = when (proposal.name) {
            WhatsAppToolName.PauseWhatsAppCapture -> PendingLocalToolAction(
                title = "Pause WhatsApp capture?",
                description = "GemmaControl will stop storing new WhatsApp notification previews until capture is resumed.",
                confirmText = "Pause Capture",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.ResumeWhatsAppCapture -> PendingLocalToolAction(
                title = "Resume WhatsApp capture?",
                description = "GemmaControl will resume processing future WhatsApp notification previews.",
                confirmText = "Resume Capture",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.CreateFollowUpFromMessage -> PendingLocalToolAction(
                title = "Create follow-up?",
                description = "GemmaControl will save this as a local follow-up task for the selected WhatsApp message.",
                confirmText = "Save Follow-Up",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.MarkFollowUpCompleted -> PendingLocalToolAction(
                title = "Mark follow-up complete?",
                description = "GemmaControl will mark this local follow-up task as completed.",
                confirmText = "Mark Complete",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.ScheduleReminderForMessage -> PendingLocalToolAction(
                title = "Schedule reminder?",
                description = "GemmaControl will store the reminder locally and post a notification at the requested time.",
                confirmText = "Schedule Reminder",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.MarkMessagePriority -> PendingLocalToolAction(
                title = "Mark message ${proposal.string("priority").orEmpty()} priority?",
                description = "GemmaControl will update only the local inbox priority flag for this captured WhatsApp message.",
                confirmText = "Mark Priority",
                proposal = proposal,
                decision = decision
            )
            WhatsAppToolName.DeleteLocalWhatsAppData -> PendingLocalToolAction(
                title = deleteLocalDataTitle(proposal),
                description = deleteLocalDataDescription(proposal),
                confirmText = "Delete Local Data",
                proposal = proposal,
                decision = decision
            )
            else -> return unsupportedProposalState(proposal)
        }
        return VoiceAssistantState.LocalToolConfirmationRequired(action)
    }

    private fun deleteLocalDataTitle(proposal: ToolProposal): String {
        val conversationName = proposal.string("conversation_name")?.trim()
        return if (conversationName.isNullOrBlank()) {
            "Delete all local WhatsApp data?"
        } else {
            "Delete local data for $conversationName?"
        }
    }

    private fun deleteLocalDataDescription(proposal: ToolProposal): String {
        val conversationName = proposal.string("conversation_name")?.trim()
        return if (conversationName.isNullOrBlank()) {
            "This removes locally stored WhatsApp conversations and message previews from this app."
        } else {
            "This removes locally stored WhatsApp message previews for $conversationName from this app."
        }
    }

    private fun ToolProposal.toActiveReplyState(
        context: FunctionGemmaVoiceProposalContext
    ): VoiceAssistantState {
        val notificationKey = string("notification_key").orEmpty()
        val replyText = string("message_text").orEmpty()
        if (notificationKey !in context.activeNotificationKeys) {
            return VoiceAssistantState.Failure("The proposed WhatsApp notification is no longer active.")
        }
        if (replyText.isBlank()) {
            return VoiceAssistantState.Failure("FunctionGemma proposed an empty reply.")
        }

        return VoiceAssistantState.ConfirmationRequired(
            PendingVoiceReply(
                notificationKey = notificationKey,
                replyText = replyText,
                conversationTitle = context.conversationTitleByNotificationKey[notificationKey]
                    ?: "Latest Conversation"
            )
        )
    }

    private fun unsupportedProposalState(proposal: ToolProposal): VoiceAssistantState.Failure {
        return VoiceAssistantState.Failure(
            "FunctionGemma proposed ${proposal.name.value}, but that action is not wired to the voice UI yet."
        )
    }
}

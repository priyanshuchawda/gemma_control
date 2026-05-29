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
            else -> unsupportedProposalState(proposal)
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

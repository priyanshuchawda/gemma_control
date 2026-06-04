package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.tools.ToolProposal
import com.example.gemmacontrol.ai.tools.WhatsAppToolName

data class FunctionGemmaVoiceProposalContext(
    val activeNotificationKeys: Set<String>,
    val latestActiveNotificationKey: String? = null,
    val conversationTitleByNotificationKey: Map<String, String> = emptyMap()
)

private data class LocalToolConfirmationCopy(
    val title: String,
    val description: String,
    val confirmText: String
)

private val localToolConfirmationCopyByName = mapOf(
    WhatsAppToolName.PauseWhatsAppCapture to LocalToolConfirmationCopy(
        title = "Pause WhatsApp capture?",
        description = "GemmaControl will stop storing new WhatsApp notification previews until capture is resumed.",
        confirmText = "Pause Capture"
    ),
    WhatsAppToolName.ResumeWhatsAppCapture to LocalToolConfirmationCopy(
        title = "Resume WhatsApp capture?",
        description = "GemmaControl will resume processing future WhatsApp notification previews.",
        confirmText = "Resume Capture"
    ),
    WhatsAppToolName.SearchWhatsAppMessages to LocalToolConfirmationCopy(
        title = "Search local WhatsApp messages?",
        description = "GemmaControl will search only locally stored decrypted WhatsApp notification rows.",
        confirmText = "Search Messages"
    ),
    WhatsAppToolName.GetWhatsAppMessageDetails to LocalToolConfirmationCopy(
        title = "Show message details?",
        description = "GemmaControl will read one locally stored WhatsApp message row.",
        confirmText = "Show Details"
    ),
    WhatsAppToolName.GetActionableInbox to LocalToolConfirmationCopy(
        title = "Show actionable inbox?",
        description = "GemmaControl will read local pending follow-ups and high-priority WhatsApp message flags.",
        confirmText = "Show Inbox"
    ),
    WhatsAppToolName.CreateFollowUpFromMessage to LocalToolConfirmationCopy(
        title = "Create follow-up?",
        description = "GemmaControl will save this as a local follow-up task for the selected WhatsApp message.",
        confirmText = "Save Follow-Up"
    ),
    WhatsAppToolName.ListPendingFollowUps to LocalToolConfirmationCopy(
        title = "Show pending follow-ups?",
        description = "GemmaControl will read pending local follow-up tasks from encrypted app storage.",
        confirmText = "Show Follow-Ups"
    ),
    WhatsAppToolName.MarkFollowUpCompleted to LocalToolConfirmationCopy(
        title = "Mark follow-up complete?",
        description = "GemmaControl will mark this local follow-up task as completed.",
        confirmText = "Mark Complete"
    ),
    WhatsAppToolName.ScheduleReminderForMessage to LocalToolConfirmationCopy(
        title = "Schedule reminder?",
        description = "GemmaControl will store the reminder locally and post a notification at the requested time.",
        confirmText = "Schedule Reminder"
    ),
    WhatsAppToolName.DraftWhatsAppReply to LocalToolConfirmationCopy(
        title = "Prepare WhatsApp reply draft?",
        description = "GemmaControl will prepare draft text locally without sending anything.",
        confirmText = "Prepare Draft"
    ),
    WhatsAppToolName.OpenWhatsAppShareDraft to LocalToolConfirmationCopy(
        title = "Open WhatsApp share draft?",
        description = "GemmaControl will open WhatsApp with prepared text. You still choose the recipient and send manually.",
        confirmText = "Open WhatsApp"
    ),
    WhatsAppToolName.OpenWhatsAppClickToChat to LocalToolConfirmationCopy(
        title = "Open WhatsApp chat draft?",
        description = "GemmaControl will open WhatsApp with prepared text for the verified phone number. You still send manually.",
        confirmText = "Open WhatsApp"
    )
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
            is GemmaEngineResult.NativeToolAction -> resolveNativeToolAction(result, context)
            is GemmaEngineResult.Blocked -> VoiceAssistantState.Failure(result.reason)
            is GemmaEngineResult.Failure -> VoiceAssistantState.Failure(result.safeReason)
            GemmaEngineResult.Ready -> VoiceAssistantState.Failure("FunctionGemma did not return a tool proposal.")
        }
    }

    private fun resolveNativeToolAction(
        result: GemmaEngineResult.NativeToolAction,
        context: FunctionGemmaVoiceProposalContext
    ): VoiceAssistantState {
        return when (val mapped = mapper.mapNativeToolAction(result.action)) {
            is VoiceToolProposalResult.Invalid -> VoiceAssistantState.Failure(mapped.reason)
            is VoiceToolProposalResult.Valid -> mapped.toVoiceState(context)
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
            WhatsAppToolName.SearchWhatsAppMessages,
            WhatsAppToolName.GetWhatsAppMessageDetails,
            WhatsAppToolName.GetActionableInbox,
            WhatsAppToolName.CreateFollowUpFromMessage,
            WhatsAppToolName.ListPendingFollowUps,
            WhatsAppToolName.MarkFollowUpCompleted,
            WhatsAppToolName.ScheduleReminderForMessage,
            WhatsAppToolName.MarkMessagePriority,
            WhatsAppToolName.DraftWhatsAppReply,
            WhatsAppToolName.OpenWhatsAppShareDraft,
            WhatsAppToolName.OpenWhatsAppClickToChat,
            WhatsAppToolName.DeleteLocalWhatsAppData -> {
                toLocalToolConfirmationState()
            }
        }
    }

    private fun VoiceToolProposalResult.Valid.toLocalToolConfirmationState(): VoiceAssistantState {
        val copy = localToolConfirmationCopy(proposal) ?: return unsupportedProposalState(proposal)
        val action = PendingLocalToolAction(
            title = copy.title,
            description = copy.description,
            confirmText = copy.confirmText,
            proposal = proposal,
            decision = decision
        )
        return VoiceAssistantState.LocalToolConfirmationRequired(action)
    }

    private fun localToolConfirmationCopy(proposal: ToolProposal): LocalToolConfirmationCopy? {
        return when (proposal.name) {
            WhatsAppToolName.MarkMessagePriority -> priorityConfirmationCopy(proposal)
            WhatsAppToolName.DeleteLocalWhatsAppData -> deleteLocalDataConfirmationCopy(proposal)
            else -> localToolConfirmationCopyByName[proposal.name]
        }
    }

    private fun priorityConfirmationCopy(proposal: ToolProposal): LocalToolConfirmationCopy {
        return LocalToolConfirmationCopy(
            title = "Mark message ${proposal.string("priority").orEmpty()} priority?",
            description = "GemmaControl will update only the local inbox priority flag for this captured WhatsApp message.",
            confirmText = "Mark Priority"
        )
    }

    private fun deleteLocalDataConfirmationCopy(proposal: ToolProposal): LocalToolConfirmationCopy {
        return LocalToolConfirmationCopy(
            title = deleteLocalDataTitle(proposal),
            description = deleteLocalDataDescription(proposal),
            confirmText = "Delete Local Data"
        )
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
        val notificationKey = resolveNotificationKey(context)
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

    private fun ToolProposal.resolveNotificationKey(
        context: FunctionGemmaVoiceProposalContext
    ): String {
        val proposedKey = string("notification_key").orEmpty()
        return if (proposedKey == VoiceCommandToolProposalMapper.LatestActiveNotificationKey) {
            context.latestActiveNotificationKey
                ?.takeIf { it in context.activeNotificationKeys }
                ?: context.activeNotificationKeys.firstOrNull().orEmpty()
        } else {
            proposedKey
        }
    }

    private fun unsupportedProposalState(proposal: ToolProposal): VoiceAssistantState.Failure {
        return VoiceAssistantState.Failure(
            "FunctionGemma proposed ${proposal.name.value}, but that action is not wired to the voice UI yet."
        )
    }
}

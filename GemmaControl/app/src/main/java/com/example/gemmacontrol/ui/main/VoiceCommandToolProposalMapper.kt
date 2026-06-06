package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolArgument
import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.ToolCallParser
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolProposal
import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
import com.example.gemmacontrol.ai.tools.ToolSafetyRouter
import com.example.gemmacontrol.ai.tools.WhatsAppToolName
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

sealed interface VoiceToolProposalResult {
    data class Valid(
        val proposal: ToolProposal,
        val decision: ToolExecutionDecision
    ) : VoiceToolProposalResult
    data class Invalid(val reason: String) : VoiceToolProposalResult
}

class VoiceCommandToolProposalMapper(
    private val registry: WhatsAppToolRegistry = WhatsAppToolRegistry.default(),
    private val parser: ToolCallParser = ToolCallParser(registry),
    private val safetyRouter: ToolSafetyRouter = ToolSafetyRouter()
) {
    fun mapNativeToolAction(action: WhatsAppToolAction): VoiceToolProposalResult {
        return when (action) {
            is WhatsAppToolAction.ReadLatestNotifications -> mapReadLatestMessages(action.limit)
            is WhatsAppToolAction.GetNotificationFrom -> mapRecentMessagesFromSender(
                senderName = action.senderName,
                limit = action.limit
            )
            is WhatsAppToolAction.ReplyToLatestNotification -> mapActiveNotificationReply(
                notificationKey = LatestActiveNotificationKey,
                replyText = action.replyText
            )
            is WhatsAppToolAction.ListUnreadChats -> mapListRecent(
                limit = action.limit,
                readMode = "unread"
            )
            is WhatsAppToolAction.ReadMessages -> mapListRecent(
                limit = action.limit,
                conversationName = action.conversationName,
                readMode = if (action.conversationName.isNullOrBlank()) "latest" else "chat"
            )
            is WhatsAppToolAction.SummarizeMessages -> mapListRecent(
                limit = action.limit,
                conversationName = action.conversationName,
                readMode = "summarize"
            )
            is WhatsAppToolAction.SearchMessages -> mapSearchMessages(action)
            is WhatsAppToolAction.GetChatMessages -> mapListRecent(
                limit = action.limit,
                conversationName = action.conversationName,
                readMode = "chat"
            )
            is WhatsAppToolAction.DraftReply -> mapDraftReply(action)
            is WhatsAppToolAction.ReplyActiveNotification -> mapActiveNotificationReply(
                notificationKey = action.notificationKey,
                replyText = action.messageText
            )
            is WhatsAppToolAction.CreateFollowUp -> mapCreateFollowUp(action)
            is WhatsAppToolAction.ListPendingFollowUps -> mapListPendingFollowUps(action)
            is WhatsAppToolAction.GetActionableInbox -> mapGetActionableInbox(action)
            is WhatsAppToolAction.ScheduleReminder -> mapScheduleReminder(action)
            is WhatsAppToolAction.MarkMessagePriority -> mapMarkMessagePriority(action.messageEventId, action.priority)
            is WhatsAppToolAction.MarkImportant -> mapMarkMessagePriority(action.messageEventId, "HIGH")
            WhatsAppToolAction.PauseCapture -> mapNoArgumentTool(WhatsAppToolName.PauseWhatsAppCapture)
        }
    }

    fun mapReadLatestMessages(limit: Int): VoiceToolProposalResult {
        val definition = registry.require(WhatsAppToolName.ListRecentWhatsAppMessages.value)
        return mapToolProposal(
            ToolProposal(
                name = definition.name,
                arguments = mapOf("limit" to ToolArgument.IntegerValue(limit)),
                definition = definition
            )
        )
    }

    private fun mapRecentMessagesFromSender(
        senderName: String,
        limit: Int
    ): VoiceToolProposalResult {
        return mapListRecent(
            limit = limit,
            conversationName = senderName,
            readMode = "chat"
        )
    }

    private fun mapListRecent(
        limit: Int,
        conversationName: String? = null,
        readMode: String? = null
    ): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.ListRecentWhatsAppMessages.value)
            putJsonObject("parameters") {
                put("limit", limit)
                conversationName?.takeIf { it.isNotBlank() }?.let { put("conversation_name", it) }
                readMode?.takeIf { it.isNotBlank() }?.let { put("read_mode", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapSearchMessages(action: WhatsAppToolAction.SearchMessages): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.SearchWhatsAppMessages.value)
            putJsonObject("parameters") {
                put("query", action.query)
                action.conversationName?.takeIf { it.isNotBlank() }?.let { put("conversation_name", it) }
                action.sinceMinutes?.let { put("since_minutes", it) }
                action.priority?.takeIf { it.isNotBlank() }?.let { put("priority", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapDraftReply(action: WhatsAppToolAction.DraftReply): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.DraftWhatsAppReply.value)
            putJsonObject("parameters") {
                put("conversation_name", action.conversationName)
                put("message_text", action.messageText)
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapCreateFollowUp(action: WhatsAppToolAction.CreateFollowUp): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.CreateFollowUpFromMessage.value)
            putJsonObject("parameters") {
                put("message_event_id", action.messageEventId)
                put("follow_up_title", action.followUpTitle)
                action.dueAt?.takeIf { it.isNotBlank() }?.let { put("due_at", it) }
                action.priority?.takeIf { it.isNotBlank() }?.let { put("priority", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapListPendingFollowUps(action: WhatsAppToolAction.ListPendingFollowUps): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.ListPendingFollowUps.value)
            putJsonObject("parameters") {
                put("limit", action.limit)
                action.priority?.takeIf { it.isNotBlank() }?.let { put("priority", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapGetActionableInbox(action: WhatsAppToolAction.GetActionableInbox): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.GetActionableInbox.value)
            putJsonObject("parameters") {
                put("limit", action.limit)
                action.status?.takeIf { it.isNotBlank() }?.let { put("status", it) }
                action.priority?.takeIf { it.isNotBlank() }?.let { put("priority", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapScheduleReminder(action: WhatsAppToolAction.ScheduleReminder): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.ScheduleReminderForMessage.value)
            putJsonObject("parameters") {
                put("message_event_id", action.messageEventId)
                put("remind_at", action.remindAt)
                action.reminderNote?.takeIf { it.isNotBlank() }?.let { put("reminder_note", it) }
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapMarkMessagePriority(
        messageEventId: String,
        priority: String
    ): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.MarkMessagePriority.value)
            putJsonObject("parameters") {
                put("message_event_id", messageEventId)
                put("priority", priority)
            }
        }.toString()
        return mapJsonProposal(json)
    }

    private fun mapNoArgumentTool(name: WhatsAppToolName): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", name.value)
            putJsonObject("parameters") {}
        }.toString()
        return mapJsonProposal(json)
    }

    fun mapActiveNotificationReply(
        notificationKey: String,
        replyText: String
    ): VoiceToolProposalResult {
        val json = buildJsonObject {
            put("name", WhatsAppToolName.SendReplyToActiveWhatsAppNotification.value)
            putJsonObject("parameters") {
                put("notification_key", notificationKey)
                put("message_text", replyText)
            }
        }.toString()

        return mapJsonProposal(json)
    }

    fun mapParseResult(result: ToolCallParseResult): VoiceToolProposalResult {
        return when (result) {
            is ToolCallParseResult.Valid -> mapToolProposal(result.proposal)
            is ToolCallParseResult.Invalid -> VoiceToolProposalResult.Invalid(result.reason)
        }
    }

    fun mapToolProposal(proposal: ToolProposal): VoiceToolProposalResult {
        return when (val decision = safetyRouter.route(proposal)) {
            is ToolExecutionDecision.AllowLocalExecution -> VoiceToolProposalResult.Valid(proposal, decision)
            is ToolExecutionDecision.RequireUserConfirmation -> VoiceToolProposalResult.Valid(proposal, decision)
            is ToolExecutionDecision.Reject -> VoiceToolProposalResult.Invalid(decision.reason)
        }
    }

    private fun mapJsonProposal(json: String): VoiceToolProposalResult {
        return when (val result = parser.parse(json)) {
            is ToolCallParseResult.Valid -> mapToolProposal(result.proposal)
            is ToolCallParseResult.Invalid -> VoiceToolProposalResult.Invalid(result.reason)
        }
    }

    companion object {
        const val LatestActiveNotificationKey = "__latest_active_notification__"
    }
}

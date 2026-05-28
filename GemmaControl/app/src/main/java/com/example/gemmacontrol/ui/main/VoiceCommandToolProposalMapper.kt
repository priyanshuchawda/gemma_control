package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolArgument
import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.ToolCallParser
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolProposal
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
    fun mapReadLatestMessages(limit: Int): VoiceToolProposalResult {
        val definition = registry.require(WhatsAppToolName.ListRecentWhatsAppMessages.value)
        return route(
            ToolProposal(
                name = definition.name,
                arguments = mapOf("limit" to ToolArgument.IntegerValue(limit)),
                definition = definition
            )
        )
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

        return when (val result = parser.parse(json)) {
            is ToolCallParseResult.Valid -> route(result.proposal)
            is ToolCallParseResult.Invalid -> VoiceToolProposalResult.Invalid(result.reason)
        }
    }

    private fun route(proposal: ToolProposal): VoiceToolProposalResult {
        return when (val decision = safetyRouter.route(proposal)) {
            is ToolExecutionDecision.AllowLocalExecution -> VoiceToolProposalResult.Valid(proposal, decision)
            is ToolExecutionDecision.RequireUserConfirmation -> VoiceToolProposalResult.Valid(proposal, decision)
            is ToolExecutionDecision.Reject -> VoiceToolProposalResult.Invalid(decision.reason)
        }
    }
}

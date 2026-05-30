package com.example.gemmacontrol.ai.tools

enum class ToolExecutionScope {
    ReadOnlyLocalData,
    LocalDataWrite
}

enum class ToolConfirmationMode {
    Standard,
    StrictManual
}

data class ToolConfirmationRequirement(
    val mode: ToolConfirmationMode,
    val requiresActiveNotification: Boolean
)

sealed interface ToolExecutionDecision {
    data class AllowLocalExecution(
        val proposal: ToolProposal,
        val scope: ToolExecutionScope
    ) : ToolExecutionDecision

    data class RequireUserConfirmation(
        val proposal: ToolProposal,
        val requirement: ToolConfirmationRequirement
    ) : ToolExecutionDecision

    data class Reject(val reason: String) : ToolExecutionDecision
}

class ToolSafetyRouter {

    fun route(proposal: ToolProposal): ToolExecutionDecision {
        rejectMalformedHighRiskProposal(proposal)?.let { return it }

        return when (proposal.definition.safetyLevel) {
            ToolSafetyLevel.ReadOnly -> ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.ReadOnlyLocalData
            )
            ToolSafetyLevel.LocalWrite -> ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.LocalDataWrite
            )
            ToolSafetyLevel.ConfirmationRequired -> ToolExecutionDecision.RequireUserConfirmation(
                proposal = proposal,
                requirement = ToolConfirmationRequirement(
                    mode = ToolConfirmationMode.Standard,
                    requiresActiveNotification = false
                )
            )
            ToolSafetyLevel.StrictManualConfirmation -> ToolExecutionDecision.RequireUserConfirmation(
                proposal = proposal,
                requirement = ToolConfirmationRequirement(
                    mode = ToolConfirmationMode.StrictManual,
                    requiresActiveNotification = true
                )
            )
        }
    }

    private fun rejectMalformedHighRiskProposal(proposal: ToolProposal): ToolExecutionDecision.Reject? {
        if (proposal.name == WhatsAppToolName.DeleteLocalWhatsAppData && proposal.boolean("delete_all") != true) {
            return ToolExecutionDecision.Reject("Deletion proposals must explicitly set delete_all=true.")
        }

        if (
            proposal.name == WhatsAppToolName.SendReplyToActiveWhatsAppNotification &&
                proposal.string("notification_key").isNullOrBlank()
        ) {
            return ToolExecutionDecision.Reject("Active notification reply requires notification_key.")
        }

        return null
    }
}

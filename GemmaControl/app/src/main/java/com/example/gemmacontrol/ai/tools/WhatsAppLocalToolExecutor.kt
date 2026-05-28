package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository

interface LocalWhatsAppDataRepository {
    suspend fun deleteAllData()
}

sealed interface ToolExecutionResult {
    data class Success(val message: String) : ToolExecutionResult
    data class Rejected(val reason: String) : ToolExecutionResult
}

class WhatsAppLocalToolExecutor(
    private val preferencesRepository: CapturePreferencesRepository,
    private val localDataRepository: LocalWhatsAppDataRepository
) {

    suspend fun executeConfirmed(decision: ToolExecutionDecision): ToolExecutionResult {
        return when (decision) {
            is ToolExecutionDecision.Reject -> ToolExecutionResult.Rejected(decision.reason)
            is ToolExecutionDecision.AllowLocalExecution -> executeProposal(decision.proposal)
            is ToolExecutionDecision.RequireUserConfirmation -> executeProposal(decision.proposal)
        }
    }

    private suspend fun executeProposal(proposal: ToolProposal): ToolExecutionResult {
        return when (proposal.name) {
            WhatsAppToolName.PauseWhatsAppCapture -> {
                preferencesRepository.setCaptureEnabled(false)
                ToolExecutionResult.Success("WhatsApp capture paused.")
            }
            WhatsAppToolName.ResumeWhatsAppCapture -> {
                preferencesRepository.setCaptureEnabled(true)
                ToolExecutionResult.Success("WhatsApp capture resumed.")
            }
            WhatsAppToolName.DeleteLocalWhatsAppData -> deleteLocalWhatsAppData(proposal)
            WhatsAppToolName.SendReplyToActiveWhatsAppNotification -> ToolExecutionResult.Rejected(
                "Active notification replies must use the notification reply executor."
            )
            else -> ToolExecutionResult.Rejected("No local executor is implemented for ${proposal.name.value}.")
        }
    }

    private suspend fun deleteLocalWhatsAppData(proposal: ToolProposal): ToolExecutionResult {
        if (proposal.string("conversation_name") != null) {
            return ToolExecutionResult.Rejected("Conversation-scoped deletion is not implemented yet.")
        }
        if (proposal.boolean("delete_all") != true) {
            return ToolExecutionResult.Rejected("Deletion proposals must explicitly set delete_all=true.")
        }

        localDataRepository.deleteAllData()
        return ToolExecutionResult.Success("Local WhatsApp data deleted.")
    }
}

package com.example.gemmacontrol.ai

import kotlinx.coroutines.flow.StateFlow

sealed interface AssistantToolProposal {
    data class DraftReply(
        val notificationKey: String,
        val replyText: String
    ) : AssistantToolProposal
}

sealed interface ModelAvailability {
    data object Checking : ModelAvailability
    data object NotInstalled : ModelAvailability
    data object Initializing : ModelAvailability
    data object Ready : ModelAvailability
    data class Failed(val safeReason: String) : ModelAvailability
}

sealed interface ProposalGenerationResult {
    data class Success(val proposalText: String) : ProposalGenerationResult
    data object ModelNotInstalled : ProposalGenerationResult
    data class Failed(val safeReason: String) : ProposalGenerationResult
    data object InvalidOutput : ProposalGenerationResult
}

interface AssistantModelAdapter {
    val availability: StateFlow<ModelAvailability>

    suspend fun generateDraftReply(
        boundedContext: List<String>
    ): ProposalGenerationResult

    fun close()
}

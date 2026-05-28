package com.example.gemmacontrol.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GemmaModelAdapter : AssistantModelAdapter {

    private val _availability = MutableStateFlow<ModelAvailability>(ModelAvailability.NotInstalled)
    override val availability: StateFlow<ModelAvailability> = _availability.asStateFlow()

    override suspend fun generateDraftReply(
        boundedContext: List<String>
    ): ProposalGenerationResult {
        // Return NotInstalled truthfully since the runtime integration is blocked / missing model
        return ProposalGenerationResult.ModelNotInstalled
    }
}

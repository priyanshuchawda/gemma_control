package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry

data class GemmaEngineConfig(
    val modelPath: String,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val cacheDirectoryPath: String? = null
)

sealed interface GemmaEngineResult {
    data object Ready : GemmaEngineResult
    data class ProposalText(
        val rawText: String,
        val parseResult: ToolCallParseResult
    ) : GemmaEngineResult
    data class Blocked(val reason: String) : GemmaEngineResult
    data class Failure(val safeReason: String) : GemmaEngineResult
}

interface GemmaEngine {
    val isReady: Boolean

    suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult

    suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit = {}
    ): GemmaEngineResult

    fun cancelGeneration(): Boolean = false

    fun close()
}

class UnavailableGemmaEngine : GemmaEngine {
    override val isReady: Boolean = false

    override suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult {
        return GemmaEngineResult.Blocked(
            "LiteRT-LM runtime is not wired yet; cannot initialize FunctionGemma."
        )
    }

    override suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit
    ): GemmaEngineResult {
        return GemmaEngineResult.Blocked(BLOCKED_REASON)
    }

    override fun close() = Unit

    private companion object {
        const val BLOCKED_REASON = "LiteRT-LM runtime is not wired yet; cannot generate FunctionGemma proposals."
    }
}

const val DEFAULT_MAX_TOKENS = 1024
const val DEFAULT_TOP_K = 64
const val DEFAULT_TOP_P = 0.95f
const val DEFAULT_TEMPERATURE = 1.0f

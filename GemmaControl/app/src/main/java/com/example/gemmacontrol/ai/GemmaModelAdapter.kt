package com.example.gemmacontrol.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object FunctionGemmaModelConfig {
    const val MODEL_PATH = "/data/local/tmp/gemmacontrol_models/functiongemma.litertlm"
}

class GemmaModelAdapter : AssistantModelAdapter {

    private val _availability = MutableStateFlow<ModelAvailability>(ModelAvailability.Checking)
    override val availability: StateFlow<ModelAvailability> = _availability.asStateFlow()

    private var engine: Engine? = null

    init {
        checkModelAvailability()
    }

    private fun checkModelAvailability() {
        val file = File(FunctionGemmaModelConfig.MODEL_PATH)
        if (!file.exists()) {
            _availability.value = ModelAvailability.NotInstalled
        } else {
            _availability.value = ModelAvailability.Ready
        }
    }

    @Synchronized
    private fun getOrInitializeEngine(): Engine {
        val existing = engine
        if (existing != null) return existing

        val file = File(FunctionGemmaModelConfig.MODEL_PATH)
        if (!file.exists()) {
            throw IllegalStateException("FunctionGemma model file is not installed at ${FunctionGemmaModelConfig.MODEL_PATH}")
        }

        val config = EngineConfig(
            modelPath = FunctionGemmaModelConfig.MODEL_PATH,
            backend = Backend.CPU()
        )
        val newEngine = Engine(config)
        engine = newEngine
        _availability.value = ModelAvailability.Ready
        return newEngine
    }

    override suspend fun generateDraftReply(
        boundedContext: List<String>
    ): ProposalGenerationResult {
        val file = File(FunctionGemmaModelConfig.MODEL_PATH)
        if (!file.exists()) {
            _availability.value = ModelAvailability.NotInstalled
            return ProposalGenerationResult.ModelNotInstalled
        }

        try {
            val currentEngine = getOrInitializeEngine()

            // Define the tool using LiteRT-LM ToolSet Kotlin pattern
            var proposedReplyText: String? = null
            val draftReplyToolSet = object : ToolSet {
                @Tool(description = "Propose a short polite WhatsApp reply draft for the user to review and edit.")
                fun draftReply(
                    @ToolParam(description = "The proposed text draft for the reply.") replyText: String
                ): String {
                    proposedReplyText = replyText
                    return "Reply draft proposed successfully."
                }
            }

            val conversationConfig = ConversationConfig(
                tools = listOf(tool(draftReplyToolSet)),
                automaticToolCalling = false
            )

            val conversation = currentEngine.createConversation(conversationConfig)
            val inputMessage = boundedContext.lastOrNull() ?: ""
            val prompt = "Incoming message: $inputMessage\nPropose one short reply draft using the draftReply tool."

            // Send standard Message using the non-deprecated user factory method
            val response = conversation.sendMessage(Message.user(prompt))

            // If automaticToolCalling = false, check response.toolCalls
            val toolCalls = response.toolCalls
            if (toolCalls.isNotEmpty()) {
                val call = toolCalls.first()
                if (call.name == "draftReply") {
                    val replyTextArg = call.arguments["replyText"]?.toString()
                    if (!replyTextArg.isNullOrBlank()) {
                        return ProposalGenerationResult.Success(replyTextArg)
                    }
                }
            }

            // Fallback parsing from response text if model returns plain text instead of tool call
            val text = response.toString()
            if (text.isNotBlank()) {
                val cleanedText = text.take(500).trim()
                return ProposalGenerationResult.Success(cleanedText)
            }

            return ProposalGenerationResult.InvalidOutput

        } catch (e: Exception) {
            return ProposalGenerationResult.Failed(e.message ?: "Unknown inference error")
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }
}

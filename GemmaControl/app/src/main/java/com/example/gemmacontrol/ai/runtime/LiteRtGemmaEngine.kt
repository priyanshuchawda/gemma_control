package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.ToolCallParser
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteRtGemmaEngine : GemmaEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override val isReady: Boolean
        get() = engine?.isInitialized() == true && conversation?.isAlive == true

    @OptIn(ExperimentalApi::class)
    override suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult = withContext(Dispatchers.IO) {
        close()
        if (config.modelPath.isBlank()) {
            return@withContext GemmaEngineResult.Failure("FunctionGemma model path is required.")
        }

        val options = config.toLiteRtGemmaEngineOptions()
        try {
            val newEngine = Engine(
                EngineConfig(
                    modelPath = options.modelPath,
                    backend = Backend.GPU(),
                    visionBackend = null,
                    audioBackend = null,
                    maxNumTokens = options.maxTokens,
                    maxNumImages = null,
                    cacheDir = options.cacheDirectoryPath
                )
            )
            newEngine.initialize()

            val newConversation = newEngine.createConversation(
                ConversationConfig(
                    systemInstruction = null,
                    initialMessages = emptyList(),
                    tools = emptyList(),
                    samplerConfig = SamplerConfig(
                        topK = options.topK,
                        topP = options.topP.toDouble(),
                        temperature = options.temperature.toDouble()
                    ),
                    automaticToolCalling = options.automaticToolCalling
                )
            )

            engine = newEngine
            conversation = newConversation
            GemmaEngineResult.Ready
        } catch (e: Exception) {
            close()
            GemmaEngineResult.Failure("LiteRT-LM failed to initialize FunctionGemma.")
        }
    }

    override suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry
    ): GemmaEngineResult = withContext(Dispatchers.IO) {
        val activeConversation = conversation
            ?: return@withContext GemmaEngineResult.Blocked("FunctionGemma conversation is not initialized.")
        if (prompt.isBlank()) {
            return@withContext GemmaEngineResult.Failure("FunctionGemma prompt cannot be blank.")
        }

        val rawText = StringBuilder()
        val result = CompletableDeferred<GemmaEngineResult>()
        try {
            activeConversation.sendMessageAsync(
                Contents.of(prompt),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        rawText.append(message.toString())
                    }

                    override fun onDone() {
                        val finalText = rawText.toString().trim()
                        result.complete(
                            GemmaEngineResult.ProposalText(
                                rawText = finalText,
                                parseResult = ToolCallParser(registry).parse(finalText)
                            )
                        )
                    }

                    override fun onError(throwable: Throwable) {
                        result.complete(
                            GemmaEngineResult.Failure("LiteRT-LM failed while generating a FunctionGemma proposal.")
                        )
                    }
                }
            )
        } catch (e: Exception) {
            return@withContext GemmaEngineResult.Failure("LiteRT-LM failed while generating a FunctionGemma proposal.")
        }

        result.await()
    }

    override fun close() {
        try {
            conversation?.close()
        } finally {
            conversation = null
        }
        try {
            engine?.close()
        } finally {
            engine = null
        }
    }
}

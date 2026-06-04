package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.ToolCallParser
import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import com.example.gemmacontrol.ai.tools.WhatsAppTools
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
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteRtGemmaEngine(
    private val fallbackCacheDirectoryPath: String? = null
) : GemmaEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val nativeToolActions = mutableListOf<WhatsAppToolAction>()

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
                    backend = options.backend.toLiteRtBackend(),
                    visionBackend = null,
                    audioBackend = null,
                    maxNumTokens = options.maxTokens,
                    maxNumImages = null,
                    cacheDir = options.cacheDirectoryPath ?: fallbackCacheDirectoryPath
                )
            )
            newEngine.initialize()

            val newConversation = newEngine.createConversation(
                ConversationConfig(
                    systemInstruction = null,
                    initialMessages = emptyList(),
                    tools = listOf(
                        tool(
                            WhatsAppTools(
                                onFunctionCalled = { action ->
                                    synchronized(nativeToolActions) {
                                        nativeToolActions += action
                                    }
                                }
                            )
                        )
                    ),
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
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit
    ): GemmaEngineResult = withContext(Dispatchers.IO) {
        val activeConversation = conversation
            ?: return@withContext GemmaEngineResult.Blocked("FunctionGemma conversation is not initialized.")
        if (prompt.isBlank()) {
            return@withContext GemmaEngineResult.Failure("FunctionGemma prompt cannot be blank.")
        }

        val rawText = StringBuilder()
        val result = CompletableDeferred<GemmaEngineResult>()
        synchronized(nativeToolActions) {
            nativeToolActions.clear()
        }
        try {
            activeConversation.sendMessageAsync(
                Contents.of(prompt),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        rawText.append(message.toString())
                        onPartialText(rawText.toString())
                    }

                    override fun onDone() {
                        val nativeAction = synchronized(nativeToolActions) {
                            nativeToolActions.lastOrNull()
                        }
                        if (nativeAction != null) {
                            result.complete(GemmaEngineResult.NativeToolAction(nativeAction))
                            return
                        }
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

    override fun cancelGeneration(): Boolean {
        val activeConversation = conversation ?: return false
        activeConversation.cancelProcess()
        return true
    }

    override fun close() {
        try {
            conversation?.close()
        } finally {
            conversation = null
        }
        synchronized(nativeToolActions) {
            nativeToolActions.clear()
        }
        try {
            engine?.close()
        } finally {
            engine = null
        }
    }

    private fun String.toLiteRtBackend(): Backend {
        return when (this) {
            GemmaBackend.CPU.name -> Backend.CPU()
            GemmaBackend.GPU.name -> Backend.GPU()
            else -> Backend.GPU()
        }
    }
}

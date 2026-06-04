package com.example.gemmacontrol.ai.runtime

import android.util.Log
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

private const val TAG = "LiteRtGemmaEngine"

class LiteRtGemmaEngine(
    private val fallbackCacheDirectoryPath: String? = null
) : GemmaEngine {
    private var engine: Engine? = null
    private var engineOptions: LiteRtGemmaEngineOptions? = null
    private var activeConversation: Conversation? = null
    private val nativeToolActions = mutableListOf<WhatsAppToolAction>()

    override val isReady: Boolean
        get() = engine?.isInitialized() == true && engineOptions != null

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
            newEngine.createWhatsAppConversation(options).close()

            engine = newEngine
            engineOptions = options
            GemmaEngineResult.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FunctionGemma", e)
            close()
            GemmaEngineResult.Failure("LiteRT-LM failed to initialize FunctionGemma.")
        }
    }

    override suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit
    ): GemmaEngineResult = withContext(Dispatchers.IO) {
        val activeEngine = engine
            ?: return@withContext GemmaEngineResult.Blocked("FunctionGemma engine is not initialized.")
        val options = engineOptions
            ?: return@withContext GemmaEngineResult.Blocked("FunctionGemma engine is not initialized.")
        if (!activeEngine.isInitialized()) {
            return@withContext GemmaEngineResult.Blocked("FunctionGemma engine is not initialized.")
        }
        if (prompt.isBlank()) {
            return@withContext GemmaEngineResult.Failure("FunctionGemma prompt cannot be blank.")
        }

        val proposalConversation = try {
            activeEngine.createWhatsAppConversation(options)
        } catch (e: Exception) {
            Log.e(TAG, "FunctionGemma conversation creation failed", e)
            return@withContext GemmaEngineResult.Failure("LiteRT-LM failed while generating a FunctionGemma proposal.")
        }
        activeConversation = proposalConversation

        val rawText = StringBuilder()
        val result = CompletableDeferred<GemmaEngineResult>()
        synchronized(nativeToolActions) {
            nativeToolActions.clear()
        }
        try {
            proposalConversation.sendMessageAsync(
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
                        Log.e(TAG, "FunctionGemma generation failed", throwable)
                        result.complete(
                            GemmaEngineResult.Failure("LiteRT-LM failed while generating a FunctionGemma proposal.")
                        )
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "FunctionGemma sendMessageAsync failed", e)
            closeActiveConversation(proposalConversation)
            return@withContext GemmaEngineResult.Failure("LiteRT-LM failed while generating a FunctionGemma proposal.")
        }

        try {
            result.await()
        } finally {
            closeActiveConversation(proposalConversation)
        }
    }

    override fun cancelGeneration(): Boolean {
        val activeConversation = activeConversation ?: return false
        activeConversation.cancelProcess()
        return true
    }

    override fun close() {
        closeActiveConversation()
        synchronized(nativeToolActions) {
            nativeToolActions.clear()
        }
        try {
            engine?.close()
        } finally {
            engine = null
            engineOptions = null
        }
    }

    private fun closeActiveConversation(conversationToClose: Conversation? = activeConversation) {
        try {
            conversationToClose?.close()
        } finally {
            if (activeConversation === conversationToClose) {
                activeConversation = null
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun Engine.createWhatsAppConversation(options: LiteRtGemmaEngineOptions): Conversation {
        return createConversation(
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
    }

    private fun String.toLiteRtBackend(): Backend {
        return when (this) {
            GemmaBackend.CPU.name -> Backend.CPU()
            GemmaBackend.GPU.name -> Backend.GPU()
            else -> Backend.GPU()
        }
    }
}

package com.example.gemmacontrol.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.tool
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class GemmaModelAdapter(private val context: Context) : AssistantModelAdapter {

    private val _availability = MutableStateFlow<ModelAvailability>(ModelAvailability.Checking)
    override val availability: StateFlow<ModelAvailability> = _availability.asStateFlow()

    private var engine: Engine? = null

    private val modelFile: File
        get() = File(context.getExternalFilesDir("models"), "functiongemma.litertlm")

    init {
        checkModelAvailability()
    }

    private fun checkModelAvailability() {
        val file = modelFile
        if (!file.exists()) {
            _availability.value = ModelAvailability.NotInstalled
        } else {
            // Keep status as Checking or Initializing until successfully loaded
            _availability.value = ModelAvailability.Checking
        }
    }

    private val engineMutex = Mutex()

    private suspend fun getOrInitializeEngine(): Engine = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            val existing = engine
            if (existing != null) return@withLock existing

            val file = modelFile
            if (!file.exists()) {
                _availability.value = ModelAvailability.NotInstalled
                throw IllegalStateException("ModelNotInstalled")
            }

            _availability.value = ModelAvailability.Initializing
            try {
                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = Backend.CPU()
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine
                _availability.value = ModelAvailability.Ready
                newEngine
            } catch (e: Exception) {
                _availability.value = ModelAvailability.Failed("Model initialization failed.")
                throw e
            }
        }
    }

    override suspend fun generateDraftReply(
        boundedContext: List<String>
    ): ProposalGenerationResult {
        val file = modelFile
        if (!file.exists()) {
            _availability.value = ModelAvailability.NotInstalled
            return ProposalGenerationResult.ModelNotInstalled
        }

        try {
            val currentEngine = getOrInitializeEngine()

            // Define open api tool schema strictly as requested
            val draftReplyOpenApiTool = object : OpenApiTool {
                override fun getToolDescriptionJsonString(): String {
                    return """
                    {
                      "name": "draft_reply",
                      "description": "Propose one short editable WhatsApp reply draft. This tool does not send anything.",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "replyText": {
                            "type": "string",
                            "description": "A short polite reply draft for the user to review and edit."
                          }
                        },
                        "required": ["replyText"]
                      }
                    }
                    """.trimIndent()
                }

                override fun execute(paramsJsonString: String): String {
                    return ""
                }
            }

            val conversationConfig = ConversationConfig(
                tools = listOf(tool(draftReplyOpenApiTool)),
                automaticToolCalling = false
            )

            currentEngine.createConversation(conversationConfig).use { conversation ->
                val inputMessage = boundedContext.lastOrNull() ?: ""
                val prompt = "Incoming WhatsApp message:\n$inputMessage\n\nCall draft_reply with one short polite editable reply proposal.\nDo not send anything."

                // Send standard Message
                val response = conversation.sendMessage(Message.user(prompt))

                // Strict tool-output checking: exactly one draft_reply tool call
                val toolCalls = response.toolCalls
                if (toolCalls.size == 1) {
                    val call = toolCalls.first()
                    if (call.name == "draft_reply") {
                        val replyTextArg = call.arguments["replyText"]?.toString()
                        if (!replyTextArg.isNullOrBlank()) {
                            return ProposalGenerationResult.Success(replyTextArg)
                        }
                    }
                }

                return ProposalGenerationResult.InvalidOutput
            }

        } catch (e: Exception) {
            if (e is IllegalStateException && e.message == "ModelNotInstalled") {
                return ProposalGenerationResult.ModelNotInstalled
            }
            return ProposalGenerationResult.Failed("Local draft generation failed.")
        }
    }

    override fun close() {
        synchronized(this) {
            engine?.close()
            engine = null
            _availability.value = ModelAvailability.Checking
        }
    }
}

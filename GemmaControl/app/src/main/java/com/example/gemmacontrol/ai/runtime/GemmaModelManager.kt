package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface GemmaModelState {
    data object Released : GemmaModelState
    data class Initializing(val config: GemmaEngineConfig) : GemmaModelState
    data class Ready(val config: GemmaEngineConfig) : GemmaModelState
    data class Streaming(val partialText: String) : GemmaModelState
    data class Blocked(val reason: String) : GemmaModelState
    data class Failed(val safeReason: String) : GemmaModelState
}

class GemmaModelManager(
    private val engineFactory: () -> GemmaEngine = { UnavailableGemmaEngine() }
) {
    private val mutex = Mutex()
    private var engine: GemmaEngine = engineFactory()
    private var activeConfig: GemmaEngineConfig? = null

    private val _state = MutableStateFlow<GemmaModelState>(GemmaModelState.Released)
    val state: StateFlow<GemmaModelState> = _state.asStateFlow()

    val isReady: Boolean
        get() = engine.isReady

    suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult = mutex.withLock {
        if (engine.isReady && activeConfig == config) {
            return@withLock GemmaEngineResult.Ready
        }

        if (activeConfig != null) {
            closeCurrentEngine()
        }

        _state.value = GemmaModelState.Initializing(config)
        val result = engine.initialize(config)
        when (result) {
            GemmaEngineResult.Ready -> {
                activeConfig = config
                _state.value = GemmaModelState.Ready(config)
            }
            is GemmaEngineResult.Blocked -> {
                closeCurrentEngine()
                _state.value = GemmaModelState.Blocked(result.reason)
            }
            is GemmaEngineResult.Failure -> {
                closeCurrentEngine()
                _state.value = GemmaModelState.Failed(result.safeReason)
            }
            is GemmaEngineResult.ProposalText -> {
                closeCurrentEngine()
                _state.value = GemmaModelState.Failed("FunctionGemma initialization returned an invalid proposal result.")
                return@withLock GemmaEngineResult.Failure("FunctionGemma initialization returned an invalid proposal result.")
            }
        }
        result
    }

    suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit = {}
    ): GemmaEngineResult = mutex.withLock {
        if (!engine.isReady) {
            return@withLock GemmaEngineResult.Blocked(NOT_INITIALIZED_REASON)
        }

        val result = engine.generateToolProposal(prompt, registry) { partialText ->
            _state.value = GemmaModelState.Streaming(partialText)
            onPartialText(partialText)
        }
        when (result) {
            is GemmaEngineResult.ProposalText,
            GemmaEngineResult.Ready -> {
                activeConfig?.let { _state.value = GemmaModelState.Ready(it) }
            }
            is GemmaEngineResult.Blocked -> _state.value = GemmaModelState.Blocked(result.reason)
            is GemmaEngineResult.Failure -> _state.value = GemmaModelState.Failed(result.safeReason)
        }
        result
    }

    fun stopResponse(): Boolean {
        if (!engine.isReady) {
            return false
        }
        return engine.cancelGeneration()
    }

    @Suppress("UNUSED_PARAMETER")
    fun releaseForMemoryPressure(reason: String): Boolean {
        if (!engine.isReady && activeConfig == null) {
            _state.value = GemmaModelState.Released
            return false
        }

        closeCurrentEngine()
        _state.value = GemmaModelState.Released
        return true
    }

    fun release() {
        closeCurrentEngine()
        _state.value = GemmaModelState.Released
    }

    private fun closeCurrentEngine() {
        engine.close()
        engine = engineFactory()
        activeConfig = null
    }

    private companion object {
        const val NOT_INITIALIZED_REASON = "FunctionGemma model is not initialized."
    }
}

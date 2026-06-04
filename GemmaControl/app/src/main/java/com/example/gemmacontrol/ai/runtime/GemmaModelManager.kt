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
    private var engine: GemmaEngine? = null
    private var activeConfig: GemmaEngineConfig? = null

    private val _state = MutableStateFlow<GemmaModelState>(GemmaModelState.Released)
    val state: StateFlow<GemmaModelState> = _state.asStateFlow()

    val isReady: Boolean
        get() = engine?.isReady == true

    suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult = mutex.withLock {
        val currentEngine = engine
        if (currentEngine?.isReady == true && activeConfig == config) {
            return@withLock GemmaEngineResult.Ready
        }

        if (currentEngine != null) {
            closeCurrentEngine()
        }

        val newEngine = engineFactory()
        engine = newEngine
        _state.value = GemmaModelState.Initializing(config)
        val result = newEngine.initialize(config)
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
            is GemmaEngineResult.NativeToolAction -> {
                closeCurrentEngine()
                _state.value = GemmaModelState.Failed("FunctionGemma initialization returned an invalid tool action.")
                return@withLock GemmaEngineResult.Failure("FunctionGemma initialization returned an invalid tool action.")
            }
        }
        result
    }

    suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit = {}
    ): GemmaEngineResult = mutex.withLock {
        val currentEngine = engine
        if (currentEngine?.isReady != true) {
            return@withLock GemmaEngineResult.Blocked(NOT_INITIALIZED_REASON)
        }

        val result = currentEngine.generateToolProposal(prompt, registry) { partialText ->
            _state.value = GemmaModelState.Streaming(partialText)
            onPartialText(partialText)
        }
        when (result) {
            is GemmaEngineResult.ProposalText,
            is GemmaEngineResult.NativeToolAction,
            GemmaEngineResult.Ready -> {
                activeConfig?.let { _state.value = GemmaModelState.Ready(it) }
            }
            is GemmaEngineResult.Blocked -> _state.value = GemmaModelState.Blocked(result.reason)
            is GemmaEngineResult.Failure -> _state.value = GemmaModelState.Failed(result.safeReason)
        }
        result
    }

    fun stopResponse(): Boolean {
        val currentEngine = engine
        if (currentEngine?.isReady != true) {
            return false
        }
        return currentEngine.cancelGeneration()
    }

    fun releaseIfIdleForBackground(): Boolean {
        if (_state.value is GemmaModelState.Streaming) {
            return false
        }
        return releaseForMemoryPressure("APP_BACKGROUND")
    }

    @Suppress("UNUSED_PARAMETER")
    fun releaseForMemoryPressure(reason: String): Boolean {
        if (engine?.isReady != true && activeConfig == null) {
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
        engine?.close()
        engine = null
        activeConfig = null
    }

    private companion object {
        const val NOT_INITIALIZED_REASON = "FunctionGemma model is not initialized."
    }
}

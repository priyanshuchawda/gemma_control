package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import java.util.ArrayDeque
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaModelManagerTest {

    private val config = GemmaEngineConfig(modelPath = "/models/functiongemma.litertlm")
    private val registry = WhatsAppToolRegistry.default()

    @Test
    fun blocksGenerationBeforeInitialization() = runTest {
        val manager = GemmaModelManager(engineFactory = { FakeGemmaEngine() })

        val result = manager.generateToolProposal("reply ok", registry)

        assertEquals(GemmaEngineResult.Blocked("FunctionGemma model is not initialized."), result)
        assertEquals(GemmaModelState.Released, manager.state.value)
    }

    @Test
    fun initializesEngineOnceForSameConfig() = runTest {
        val engine = FakeGemmaEngine()
        val manager = GemmaModelManager(engineFactory = { engine })

        assertEquals(GemmaEngineResult.Ready, manager.initialize(config))
        assertEquals(GemmaEngineResult.Ready, manager.initialize(config))

        assertEquals(1, engine.initializeCalls)
        assertTrue(manager.isReady)
        assertEquals(GemmaModelState.Ready(config), manager.state.value)
    }

    @Test
    fun reinitializingDifferentConfigClosesPreviousEngine() = runTest {
        val first = FakeGemmaEngine()
        val second = FakeGemmaEngine()
        val engines = ArrayDeque(listOf(first, second))
        val manager = GemmaModelManager(engineFactory = { engines.removeFirst() })
        val nextConfig = config.copy(modelPath = "/models/functiongemma-new.litertlm")

        assertEquals(GemmaEngineResult.Ready, manager.initialize(config))
        assertEquals(GemmaEngineResult.Ready, manager.initialize(nextConfig))

        assertEquals(1, first.closeCalls)
        assertEquals(1, second.initializeCalls)
        assertEquals(GemmaModelState.Ready(nextConfig), manager.state.value)
    }

    @Test
    fun failedInitializationClosesEngineAndStoresSafeState() = runTest {
        val engine = FakeGemmaEngine(initializeResult = GemmaEngineResult.Failure("Invalid model file."))
        val manager = GemmaModelManager(engineFactory = { engine })

        val result = manager.initialize(config)

        assertEquals(GemmaEngineResult.Failure("Invalid model file."), result)
        assertEquals(1, engine.closeCalls)
        assertFalse(manager.isReady)
        assertEquals(GemmaModelState.Failed("Invalid model file."), manager.state.value)
    }

    @Test
    fun releaseForMemoryPressureClosesReadyEngine() = runTest {
        val engine = FakeGemmaEngine()
        val manager = GemmaModelManager(engineFactory = { engine })
        manager.initialize(config)

        val released = manager.releaseForMemoryPressure("TRIM_MEMORY_RUNNING_LOW")

        assertTrue(released)
        assertEquals(1, engine.closeCalls)
        assertFalse(manager.isReady)
        assertEquals(GemmaModelState.Released, manager.state.value)
    }

    @Test
    fun generateToolProposalPublishesStreamingStateAndForwardsPartialText() = runTest {
        val engine = FakeGemmaEngine(streamingPartials = listOf("Thinking", "Thinking done"))
        val manager = GemmaModelManager(engineFactory = { engine })
        manager.initialize(config)
        val observedPartials = mutableListOf<String>()
        val observedStates = mutableListOf<GemmaModelState>()

        val result = manager.generateToolProposal("reply ok", registry) { partialText ->
            observedPartials += partialText
            observedStates += manager.state.value
        }

        assertTrue(result is GemmaEngineResult.ProposalText)
        assertEquals(listOf("Thinking", "Thinking done"), observedPartials)
        assertEquals(
            listOf(
                GemmaModelState.Streaming("Thinking"),
                GemmaModelState.Streaming("Thinking done")
            ),
            observedStates
        )
        assertEquals(GemmaModelState.Ready(config), manager.state.value)
    }

    @Test
    fun stopResponseCancelsReadyEngineGeneration() = runTest {
        val engine = FakeGemmaEngine()
        val manager = GemmaModelManager(engineFactory = { engine })
        manager.initialize(config)

        assertTrue(manager.stopResponse())

        assertEquals(1, engine.cancelGenerationCalls)
    }

    private class FakeGemmaEngine(
        private val initializeResult: GemmaEngineResult = GemmaEngineResult.Ready,
        private val proposalResult: GemmaEngineResult = GemmaEngineResult.ProposalText(
            rawText = "{}",
            parseResult = ToolCallParseResult.Invalid("test")
        ),
        private val streamingPartials: List<String> = emptyList()
    ) : GemmaEngine {
        var initializeCalls = 0
            private set
        var closeCalls = 0
            private set
        var cancelGenerationCalls = 0
            private set
        override var isReady: Boolean = false
            private set

        override suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult {
            initializeCalls += 1
            if (initializeResult == GemmaEngineResult.Ready) {
                isReady = true
            }
            return initializeResult
        }

        override suspend fun generateToolProposal(
            prompt: String,
            registry: WhatsAppToolRegistry,
            onPartialText: (String) -> Unit
        ): GemmaEngineResult {
            streamingPartials.forEach(onPartialText)
            return proposalResult
        }

        override fun cancelGeneration(): Boolean {
            cancelGenerationCalls += 1
            return isReady
        }

        override fun close() {
            closeCalls += 1
            isReady = false
        }
    }
}

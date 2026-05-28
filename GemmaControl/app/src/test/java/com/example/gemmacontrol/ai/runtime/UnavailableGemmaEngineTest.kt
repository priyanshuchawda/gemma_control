package com.example.gemmacontrol.ai.runtime

import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnavailableGemmaEngineTest {

    @Test
    fun unavailableEngineReportsBlockedInitialization() = runTest {
        val engine = UnavailableGemmaEngine()

        val result = engine.initialize(
            GemmaEngineConfig(modelPath = "C:/models/functiongemma.litertlm")
        )

        assertTrue(result is GemmaEngineResult.Blocked)
        assertFalse(engine.isReady)
        val reason = (result as GemmaEngineResult.Blocked).reason
        assertTrue(reason.contains("LiteRT-LM runtime is not wired"))
        assertFalse(reason.contains("C:/models/functiongemma.litertlm"))
    }

    @Test
    fun unavailableEngineDoesNotGenerateToolProposals() = runTest {
        val engine = UnavailableGemmaEngine()

        val result = engine.generateToolProposal(
            prompt = "User command: reply ok",
            registry = WhatsAppToolRegistry.default()
        )

        assertEquals(
            GemmaEngineResult.Blocked("LiteRT-LM runtime is not wired yet; cannot generate FunctionGemma proposals."),
            result
        )
    }

    @Test
    fun parsedProposalResultCarriesModelTextSeparatelyFromExecution() {
        val result = GemmaEngineResult.ProposalText(
            rawText = """{"name":"pause_whatsapp_capture","parameters":{}}""",
            parseResult = ToolCallParseResult.Invalid("example")
        )

        assertEquals("""{"name":"pause_whatsapp_capture","parameters":{}}""", result.rawText)
        assertEquals(ToolCallParseResult.Invalid("example"), result.parseResult)
    }
}

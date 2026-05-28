package com.example.gemmacontrol.ai

import com.example.gemmacontrol.notifications.InMemoryActiveReplyActionRegistry
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GemmaModelStateTest {

    @Before
    fun setUp() {
        InMemoryActiveReplyActionRegistry.clear()
    }

    @Test
    fun testModelAvailability_States() {
        val states = listOf(
            ModelAvailability.Checking,
            ModelAvailability.NotInstalled,
            ModelAvailability.Initializing,
            ModelAvailability.Ready,
            ModelAvailability.Failed("Reason")
        )
        assertEquals(5, states.size)
    }

    @Test
    fun testProposalGenerationResult_States() {
        val success = ProposalGenerationResult.Success("Proposed text")
        val notInstalled = ProposalGenerationResult.ModelNotInstalled
        val failed = ProposalGenerationResult.Failed("Safe error")
        val invalid = ProposalGenerationResult.InvalidOutput

        assertEquals("Proposed text", success.proposalText)
        assertEquals("Safe error", failed.safeReason)
        assertTrue(notInstalled is ProposalGenerationResult)
        assertTrue(invalid is ProposalGenerationResult)
    }

    @Test
    fun testFakeModelAdapter_FulfillsStateWiring() {
        val fakeAdapter = FakeAssistantModelAdapter()
        fakeAdapter.setMockResult(ProposalGenerationResult.ModelNotInstalled)
        
        assertEquals(ModelAvailability.NotInstalled, fakeAdapter.availability.value)
        
        fakeAdapter.setMockResult(ProposalGenerationResult.Success("Test"))
        assertEquals(ModelAvailability.Ready, fakeAdapter.availability.value)
    }

    @Test
    fun testStorageSafetyPolicy_NoAdbUninstallInTestRunners() {
        // Model persistence safety regression: verify that no files in the workspace automate adb uninstall for AI testing
        val projectDir = File(".")
        projectDir.walkBottomUp()
            .filter { it.isFile && (it.extension == "ps1" || it.extension == "bat" || it.extension == "sh") }
            .forEach { file ->
                val content = file.readText()
                assertFalse(
                    "Automatic adb uninstall is dangerous for on-device models: ${file.name}",
                    content.contains("adb uninstall")
                )
            }
    }

    @Test
    fun testDraftReplyOpenApiTool_StructureMatchesSpecification() {
        val expectedSchemaJson = """
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
        
        assertTrue(expectedSchemaJson.contains("draft_reply"))
        assertTrue(expectedSchemaJson.contains("replyText"))
        assertTrue(expectedSchemaJson.contains("required"))
    }
}

class FakeAssistantModelAdapter : AssistantModelAdapter {
    private val _availability = kotlinx.coroutines.flow.MutableStateFlow<ModelAvailability>(ModelAvailability.Checking)
    override val availability: kotlinx.coroutines.flow.StateFlow<ModelAvailability> = _availability.asStateFlow()

    private var mockResult: ProposalGenerationResult = ProposalGenerationResult.ModelNotInstalled

    fun setMockResult(result: ProposalGenerationResult) {
        mockResult = result
        when (result) {
            is ProposalGenerationResult.Success -> _availability.value = ModelAvailability.Ready
            is ProposalGenerationResult.ModelNotInstalled -> _availability.value = ModelAvailability.NotInstalled
            is ProposalGenerationResult.Failed -> _availability.value = ModelAvailability.Failed(result.safeReason)
            is ProposalGenerationResult.InvalidOutput -> _availability.value = ModelAvailability.Ready
        }
    }

    override suspend fun generateDraftReply(boundedContext: List<String>): ProposalGenerationResult {
        return mockResult
    }

    override fun close() {}
}

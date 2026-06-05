package com.example.gemmacontrol.ai.safety

import com.example.gemmacontrol.ai.media.MediaUnderstandingPolicy
import com.example.gemmacontrol.ai.tools.ToolSafetyLevel
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import com.example.gemmacontrol.notifications.WhatsAppContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantSafetyPolicyTest {

    @Test
    fun everyRegisteredToolSafetyLevelMapsToDeterministicPolicy() {
        val registry = WhatsAppToolRegistry.default()

        registry.tools.forEach { tool ->
            val policy = AssistantSafetyPolicy.forToolSafetyLevel(tool.safetyLevel)

            assertTrue(policy.deterministicKotlinGate)
            assertEquals(SafetyModelGate.NotNeededForV1, policy.modelGate)
            assertFalse(policy.level == AssistantOperationSafetyLevel.MediaAnalysis)
        }
    }

    @Test
    fun toolSafetyLevelsMapToExpectedOperationLevels() {
        assertPolicy(ToolSafetyLevel.ReadOnly, AssistantOperationSafetyLevel.ReadOnly, confirm = false)
        assertPolicy(ToolSafetyLevel.LocalWrite, AssistantOperationSafetyLevel.LocalWrite, confirm = false)
        assertPolicy(
            ToolSafetyLevel.ConfirmationRequired,
            AssistantOperationSafetyLevel.ConfirmationRequired,
            confirm = true
        )
        assertPolicy(ToolSafetyLevel.OpenExternalApp, AssistantOperationSafetyLevel.OpenExternalApp, confirm = true)
        assertPolicy(ToolSafetyLevel.SendMessage, AssistantOperationSafetyLevel.SendMessage, confirm = true)
        assertPolicy(ToolSafetyLevel.DeleteData, AssistantOperationSafetyLevel.DeleteData, confirm = true)
    }

    @Test
    fun sendMessagePolicyRequiresActiveNotification() {
        val policy = AssistantSafetyPolicy.forToolSafetyLevel(ToolSafetyLevel.SendMessage)

        assertTrue(policy.requiresUserConfirmation)
        assertTrue(policy.requiresActiveNotification)
        assertEquals(AssistantOperationSafetyLevel.SendMessage, policy.level)
    }

    @Test
    fun selectedMediaAnalysisIsFutureShieldGemmaGate() {
        val mediaDecision = MediaUnderstandingPolicy.forUserSelectedFile(
            displayName = "test-photo.jpg",
            mimeType = "image/jpeg"
        )

        val policy = AssistantSafetyPolicy.forMediaUnderstanding(mediaDecision)

        assertEquals(AssistantOperationSafetyLevel.MediaAnalysis, policy.level)
        assertTrue(policy.requiresUserConfirmation)
        assertEquals(SafetyModelGate.FutureShieldGemma2ImageGate, policy.modelGate)
    }

    @Test
    fun placeholderMediaAnalysisIsRejectedWithoutSafetyModel() {
        val mediaDecision = MediaUnderstandingPolicy.fromNotificationContent(
            WhatsAppContentKind.PHOTO
        )

        val policy = AssistantSafetyPolicy.forMediaUnderstanding(mediaDecision)

        assertEquals(AssistantOperationSafetyLevel.Reject, policy.level)
        assertFalse(policy.requiresUserConfirmation)
        assertEquals(SafetyModelGate.NotNeededForV1, policy.modelGate)
    }

    @Test
    fun v1TextModerationDoesNotRequireShieldGemma() {
        assertEquals(SafetyModelGate.NotNeededForV1, AssistantSafetyPolicy.textModerationGateForV1())
    }

    private fun assertPolicy(
        toolSafetyLevel: ToolSafetyLevel,
        expectedLevel: AssistantOperationSafetyLevel,
        confirm: Boolean
    ) {
        val policy = AssistantSafetyPolicy.forToolSafetyLevel(toolSafetyLevel)

        assertEquals(expectedLevel, policy.level)
        assertEquals(confirm, policy.requiresUserConfirmation)
        assertTrue(policy.deterministicKotlinGate)
    }
}

package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCapabilityMatrixTest {

    private val registry = WhatsAppToolRegistry.default()
    private val matrix = AssistantCapabilityMatrix.default()

    @Test
    fun everyRegisteredToolHasCapabilityRequirements() {
        val uncovered = registry.tools
            .map { it.name }
            .filter { matrix.requirementsForTool(it).isEmpty() }

        assertEquals(emptyList<WhatsAppToolName>(), uncovered)
    }

    @Test
    fun adbIsDocumentedButNeverRequiredByProductionTools() {
        assertTrue(matrix.capability(AssistantCapabilitySource.AdbDevelopmentOnly).developmentOnly)
        val adbBackedTools = registry.tools
            .map { it.name }
            .filter { toolName ->
                matrix.requirementsForTool(toolName).any {
                    it.source == AssistantCapabilitySource.AdbDevelopmentOnly
                }
            }

        assertEquals(emptyList<WhatsAppToolName>(), adbBackedTools)
    }

    @Test
    fun accessibilityIsDocumentedButNeverRequiredByV1Tools() {
        val capability = matrix.capability(AssistantCapabilitySource.AccessibilityService)
        assertTrue(capability.permission.contains("V2 evaluation only"))
        val accessibilityBackedTools = registry.tools
            .map { it.name }
            .filter { toolName ->
                matrix.requirementsForTool(toolName).any {
                    it.source == AssistantCapabilitySource.AccessibilityService
                }
            }

        assertEquals(emptyList<WhatsAppToolName>(), accessibilityBackedTools)
    }

    @Test
    fun notificationReadToolExplainsMissingNotificationListenerSetup() {
        val state = AssistantCapabilityState.assumeReady().copy(notificationListenerEnabled = false)

        val missing = matrix.missingRequirements(
            toolName = WhatsAppToolName.ListRecentWhatsAppMessages,
            state = state
        )

        assertEquals(listOf(AssistantCapabilitySource.NotificationListener), missing.map { it.source })
        assertTrue(missing.single().setupResponse.contains("Notification Listener Access"))
    }

    @Test
    fun routerRejectsCapabilityBlockedToolBeforeSafetyRouting() {
        val parser = ToolCallParser(registry)
        val proposal = (parser.parse(
            """{"name":"schedule_reminder_for_message","parameters":{"message_event_id":"1","remind_at":"2026-06-05T09:00:00"}}"""
        ) as ToolCallParseResult.Valid).proposal
        val router = ToolSafetyRouter(
            capabilityState = AssistantCapabilityState.assumeReady().copy(postNotificationsGranted = false)
        )

        val decision = router.route(proposal)

        assertTrue(decision is ToolExecutionDecision.Reject)
        assertTrue((decision as ToolExecutionDecision.Reject).reason.contains("Reminder Notifications"))
    }

    @Test
    fun shareDraftRequiresWhatsAppIntentCapability() {
        val state = AssistantCapabilityState.assumeReady().copy(whatsappInstalled = false)

        val missing = matrix.missingRequirements(
            toolName = WhatsAppToolName.OpenWhatsAppShareDraft,
            state = state
        )

        assertFalse(missing.isEmpty())
        assertTrue(missing.any { it.source == AssistantCapabilitySource.WhatsAppIntent })
    }
}

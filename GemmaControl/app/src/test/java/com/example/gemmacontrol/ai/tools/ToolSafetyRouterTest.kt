package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSafetyRouterTest {

    private val parser = ToolCallParser(WhatsAppToolRegistry.default())
    private val router = ToolSafetyRouter()

    @Test
    fun readOnlyProposalCanRunAgainstLocalDataWithoutConfirmation() {
        val proposal = parseValid("""{"name":"list_recent_whatsapp_messages","parameters":{"limit":3}}""")

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.ReadOnlyLocalData
            ),
            decision
        )
    }

    @Test
    fun localWriteProposalRunsOnlyInsideLocalStorageBoundary() {
        val proposal = parseValid(
            """
            {
              "name": "create_follow_up_from_message",
              "parameters": {
                "message_event_id": "42",
                "follow_up_title": "Call back",
                "priority": "HIGH"
              }
            }
            """.trimIndent()
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.LocalDataWrite
            ),
            decision
        )
    }

    @Test
    fun activeNotificationReplyRequiresStrictManualConfirmationAndLiveNotification() {
        val proposal = parseValid(
            """
            {
              "name": "send_reply_to_active_whatsapp_notification",
              "parameters": {
                "notification_key": "active-key",
                "message_text": "I am in a meeting"
              }
            }
            """.trimIndent()
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.RequireUserConfirmation(
                proposal = proposal,
                requirement = ToolConfirmationRequirement(
                    mode = ToolConfirmationMode.StrictManual,
                    requiresActiveNotification = true
                )
            ),
            decision
        )
    }

    @Test
    fun externalAppOpenRequiresStandardConfirmationWithoutActiveNotification() {
        val proposal = parseValid(
            """
            {
              "name": "open_whatsapp_click_to_chat",
              "parameters": {
                "phone_number_e164": "+15551234567",
                "message_text": "Hello"
              }
            }
            """.trimIndent()
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.RequireUserConfirmation(
                proposal = proposal,
                requirement = ToolConfirmationRequirement(
                    mode = ToolConfirmationMode.Standard,
                    requiresActiveNotification = false
                )
            ),
            decision
        )
    }

    @Test
    fun deleteLocalDataRequiresStrictManualConfirmationWithoutActiveNotification() {
        val proposal = parseValid(
            """
            {
              "name": "delete_local_whatsapp_data",
              "parameters": {
                "delete_all": true
              }
            }
            """.trimIndent()
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.RequireUserConfirmation(
                proposal = proposal,
                requirement = ToolConfirmationRequirement(
                    mode = ToolConfirmationMode.StrictManual,
                    requiresActiveNotification = false
                )
            ),
            decision
        )
    }

    @Test
    fun deleteLocalDataRequiresExplicitTrueFlagAfterParsing() {
        val proposal = parseValid(
            """
            {
              "name": "delete_local_whatsapp_data",
              "parameters": {
                "delete_all": false
              }
            }
            """.trimIndent()
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.Reject("Deletion proposals must explicitly set delete_all=true."),
            decision
        )
    }

    @Test
    fun rejectsStrictReplyProposalWithoutNotificationKeyEvenIfConstructedDirectly() {
        val definition = WhatsAppToolRegistry.default()
            .require(WhatsAppToolName.SendReplyToActiveWhatsAppNotification.value)
        val proposal = ToolProposal(
            name = WhatsAppToolName.SendReplyToActiveWhatsAppNotification,
            arguments = mapOf("message_text" to ToolArgument.StringValue("ok")),
            definition = definition
        )

        val decision = router.route(proposal)

        assertEquals(
            ToolExecutionDecision.Reject("Active notification reply requires notification_key."),
            decision
        )
    }

    @Test
    fun rejectsActiveReplyWhenNotificationListenerCapabilityIsMissing() {
        val proposal = parseValid(
            """
            {
              "name": "send_reply_to_active_whatsapp_notification",
              "parameters": {
                "notification_key": "active-key",
                "message_text": "I am in a meeting"
              }
            }
            """.trimIndent()
        )
        val capabilityBlockedRouter = ToolSafetyRouter(
            capabilityState = AssistantCapabilityState.assumeReady().copy(notificationListenerEnabled = false)
        )

        val decision = capabilityBlockedRouter.route(proposal)

        assertTrue(decision is ToolExecutionDecision.Reject)
        assertEquals(
            "Enable Notification Listener Access so GemmaControl can read WhatsApp notifications and active reply actions.",
            (decision as ToolExecutionDecision.Reject).reason
        )
    }

    private fun parseValid(rawJson: String): ToolProposal {
        val result = parser.parse(rawJson)
        assertTrue(result is ToolCallParseResult.Valid)
        return (result as ToolCallParseResult.Valid).proposal
    }
}

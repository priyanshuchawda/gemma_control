package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolArgument
import com.example.gemmacontrol.ai.tools.ToolConfirmationMode
import com.example.gemmacontrol.ai.tools.ToolConfirmationRequirement
import com.example.gemmacontrol.ai.tools.ToolDefinition
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolExecutionScope
import com.example.gemmacontrol.ai.tools.ToolProposal
import com.example.gemmacontrol.ai.tools.ToolSafetyLevel
import com.example.gemmacontrol.ai.tools.WhatsAppToolName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallDetailsUiStateTest {
    @Test
    fun toolCallDetailsUiStateShowsExactToolNameSafetyBoundaryAndArguments() {
        val action = pendingAction(
            name = WhatsAppToolName.OpenWhatsAppClickToChat,
            safetyLevel = ToolSafetyLevel.ConfirmationRequired,
            arguments = mapOf(
                "phone_number_e164" to ToolArgument.StringValue("+15551234567"),
                "message_text" to ToolArgument.StringValue("Hello")
            ),
            decisionFactory = { proposal ->
                ToolExecutionDecision.RequireUserConfirmation(
                    proposal = proposal,
                    requirement = ToolConfirmationRequirement(
                        mode = ToolConfirmationMode.Standard,
                        requiresActiveNotification = false
                    )
                )
            }
        )

        val state = toolCallDetailsUiState(action)

        assertEquals("open_whatsapp_click_to_chat", state.toolName)
        assertEquals("Needs your confirmation", state.safetyLabel)
        assertEquals("Kotlin validates and runs this local action after you approve it.", state.boundaryLabel)
        assertEquals(
            listOf(
                ToolCallDetailRow("message_text", "Hello"),
                ToolCallDetailRow("phone_number_e164", "+15551234567")
            ),
            state.arguments
        )
    }

    @Test
    fun toolCallDetailsUiStateLabelsExecutionScope() {
        assertEquals(
            "Read-only local data",
            toolCallDetailsUiState(
                pendingAction(
                    name = WhatsAppToolName.SearchWhatsAppMessages,
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    decisionFactory = { proposal ->
                        ToolExecutionDecision.AllowLocalExecution(
                            proposal = proposal,
                            scope = ToolExecutionScope.ReadOnlyLocalData
                        )
                    }
                )
            ).safetyLabel
        )
        assertEquals(
            "Local app data write",
            toolCallDetailsUiState(
                pendingAction(
                    name = WhatsAppToolName.CreateFollowUpFromMessage,
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    decisionFactory = { proposal ->
                        ToolExecutionDecision.AllowLocalExecution(
                            proposal = proposal,
                            scope = ToolExecutionScope.LocalDataWrite
                        )
                    }
                )
            ).safetyLabel
        )
        assertEquals(
            "Strict manual confirmation",
            toolCallDetailsUiState(
                pendingAction(
                    name = WhatsAppToolName.DeleteLocalWhatsAppData,
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired,
                    decisionFactory = { proposal ->
                        ToolExecutionDecision.RequireUserConfirmation(
                            proposal = proposal,
                            requirement = ToolConfirmationRequirement(
                                mode = ToolConfirmationMode.StrictManual,
                                requiresActiveNotification = true
                            )
                        )
                    }
                )
            ).safetyLabel
        )
    }

    @Test
    fun toolCallDetailsUiStateFormatsArgumentsDeterministicallyAndBoundsLongValues() {
        val longText = "x".repeat(160)
        val action = pendingAction(
            name = WhatsAppToolName.GetActionableInbox,
            safetyLevel = ToolSafetyLevel.ReadOnly,
            arguments = mapOf(
                "limit" to ToolArgument.IntegerValue(10),
                "status" to ToolArgument.StringValue(longText),
                "include_completed" to ToolArgument.BooleanValue(false)
            )
        )

        val rows = toolCallDetailsUiState(action).arguments

        assertEquals("include_completed", rows[0].label)
        assertEquals("false", rows[0].value)
        assertEquals("limit", rows[1].label)
        assertEquals("10", rows[1].value)
        assertEquals("status", rows[2].label)
        assertTrue(rows[2].value.endsWith("..."))
        assertTrue(rows[2].value.length <= 99)
    }

    private fun pendingAction(
        name: WhatsAppToolName,
        safetyLevel: ToolSafetyLevel,
        arguments: Map<String, ToolArgument> = emptyMap(),
        decisionFactory: (ToolProposal) -> ToolExecutionDecision = { proposal ->
            ToolExecutionDecision.AllowLocalExecution(
                proposal = proposal,
                scope = ToolExecutionScope.ReadOnlyLocalData
            )
        }
    ): PendingLocalToolAction {
        val definition = ToolDefinition(
            name = name,
            description = "Test tool.",
            safetyLevel = safetyLevel
        )
        val proposal = ToolProposal(
            name = name,
            arguments = arguments,
            definition = definition
        )
        return PendingLocalToolAction(
            title = "Review action?",
            description = "Review action details.",
            confirmText = "Continue",
            proposal = proposal,
            decision = decisionFactory(proposal)
        )
    }
}

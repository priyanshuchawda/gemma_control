package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolArgument
import com.example.gemmacontrol.ai.tools.ToolConfirmationMode
import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolExecutionScope

data class ToolCallDetailsUiState(
    val toolName: String,
    val safetyLabel: String,
    val boundaryLabel: String,
    val arguments: List<ToolCallDetailRow>,
    val emptyArgumentsLabel: String = "No arguments"
)

data class ToolCallDetailRow(
    val label: String,
    val value: String
)

fun toolCallDetailsUiState(action: PendingLocalToolAction): ToolCallDetailsUiState {
    return ToolCallDetailsUiState(
        toolName = action.proposal.name.value,
        safetyLabel = action.decision.toSafetyLabel(),
        boundaryLabel = "Kotlin validates and runs this local action after you approve it.",
        arguments = action.proposal.arguments
            .toSortedMap()
            .map { (name, value) ->
                ToolCallDetailRow(
                    label = name,
                    value = value.toDisplayValue()
                )
            }
    )
}

private fun ToolExecutionDecision.toSafetyLabel(): String {
    return when (this) {
        is ToolExecutionDecision.AllowLocalExecution -> when (scope) {
            ToolExecutionScope.ReadOnlyLocalData -> "Read-only local data"
            ToolExecutionScope.LocalDataWrite -> "Local app data write"
        }
        is ToolExecutionDecision.RequireUserConfirmation -> when (requirement.mode) {
            ToolConfirmationMode.Standard -> "Needs your confirmation"
            ToolConfirmationMode.StrictManual -> "Strict manual confirmation"
        }
        is ToolExecutionDecision.Reject -> "Rejected"
    }
}

private fun ToolArgument.toDisplayValue(): String {
    val rawValue = when (this) {
        is ToolArgument.BooleanValue -> value.toString()
        is ToolArgument.IntegerValue -> value.toString()
        is ToolArgument.StringValue -> value.trim().ifBlank { "(blank)" }
    }
    return rawValue.toSingleLine().limitForConfirmation()
}

private fun String.toSingleLine(): String = replace(ToolArgumentWhitespaceRegex, " ").trim()

private fun String.limitForConfirmation(): String {
    return if (length <= MaxToolArgumentValueLength) {
        this
    } else {
        take(MaxToolArgumentValueLength) + "..."
    }
}

private const val MaxToolArgumentValueLength = 96
private val ToolArgumentWhitespaceRegex = Regex("\\s+")

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
        safetyLabel = safetyLabel(action.decision),
        boundaryLabel = "Kotlin validates and runs this local action after you approve it.",
        arguments = toolCallDetailRows(action.proposal.arguments)
    )
}

private fun toolCallDetailRows(arguments: Map<String, ToolArgument>): List<ToolCallDetailRow> {
    return arguments.toSortedMap().map { (name, value) ->
        ToolCallDetailRow(
            label = name,
            value = displayValue(value)
        )
    }
}

private fun safetyLabel(decision: ToolExecutionDecision): String {
    return when (decision) {
        is ToolExecutionDecision.AllowLocalExecution -> when (decision.scope) {
            ToolExecutionScope.ReadOnlyLocalData -> "Read-only local data"
            ToolExecutionScope.LocalDataWrite -> "Local app data write"
        }
        is ToolExecutionDecision.RequireUserConfirmation -> when (decision.requirement.mode) {
            ToolConfirmationMode.Standard -> "Needs your confirmation"
            ToolConfirmationMode.StrictManual -> "Strict manual confirmation"
        }
        is ToolExecutionDecision.Reject -> "Rejected"
    }
}

private fun displayValue(argument: ToolArgument): String {
    val rawValue = when (argument) {
        is ToolArgument.BooleanValue -> argument.value.toString()
        is ToolArgument.IntegerValue -> argument.value.toString()
        is ToolArgument.StringValue -> argument.value.trim().ifBlank { "(blank)" }
    }
    return limitForConfirmation(singleLine(rawValue))
}

private fun singleLine(value: String): String = value.replace(ToolArgumentWhitespaceRegex, " ").trim()

private fun limitForConfirmation(value: String): String {
    return if (value.length <= MaxToolArgumentValueLength) {
        value
    } else {
        value.take(MaxToolArgumentValueLength) + "..."
    }
}

private const val MaxToolArgumentValueLength = 96
private val ToolArgumentWhitespaceRegex = Regex("\\s+")

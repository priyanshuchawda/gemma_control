package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.LocalActionableInboxItem

data class ActionableInboxSectionState(
    val totalCount: Int,
    val pendingCount: Int,
    val highPriorityCount: Int,
    val subtitle: String,
    val emptyTitle: String,
    val emptySubtitle: String
) {
    val hasItems: Boolean = totalCount > 0
}

fun buildActionableInboxSectionState(
    items: List<LocalActionableInboxItem>
): ActionableInboxSectionState {
    val pendingCount = items.count { it.status.equals("PENDING", ignoreCase = true) }
    val highPriorityCount = items.count { it.priority.equals("HIGH", ignoreCase = true) }
    return ActionableInboxSectionState(
        totalCount = items.size,
        pendingCount = pendingCount,
        highPriorityCount = highPriorityCount,
        subtitle = when {
            items.isEmpty() -> "Ready when follow-ups exist"
            pendingCount == 1 -> "1 pending"
            else -> "$pendingCount pending"
        },
        emptyTitle = "No action items yet",
        emptySubtitle = "Ask FunctionGemma to create a follow-up or mark a message priority, then it will appear here."
    )
}

fun actionableInboxTypeLabel(type: String): String {
    return when (type.uppercase()) {
        "FOLLOW_UP" -> "Follow-up"
        "PRIORITY_MESSAGE" -> "Priority"
        else -> type.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }
}

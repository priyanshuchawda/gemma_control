package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.LocalActionableInboxItem
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class StoredInboxActionableUiStateTest {
    @Test
    fun buildActionableInboxSectionState_countsPendingAndHighPriorityItems() {
        val state = buildActionableInboxSectionState(
            listOf(
                actionableItem(type = "FOLLOW_UP", priority = "HIGH", status = "PENDING"),
                actionableItem(type = "PRIORITY_MESSAGE", priority = "NORMAL", status = "PENDING"),
                actionableItem(type = "FOLLOW_UP", priority = "HIGH", status = "COMPLETED")
            )
        )

        assertTrue(state.hasItems)
        assertEquals(3, state.totalCount)
        assertEquals(2, state.pendingCount)
        assertEquals(2, state.highPriorityCount)
        assertEquals("2 pending", state.subtitle)
    }

    @Test
    fun buildActionableInboxSectionState_hasGuidedEmptyState() {
        val state = buildActionableInboxSectionState(emptyList())

        assertFalse(state.hasItems)
        assertEquals("No action items yet", state.emptyTitle)
        assertEquals(
            "Ask FunctionGemma to create a follow-up or mark a message priority, then it will appear here.",
            state.emptySubtitle
        )
    }

    private fun actionableItem(
        type: String,
        priority: String,
        status: String
    ): LocalActionableInboxItem {
        return LocalActionableInboxItem(
            id = "$type-$priority-$status",
            messageEventId = "message-1",
            type = type,
            title = "Call back",
            conversationName = "Mom",
            text = "Call me tomorrow",
            priority = priority,
            status = status,
            dueAt = null,
            updatedAt = 1_716_900_000_000L
        )
    }
}

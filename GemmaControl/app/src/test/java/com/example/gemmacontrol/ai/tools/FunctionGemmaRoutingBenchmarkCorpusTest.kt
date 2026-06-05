package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.ui.main.AssistantPlan
import com.example.gemmacontrol.ui.main.AssistantPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionGemmaRoutingBenchmarkCorpusTest {

    private val registry = WhatsAppToolRegistry.default()

    @Test
    fun offlineCorpusCoversCoreRoutingCategories() {
        assertEquals(
            setOf(
                "read_latest",
                "summarize",
                "continue",
                "search",
                "reply_latest",
                "reply_named_chat",
                "multiple_active_chats",
                "follow_up",
                "reminder",
                "priority",
                "unsupported_voice_note",
                "unsupported_direct_history",
                "unsupported_hidden_media_contents"
            ),
            routingCorpus.map { it.category }.toSet()
        )
    }

    @Test
    fun enrichedToolDescriptionsCoverNaturalPhoneLanguage() {
        assertDescriptionHints(
            toolName = WhatsAppToolName.ListRecentWhatsAppMessages,
            "what did i miss",
            "latest",
            "summarize"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.SearchWhatsAppMessages,
            "find",
            "topic",
            "payment"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.DraftWhatsAppReply,
            "reply to named chat",
            "tell mom"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.GetActionableInbox,
            "anything urgent",
            "important"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.CreateFollowUpFromMessage,
            "follow up",
            "todo"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.ScheduleReminderForMessage,
            "remind me",
            "later"
        )
        assertDescriptionHints(
            toolName = WhatsAppToolName.MarkMessagePriority,
            "urgent",
            "important"
        )
    }

    @Test
    fun kotlinStateHandlesKnownUnsupportedWorkflowsBeforeModelExecution() {
        val planner = AssistantPlanner()

        assertTrue(planner.plan("send a voice note to Mom") is AssistantPlan.AskClarification)
        assertTrue(planner.plan("reply to the latest message:") is AssistantPlan.AskClarification)
        assertTrue(planner.plan("read my full WhatsApp history from last month") is AssistantPlan.RequestModelProposal)
        assertTrue(planner.plan("describe the photo someone sent me") is AssistantPlan.RequestModelProposal)
    }

    @Test
    fun baselineRecordsCurrentToolingBeforeNativeToolExpansion() {
        assertEquals(16, registry.tools.size)
        assertEquals(3, currentNativeLiteRtToolCallbackCount)
        assertEquals(0, currentOfflineModelAccuracyPercent)
        assertEquals("model execution deferred; no model download in this benchmark slice", baselineReason)
    }

    private fun assertDescriptionHints(
        toolName: WhatsAppToolName,
        vararg expectedHints: String
    ) {
        val description = registry.require(toolName.value).description.lowercase()
        expectedHints.forEach { hint ->
            assertTrue(
                "Expected ${toolName.value} description to contain '$hint'. Actual: $description",
                description.contains(hint)
            )
        }
    }

    private companion object {
        const val currentNativeLiteRtToolCallbackCount = 3
        const val currentOfflineModelAccuracyPercent = 0
        const val baselineReason = "model execution deferred; no model download in this benchmark slice"

        val routingCorpus = listOf(
            RoutingCorpusCase("read_latest", "read my latest WhatsApp messages"),
            RoutingCorpusCase("summarize", "what did I miss on WhatsApp"),
            RoutingCorpusCase("continue", "continue reading"),
            RoutingCorpusCase("search", "find the payment message from yesterday"),
            RoutingCorpusCase("reply_latest", "reply to the latest message: I am late"),
            RoutingCorpusCase("reply_named_chat", "tell Mom I am leaving now"),
            RoutingCorpusCase("multiple_active_chats", "which chats have unread messages"),
            RoutingCorpusCase("follow_up", "make a todo to call them back"),
            RoutingCorpusCase("reminder", "remind me later about this message"),
            RoutingCorpusCase("priority", "mark this as important"),
            RoutingCorpusCase("unsupported_voice_note", "send a voice note to Mom"),
            RoutingCorpusCase("unsupported_direct_history", "read my full WhatsApp history from last month"),
            RoutingCorpusCase("unsupported_hidden_media_contents", "describe the hidden photo someone sent me")
        )
    }
}

private data class RoutingCorpusCase(
    val category: String,
    val utterance: String
)

package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppToolRegistryTest {

    @Test
    fun registryMatchesDocumentedSixteenToolContract() {
        val registry = WhatsAppToolRegistry.default()

        assertEquals(16, registry.tools.size)
        assertEquals(
            listOf(
                "list_recent_whatsapp_messages",
                "search_whatsapp_messages",
                "get_whatsapp_message_details",
                "get_actionable_inbox",
                "create_follow_up_from_message",
                "list_pending_follow_ups",
                "mark_follow_up_completed",
                "schedule_reminder_for_message",
                "mark_message_priority",
                "draft_whatsapp_reply",
                "open_whatsapp_share_draft",
                "open_whatsapp_click_to_chat",
                "send_reply_to_active_whatsapp_notification",
                "pause_whatsapp_capture",
                "resume_whatsapp_capture",
                "delete_local_whatsapp_data"
            ),
            registry.tools.map { it.name.value }
        )
    }

    @Test
    fun registryMarksExternalAndPrivacyToolsAsRequiringConfirmation() {
        val registry = WhatsAppToolRegistry.default()

        assertEquals(ToolSafetyLevel.ReadOnly, registry.require("list_recent_whatsapp_messages").safetyLevel)
        assertEquals(ToolSafetyLevel.LocalWrite, registry.require("create_follow_up_from_message").safetyLevel)
        assertEquals(ToolSafetyLevel.ConfirmationRequired, registry.require("draft_whatsapp_reply").safetyLevel)
        assertEquals(ToolSafetyLevel.ConfirmationRequired, registry.require("open_whatsapp_click_to_chat").safetyLevel)
        assertEquals(ToolSafetyLevel.StrictManualConfirmation, registry.require("send_reply_to_active_whatsapp_notification").safetyLevel)
        assertEquals(ToolSafetyLevel.ConfirmationRequired, registry.require("delete_local_whatsapp_data").safetyLevel)
    }

    @Test
    fun systemPromptSummarisesToolNamesAndManualExecutionBoundary() {
        val prompt = WhatsAppToolRegistry.default().buildSystemPrompt(
            currentDateTimeIso = "2026-05-28T20:30:00",
            dayOfWeek = "Thursday"
        )

        assertTrue(prompt.contains("Use the native LiteRT-LM WhatsApp tools"))
        assertTrue(prompt.contains("send_reply_to_active_whatsapp_notification"))
        assertTrue(prompt.contains("Never claim that a reply was sent"))
        assertTrue(prompt.contains("Current date and time: 2026-05-28T20:30:00"))
    }
}

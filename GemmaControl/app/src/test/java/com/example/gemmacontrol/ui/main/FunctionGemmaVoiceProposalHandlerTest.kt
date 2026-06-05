package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.tools.ToolCallParser
import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionGemmaVoiceProposalHandlerTest {
    private val registry = WhatsAppToolRegistry.default()
    private val parser = ToolCallParser(registry)
    private val handler = FunctionGemmaVoiceProposalHandler()

    @Test
    fun mapsActiveNotificationReplyProposalToConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "send_reply_to_active_whatsapp_notification",
                params = mapOf(
                    "notification_key" to "active-key-1",
                    "message_text" to "On my way"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(
                activeNotificationKeys = setOf("active-key-1"),
                conversationTitleByNotificationKey = mapOf("active-key-1" to "Mom")
            )
        )

        assertTrue(state is VoiceAssistantState.ConfirmationRequired)
        val draft = (state as VoiceAssistantState.ConfirmationRequired).draft
        assertEquals("active-key-1", draft.notificationKey)
        assertEquals("On my way", draft.replyText)
        assertEquals("Mom", draft.conversationTitle)
    }

    @Test
    fun rejectsReplyProposalForExpiredNotificationKey() {
        val state = handler.resolve(
            result = proposalResult(
                name = "send_reply_to_active_whatsapp_notification",
                params = mapOf(
                    "notification_key" to "expired-key",
                    "message_text" to "On my way"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertEquals(
            VoiceAssistantState.Failure(
                "That WhatsApp reply target is no longer active. Ask the sender to message again, then retry while the notification is still visible."
            ),
            state
        )
    }

    @Test
    fun mapsReadLatestProposalToCommandReady() {
        val state = handler.resolve(
            result = proposalResult(
                name = "list_recent_whatsapp_messages",
                params = mapOf("limit" to 3)
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertEquals(VoiceAssistantState.CommandReady(VoiceCommand.ReadLatestMessages), state)
    }

    @Test
    fun mapsNativeReadLatestToolActionToCommandReady() {
        val state = handler.resolve(
            result = GemmaEngineResult.NativeToolAction(
                action = WhatsAppToolAction.ReadLatestNotifications(limit = 3)
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertEquals(VoiceAssistantState.CommandReady(VoiceCommand.ReadLatestMessages), state)
    }

    @Test
    fun mapsNativeLatestReplyToolActionToConfirmationForLatestActiveNotification() {
        val state = handler.resolve(
            result = GemmaEngineResult.NativeToolAction(
                action = WhatsAppToolAction.ReplyToLatestNotification("I am in a meeting")
            ),
            context = FunctionGemmaVoiceProposalContext(
                activeNotificationKeys = setOf("active-key-1"),
                conversationTitleByNotificationKey = mapOf("active-key-1" to "Mom")
            )
        )

        assertTrue(state is VoiceAssistantState.ConfirmationRequired)
        val draft = (state as VoiceAssistantState.ConfirmationRequired).draft
        assertEquals("active-key-1", draft.notificationKey)
        assertEquals("I am in a meeting", draft.replyText)
        assertEquals("Mom", draft.conversationTitle)
    }

    @Test
    fun nativeLatestReplyUsesPreferredLatestActiveNotificationKey() {
        val state = handler.resolve(
            result = GemmaEngineResult.NativeToolAction(
                action = WhatsAppToolAction.ReplyToLatestNotification("I am in a meeting")
            ),
            context = FunctionGemmaVoiceProposalContext(
                activeNotificationKeys = setOf("older-key", "newer-key"),
                latestActiveNotificationKey = "newer-key",
                conversationTitleByNotificationKey = mapOf(
                    "older-key" to "Dad",
                    "newer-key" to "Mom"
                )
            )
        )

        assertTrue(state is VoiceAssistantState.ConfirmationRequired)
        val draft = (state as VoiceAssistantState.ConfirmationRequired).draft
        assertEquals("newer-key", draft.notificationKey)
        assertEquals("Mom", draft.conversationTitle)
    }

    @Test
    fun mapsShareDraftProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "open_whatsapp_share_draft",
                params = mapOf("message_text" to "Hello")
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Open WhatsApp share draft?", action.title)
        assertEquals("Open WhatsApp", action.confirmText)
    }

    @Test
    fun mapsClickToChatDraftProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "open_whatsapp_click_to_chat",
                params = mapOf(
                    "phone_number_e164" to "+15551234567",
                    "message_text" to "Hello"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Open WhatsApp chat draft?", action.title)
        assertEquals("Open WhatsApp", action.confirmText)
    }

    @Test
    fun mapsDraftReplyProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "draft_whatsapp_reply",
                params = mapOf(
                    "conversation_name" to "Mom",
                    "message_text" to "On my way"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Prepare WhatsApp reply draft?", action.title)
        assertEquals("Prepare Draft", action.confirmText)
    }

    @Test
    fun mapsPauseCaptureProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "pause_whatsapp_capture",
                params = emptyMap()
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Pause WhatsApp capture?", action.title)
        assertEquals("pause_whatsapp_capture", action.proposal.name.value)
    }

    @Test
    fun mapsDeleteAllDataProposalToStrictLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "delete_local_whatsapp_data",
                params = mapOf("delete_all" to true)
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Delete all local WhatsApp data?", action.title)
        assertEquals("delete_local_whatsapp_data", action.proposal.name.value)
    }

    @Test
    fun mapsCreateFollowUpProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "create_follow_up_from_message",
                params = mapOf(
                    "message_event_id" to "message-1",
                    "follow_up_title" to "Call back",
                    "priority" to "HIGH"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Create follow-up?", action.title)
        assertEquals("Save Follow-Up", action.confirmText)
    }

    @Test
    fun mapsMarkPriorityProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "mark_message_priority",
                params = mapOf(
                    "message_event_id" to "message-1",
                    "priority" to "HIGH"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Mark message HIGH priority?", action.title)
        assertEquals("Mark Priority", action.confirmText)
    }

    @Test
    fun mapsScheduleReminderProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "schedule_reminder_for_message",
                params = mapOf(
                    "message_event_id" to "message-1",
                    "remind_at" to "2026-05-30T09:00:00+05:30",
                    "reminder_note" to "Call back"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Schedule reminder?", action.title)
        assertEquals("Schedule Reminder", action.confirmText)
    }

    @Test
    fun mapsListPendingFollowUpsProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "list_pending_follow_ups",
                params = mapOf(
                    "limit" to 5,
                    "priority" to "HIGH"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Show pending follow-ups?", action.title)
        assertEquals("Show Follow-Ups", action.confirmText)
    }

    @Test
    fun mapsSearchMessagesProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "search_whatsapp_messages",
                params = mapOf(
                    "query" to "dinner",
                    "conversation_name" to "Mom"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Search local WhatsApp messages?", action.title)
        assertEquals("Search Messages", action.confirmText)
    }

    @Test
    fun mapsMessageDetailsProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "get_whatsapp_message_details",
                params = mapOf("message_event_id" to "message-1")
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Show message details?", action.title)
        assertEquals("Show Details", action.confirmText)
    }

    @Test
    fun mapsActionableInboxProposalToLocalToolConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "get_actionable_inbox",
                params = mapOf(
                    "status" to "PENDING",
                    "priority" to "HIGH",
                    "limit" to 10
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Show actionable inbox?", action.title)
        assertEquals("Show Inbox", action.confirmText)
    }

    @Test
    fun mapsConversationScopedDeleteProposalToSpecificConfirmation() {
        val state = handler.resolve(
            result = proposalResult(
                name = "delete_local_whatsapp_data",
                params = mapOf(
                    "delete_all" to true,
                    "conversation_name" to "Mom"
                )
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.LocalToolConfirmationRequired)
        val action = (state as VoiceAssistantState.LocalToolConfirmationRequired).action
        assertEquals("Delete local data for Mom?", action.title)
    }

    @Test
    fun mapsInvalidModelOutputToSafeFailure() {
        val state = handler.resolve(
            result = GemmaEngineResult.ProposalText(
                rawText = "{}",
                parseResult = parser.parse("{}")
            ),
            context = FunctionGemmaVoiceProposalContext(activeNotificationKeys = emptySet())
        )

        assertTrue(state is VoiceAssistantState.Failure)
    }

    private fun proposalResult(
        name: String,
        params: Map<String, Any>
    ): GemmaEngineResult.ProposalText {
        val json = buildJsonObject {
            put("name", name)
            putJsonObject("parameters") {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Boolean -> put(key, value)
                    }
                }
            }
        }.toString()
        return GemmaEngineResult.ProposalText(
            rawText = json,
            parseResult = parser.parse(json)
        )
    }
}

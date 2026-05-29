package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppLocalToolExecutorTest {

    private val parser = ToolCallParser(WhatsAppToolRegistry.default())
    private val router = ToolSafetyRouter()

    @Test
    fun confirmedPauseCaptureTurnsCaptureOff() = runTest {
        val preferences = FakeCapturePreferencesRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = preferences,
            localDataRepository = FakeLocalWhatsAppDataRepository()
        )

        val result = executor.executeConfirmed(route("""{"name":"pause_whatsapp_capture","parameters":{}}"""))

        assertEquals(ToolExecutionResult.Success("WhatsApp capture paused."), result)
        assertFalse(preferences.captureEnabledFlow.value)
    }

    @Test
    fun confirmedResumeCaptureTurnsCaptureOn() = runTest {
        val preferences = FakeCapturePreferencesRepository().also {
            it.captureEnabledFlow.value = false
        }
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = preferences,
            localDataRepository = FakeLocalWhatsAppDataRepository()
        )

        val result = executor.executeConfirmed(route("""{"name":"resume_whatsapp_capture","parameters":{}}"""))

        assertEquals(ToolExecutionResult.Success("WhatsApp capture resumed."), result)
        assertTrue(preferences.captureEnabledFlow.value)
    }

    @Test
    fun confirmedDeleteAllPurgesLocalWhatsAppData() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route("""{"name":"delete_local_whatsapp_data","parameters":{"delete_all":true}}""")
        )

        assertEquals(ToolExecutionResult.Success("Local WhatsApp data deleted."), result)
        assertEquals(1, repository.deleteAllCalls)
    }

    @Test
    fun confirmedConversationScopedDeletePurgesMatchingConversation() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "delete_local_whatsapp_data",
                  "parameters": {
                    "delete_all": true,
                    "conversation_name": "Mom"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(
            ToolExecutionResult.Success("Local WhatsApp data deleted for Mom."),
            result
        )
        assertEquals(listOf("Mom"), repository.deleteConversationCalls)
        assertEquals(0, repository.deleteAllCalls)
    }

    @Test
    fun rejectsConversationScopedDeleteWhenNoConversationMatches() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(conversationDeleteResult = false)
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "delete_local_whatsapp_data",
                  "parameters": {
                    "delete_all": true,
                    "conversation_name": "Missing Chat"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(
            ToolExecutionResult.Rejected("No local WhatsApp data matched conversation_name."),
            result
        )
        assertEquals(listOf("Missing Chat"), repository.deleteConversationCalls)
    }

    @Test
    fun confirmedCreateFollowUpDelegatesToRepository() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "create_follow_up_from_message",
                  "parameters": {
                    "message_event_id": "message-1",
                    "follow_up_title": "Call back",
                    "due_at": "2026-05-30T09:00:00+05:30",
                    "priority": "HIGH"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("Follow-up saved: Call back."), result)
        assertEquals(
            listOf(
                CreateFollowUpCall(
                    messageEventId = "message-1",
                    title = "Call back",
                    dueAt = "2026-05-30T09:00:00+05:30",
                    priority = "HIGH"
                )
            ),
            repository.createFollowUpCalls
        )
    }

    @Test
    fun confirmedMarkMessagePriorityDelegatesToRepository() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "mark_message_priority",
                  "parameters": {
                    "message_event_id": "message-1",
                    "priority": "HIGH"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("Message marked HIGH priority."), result)
        assertEquals(listOf(MarkPriorityCall(messageEventId = "message-1", priority = "HIGH")), repository.markPriorityCalls)
    }

    @Test
    fun confirmedMarkFollowUpCompletedDelegatesToRepository() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "mark_follow_up_completed",
                  "parameters": {
                    "follow_up_id": "follow-up-1"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("Follow-up marked complete."), result)
        assertEquals(listOf("follow-up-1"), repository.completedFollowUpCalls)
    }

    @Test
    fun listPendingFollowUpsReturnsReadableSummary() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(
            pendingFollowUps = listOf(
                LocalFollowUp(
                    id = "follow-up-1",
                    messageEventId = "message-1",
                    title = "Call back",
                    dueAt = "2026-05-30T09:00:00+05:30",
                    priority = "HIGH",
                    createdAt = 1L,
                    completedAt = null
                )
            )
        )
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "list_pending_follow_ups",
                  "parameters": {
                    "limit": 10,
                    "priority": "HIGH"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(
            ToolExecutionResult.Success("- Call back [HIGH] due 2026-05-30T09:00:00+05:30"),
            result
        )
        assertEquals(
            listOf(ListPendingFollowUpsCall(limit = 10, priority = "HIGH")),
            repository.listPendingFollowUpsCalls
        )
    }

    @Test
    fun confirmedScheduleReminderDelegatesToRepository() = runTest {
        val repository = FakeLocalWhatsAppDataRepository()
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "schedule_reminder_for_message",
                  "parameters": {
                    "message_event_id": "message-1",
                    "remind_at": "2026-05-30T09:00:00+05:30",
                    "reminder_note": "Call back"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("Reminder scheduled."), result)
        assertEquals(
            listOf(
                ScheduleReminderCall(
                    messageEventId = "message-1",
                    remindAt = "2026-05-30T09:00:00+05:30",
                    reminderNote = "Call back"
                )
            ),
            repository.scheduleReminderCalls
        )
    }

    @Test
    fun searchWhatsAppMessagesReturnsReadableSummary() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(
            searchResults = listOf(
                LocalWhatsAppMessage(
                    id = "message-1",
                    conversationName = "Mom",
                    senderName = "Mom",
                    text = "Dinner at 7",
                    postedAt = 1000L,
                    priority = "NORMAL"
                )
            )
        )
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "search_whatsapp_messages",
                  "parameters": {
                    "query": "dinner",
                    "conversation_name": "Mom"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("- [message-1] Mom: Dinner at 7"), result)
        assertEquals(
            listOf(SearchMessagesCall(query = "dinner", conversationName = "Mom")),
            repository.searchMessagesCalls
        )
    }

    @Test
    fun listRecentWhatsAppMessagesReturnsReadableSummary() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(
            recentMessages = listOf(
                LocalWhatsAppMessage(
                    id = "message-1",
                    conversationName = "Mom",
                    senderName = "Mom",
                    text = "Dinner at 7",
                    postedAt = 1000L,
                    priority = "NORMAL"
                )
            )
        )
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "list_recent_whatsapp_messages",
                  "parameters": {
                    "conversation_name": "Mom",
                    "limit": 3,
                    "since_minutes": 15
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("- [message-1] Mom: Dinner at 7"), result)
        assertEquals(
            listOf(ListRecentMessagesCall(conversationName = "Mom", limit = 3, sinceMinutes = 15)),
            repository.listRecentMessagesCalls
        )
    }

    @Test
    fun getMessageDetailsReturnsReadableSummary() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(
            messageDetails = LocalWhatsAppMessage(
                id = "message-1",
                conversationName = "Mom",
                senderName = "Mom",
                text = "Dinner at 7",
                postedAt = 1000L,
                priority = "HIGH"
            )
        )
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "get_whatsapp_message_details",
                  "parameters": {
                    "message_event_id": "message-1"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(ToolExecutionResult.Success("- [message-1] Mom: Dinner at 7"), result)
        assertEquals(listOf("message-1"), repository.messageDetailsCalls)
    }

    @Test
    fun getActionableInboxReturnsReadableSummary() = runTest {
        val repository = FakeLocalWhatsAppDataRepository(
            actionableInbox = listOf(
                LocalActionableInboxItem(
                    id = "follow-up-1",
                    messageEventId = "message-1",
                    type = "FOLLOW_UP",
                    title = "Call back",
                    conversationName = "Mom",
                    text = "Call me tomorrow",
                    priority = "HIGH",
                    status = "PENDING",
                    dueAt = "2026-05-30T09:00:00+05:30",
                    updatedAt = 1000L
                ),
                LocalActionableInboxItem(
                    id = "message-2",
                    messageEventId = "message-2",
                    type = "PRIORITY_MESSAGE",
                    title = "High priority message",
                    conversationName = "Peter",
                    text = "Deadline moved earlier",
                    priority = "HIGH",
                    status = "PENDING",
                    dueAt = null,
                    updatedAt = 900L
                )
            )
        )
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = repository
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "get_actionable_inbox",
                  "parameters": {
                    "status": "PENDING",
                    "priority": "HIGH",
                    "limit": 10
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(
            ToolExecutionResult.Success(
                "- [FOLLOW_UP] Call back (Mom) [HIGH/PENDING] due 2026-05-30T09:00:00+05:30: Call me tomorrow\n" +
                    "- [PRIORITY_MESSAGE] High priority message (Peter) [HIGH/PENDING]: Deadline moved earlier"
            ),
            result
        )
        assertEquals(
            listOf(GetActionableInboxCall(status = "PENDING", priority = "HIGH", limit = 10)),
            repository.actionableInboxCalls
        )
    }

    @Test
    fun doesNotExecuteActiveNotificationRepliesInLocalExecutor() = runTest {
        val executor = WhatsAppLocalToolExecutor(
            preferencesRepository = FakeCapturePreferencesRepository(),
            localDataRepository = FakeLocalWhatsAppDataRepository()
        )

        val result = executor.executeConfirmed(
            route(
                """
                {
                  "name": "send_reply_to_active_whatsapp_notification",
                  "parameters": {
                    "notification_key": "active",
                    "message_text": "ok"
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(
            ToolExecutionResult.Rejected("Active notification replies must use the notification reply executor."),
            result
        )
    }

    private fun route(rawJson: String): ToolExecutionDecision {
        val parsed = parser.parse(rawJson)
        assertTrue(parsed is ToolCallParseResult.Valid)
        return router.route((parsed as ToolCallParseResult.Valid).proposal)
    }

    private class FakeCapturePreferencesRepository : CapturePreferencesRepository {
        override val captureEnabledFlow = MutableStateFlow(true)
        override val storageEnabledFlow = MutableStateFlow(false)
        override val storageEnabledAtFlow = MutableStateFlow(0L)
        override val xiaomiAutostartAcknowledgedFlow = MutableStateFlow(false)
        override val voiceInputModeFlow = MutableStateFlow(VoiceInputMode.TapToggle)

        override suspend fun setCaptureEnabled(enabled: Boolean) {
            captureEnabledFlow.value = enabled
        }

        override suspend fun setStorageEnabled(enabled: Boolean) {
            storageEnabledFlow.value = enabled
            storageEnabledAtFlow.value = if (enabled) 1L else 0L
        }

        override suspend fun setXiaomiAutostartAcknowledged(acknowledged: Boolean) {
            xiaomiAutostartAcknowledgedFlow.value = acknowledged
        }

        override suspend fun setVoiceInputMode(mode: VoiceInputMode) {
            voiceInputModeFlow.value = mode
        }
    }

    private data class CreateFollowUpCall(
        val messageEventId: String,
        val title: String,
        val dueAt: String?,
        val priority: String?
    )

    private data class MarkPriorityCall(
        val messageEventId: String,
        val priority: String
    )

    private data class ListPendingFollowUpsCall(
        val limit: Int,
        val priority: String?
    )

    private data class ScheduleReminderCall(
        val messageEventId: String,
        val remindAt: String,
        val reminderNote: String?
    )

    private data class SearchMessagesCall(
        val query: String,
        val conversationName: String?
    )

    private data class ListRecentMessagesCall(
        val conversationName: String?,
        val limit: Int,
        val sinceMinutes: Int?
    )

    private data class GetActionableInboxCall(
        val status: String?,
        val priority: String?,
        val limit: Int
    )

    private class FakeLocalWhatsAppDataRepository(
        private val conversationDeleteResult: Boolean = true,
        private val pendingFollowUps: List<LocalFollowUp> = emptyList(),
        private val recentMessages: List<LocalWhatsAppMessage> = emptyList(),
        private val searchResults: List<LocalWhatsAppMessage> = emptyList(),
        private val messageDetails: LocalWhatsAppMessage? = null,
        private val actionableInbox: List<LocalActionableInboxItem> = emptyList()
    ) : LocalWhatsAppDataRepository {
        var deleteAllCalls = 0
            private set
        val deleteConversationCalls = mutableListOf<String>()
        val createFollowUpCalls = mutableListOf<CreateFollowUpCall>()
        val markPriorityCalls = mutableListOf<MarkPriorityCall>()
        val completedFollowUpCalls = mutableListOf<String>()
        val listPendingFollowUpsCalls = mutableListOf<ListPendingFollowUpsCall>()
        val scheduleReminderCalls = mutableListOf<ScheduleReminderCall>()
        val listRecentMessagesCalls = mutableListOf<ListRecentMessagesCall>()
        val searchMessagesCalls = mutableListOf<SearchMessagesCall>()
        val messageDetailsCalls = mutableListOf<String>()
        val actionableInboxCalls = mutableListOf<GetActionableInboxCall>()

        override suspend fun deleteAllData() {
            deleteAllCalls += 1
        }

        override suspend fun deleteConversationData(conversationName: String): Boolean {
            deleteConversationCalls += conversationName
            return conversationDeleteResult
        }

        override suspend fun createFollowUp(
            messageEventId: String,
            title: String,
            dueAt: String?,
            priority: String?
        ): String? {
            createFollowUpCalls += CreateFollowUpCall(messageEventId, title, dueAt, priority)
            return "follow-up-1"
        }

        override suspend fun listPendingFollowUps(limit: Int, priority: String?): List<LocalFollowUp> {
            listPendingFollowUpsCalls += ListPendingFollowUpsCall(limit, priority)
            return pendingFollowUps
        }

        override suspend fun listRecentMessages(
            conversationName: String?,
            limit: Int,
            sinceMinutes: Int?
        ): List<LocalWhatsAppMessage> {
            listRecentMessagesCalls += ListRecentMessagesCall(conversationName, limit, sinceMinutes)
            return recentMessages
        }

        override suspend fun searchMessages(query: String, conversationName: String?): List<LocalWhatsAppMessage> {
            searchMessagesCalls += SearchMessagesCall(query, conversationName)
            return searchResults
        }

        override suspend fun getMessageDetails(messageEventId: String): LocalWhatsAppMessage? {
            messageDetailsCalls += messageEventId
            return messageDetails
        }

        override suspend fun getActionableInbox(
            status: String?,
            priority: String?,
            limit: Int
        ): List<LocalActionableInboxItem> {
            actionableInboxCalls += GetActionableInboxCall(status, priority, limit)
            return actionableInbox
        }

        override suspend fun markMessagePriority(messageEventId: String, priority: String): Boolean {
            markPriorityCalls += MarkPriorityCall(messageEventId, priority)
            return true
        }

        override suspend fun markFollowUpCompleted(followUpId: String): Boolean {
            completedFollowUpCalls += followUpId
            return true
        }

        override suspend fun scheduleReminder(
            messageEventId: String,
            remindAt: String,
            reminderNote: String?
        ): String? {
            scheduleReminderCalls += ScheduleReminderCall(messageEventId, remindAt, reminderNote)
            return "reminder-1"
        }
    }
}

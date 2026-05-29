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

    private class FakeLocalWhatsAppDataRepository(
        private val conversationDeleteResult: Boolean = true
    ) : LocalWhatsAppDataRepository {
        var deleteAllCalls = 0
            private set
        val deleteConversationCalls = mutableListOf<String>()

        override suspend fun deleteAllData() {
            deleteAllCalls += 1
        }

        override suspend fun deleteConversationData(conversationName: String): Boolean {
            deleteConversationCalls += conversationName
            return conversationDeleteResult
        }
    }
}

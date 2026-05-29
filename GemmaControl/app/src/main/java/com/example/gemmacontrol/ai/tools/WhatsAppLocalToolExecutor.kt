package com.example.gemmacontrol.ai.tools

import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository

interface LocalWhatsAppDataRepository {
    suspend fun deleteAllData()
    suspend fun deleteConversationData(conversationName: String): Boolean
    suspend fun createFollowUp(
        messageEventId: String,
        title: String,
        dueAt: String?,
        priority: String?
    ): String?
    suspend fun listPendingFollowUps(limit: Int, priority: String?): List<LocalFollowUp>
    suspend fun markFollowUpCompleted(followUpId: String): Boolean
    suspend fun markMessagePriority(messageEventId: String, priority: String): Boolean
    suspend fun scheduleReminder(
        messageEventId: String,
        remindAt: String,
        reminderNote: String?
    ): String?
    suspend fun searchMessages(query: String, conversationName: String?): List<LocalWhatsAppMessage>
}

data class LocalFollowUp(
    val id: String,
    val messageEventId: String,
    val title: String,
    val dueAt: String?,
    val priority: String,
    val createdAt: Long,
    val completedAt: Long?
)

data class LocalWhatsAppMessage(
    val id: String,
    val conversationName: String,
    val senderName: String?,
    val text: String?,
    val postedAt: Long,
    val priority: String
)

sealed interface ToolExecutionResult {
    data class Success(val message: String) : ToolExecutionResult
    data class Rejected(val reason: String) : ToolExecutionResult
}

class WhatsAppLocalToolExecutor(
    private val preferencesRepository: CapturePreferencesRepository,
    private val localDataRepository: LocalWhatsAppDataRepository
) {

    suspend fun executeConfirmed(decision: ToolExecutionDecision): ToolExecutionResult {
        return when (decision) {
            is ToolExecutionDecision.Reject -> ToolExecutionResult.Rejected(decision.reason)
            is ToolExecutionDecision.AllowLocalExecution -> executeProposal(decision.proposal)
            is ToolExecutionDecision.RequireUserConfirmation -> executeProposal(decision.proposal)
        }
    }

    private suspend fun executeProposal(proposal: ToolProposal): ToolExecutionResult {
        return when (proposal.name) {
            WhatsAppToolName.PauseWhatsAppCapture -> {
                preferencesRepository.setCaptureEnabled(false)
                ToolExecutionResult.Success("WhatsApp capture paused.")
            }
            WhatsAppToolName.ResumeWhatsAppCapture -> {
                preferencesRepository.setCaptureEnabled(true)
                ToolExecutionResult.Success("WhatsApp capture resumed.")
            }
            WhatsAppToolName.CreateFollowUpFromMessage -> createFollowUp(proposal)
            WhatsAppToolName.ListPendingFollowUps -> listPendingFollowUps(proposal)
            WhatsAppToolName.MarkFollowUpCompleted -> markFollowUpCompleted(proposal)
            WhatsAppToolName.ScheduleReminderForMessage -> scheduleReminder(proposal)
            WhatsAppToolName.MarkMessagePriority -> markMessagePriority(proposal)
            WhatsAppToolName.SearchWhatsAppMessages -> searchMessages(proposal)
            WhatsAppToolName.DeleteLocalWhatsAppData -> deleteLocalWhatsAppData(proposal)
            WhatsAppToolName.SendReplyToActiveWhatsAppNotification -> ToolExecutionResult.Rejected(
                "Active notification replies must use the notification reply executor."
            )
            else -> ToolExecutionResult.Rejected("No local executor is implemented for ${proposal.name.value}.")
        }
    }

    private suspend fun deleteLocalWhatsAppData(proposal: ToolProposal): ToolExecutionResult {
        if (proposal.boolean("delete_all") != true) {
            return ToolExecutionResult.Rejected("Deletion proposals must explicitly set delete_all=true.")
        }

        val conversationName = proposal.string("conversation_name")?.trim()
        if (!conversationName.isNullOrBlank()) {
            val deleted = localDataRepository.deleteConversationData(conversationName)
            return if (deleted) {
                ToolExecutionResult.Success("Local WhatsApp data deleted for $conversationName.")
            } else {
                ToolExecutionResult.Rejected("No local WhatsApp data matched conversation_name.")
            }
        }

        localDataRepository.deleteAllData()
        return ToolExecutionResult.Success("Local WhatsApp data deleted.")
    }

    private suspend fun createFollowUp(proposal: ToolProposal): ToolExecutionResult {
        val messageEventId = proposal.string("message_event_id")?.trim().orEmpty()
        val title = proposal.string("follow_up_title")?.trim().orEmpty()
        if (messageEventId.isBlank()) {
            return ToolExecutionResult.Rejected("Follow-up proposals require message_event_id.")
        }
        if (title.isBlank()) {
            return ToolExecutionResult.Rejected("Follow-up title must not be blank.")
        }

        val followUpId = localDataRepository.createFollowUp(
            messageEventId = messageEventId,
            title = title,
            dueAt = proposal.string("due_at")?.trim()?.takeIf { it.isNotBlank() },
            priority = proposal.string("priority")?.trim()?.takeIf { it.isNotBlank() }
        )
        return if (followUpId != null) {
            ToolExecutionResult.Success("Follow-up saved: $title.")
        } else {
            ToolExecutionResult.Rejected("No local WhatsApp message matched message_event_id.")
        }
    }

    private suspend fun markFollowUpCompleted(proposal: ToolProposal): ToolExecutionResult {
        val followUpId = proposal.string("follow_up_id")?.trim().orEmpty()
        if (followUpId.isBlank()) {
            return ToolExecutionResult.Rejected("Follow-up completion requires follow_up_id.")
        }

        return if (localDataRepository.markFollowUpCompleted(followUpId)) {
            ToolExecutionResult.Success("Follow-up marked complete.")
        } else {
            ToolExecutionResult.Rejected("No pending follow-up matched follow_up_id.")
        }
    }

    private suspend fun listPendingFollowUps(proposal: ToolProposal): ToolExecutionResult {
        val limit = proposal.integer("limit") ?: 10
        val followUps = localDataRepository.listPendingFollowUps(
            limit = limit,
            priority = proposal.string("priority")?.trim()?.takeIf { it.isNotBlank() }
        )
        if (followUps.isEmpty()) {
            return ToolExecutionResult.Success("No pending follow-ups.")
        }

        val summary = followUps.joinToString(separator = "\n") { followUp ->
            val due = followUp.dueAt?.let { " due $it" }.orEmpty()
            "- ${followUp.title} [${followUp.priority}]$due"
        }
        return ToolExecutionResult.Success(summary)
    }

    private suspend fun scheduleReminder(proposal: ToolProposal): ToolExecutionResult {
        val messageEventId = proposal.string("message_event_id")?.trim().orEmpty()
        val remindAt = proposal.string("remind_at")?.trim().orEmpty()
        if (messageEventId.isBlank()) {
            return ToolExecutionResult.Rejected("Reminder proposals require message_event_id.")
        }
        if (remindAt.isBlank()) {
            return ToolExecutionResult.Rejected("Reminder proposals require remind_at.")
        }

        val reminderId = localDataRepository.scheduleReminder(
            messageEventId = messageEventId,
            remindAt = remindAt,
            reminderNote = proposal.string("reminder_note")?.trim()?.takeIf { it.isNotBlank() }
        )
        return if (reminderId != null) {
            ToolExecutionResult.Success("Reminder scheduled.")
        } else {
            ToolExecutionResult.Rejected("Reminder could not be scheduled for that message and time.")
        }
    }

    private suspend fun markMessagePriority(proposal: ToolProposal): ToolExecutionResult {
        val messageEventId = proposal.string("message_event_id")?.trim().orEmpty()
        val priority = proposal.string("priority")?.trim().orEmpty()
        if (messageEventId.isBlank()) {
            return ToolExecutionResult.Rejected("Priority proposals require message_event_id.")
        }
        if (priority !in setOf("HIGH", "NORMAL")) {
            return ToolExecutionResult.Rejected("Message priority must be HIGH or NORMAL.")
        }

        return if (localDataRepository.markMessagePriority(messageEventId, priority)) {
            ToolExecutionResult.Success("Message marked $priority priority.")
        } else {
            ToolExecutionResult.Rejected("No local WhatsApp message matched message_event_id.")
        }
    }

    private suspend fun searchMessages(proposal: ToolProposal): ToolExecutionResult {
        val query = proposal.string("query")?.trim().orEmpty()
        if (query.isBlank()) {
            return ToolExecutionResult.Rejected("Search proposals require query.")
        }
        val messages = localDataRepository.searchMessages(
            query = query,
            conversationName = proposal.string("conversation_name")?.trim()?.takeIf { it.isNotBlank() }
        )
        if (messages.isEmpty()) {
            return ToolExecutionResult.Success("No matching WhatsApp messages.")
        }
        return ToolExecutionResult.Success(formatMessages(messages))
    }

    private fun formatMessages(messages: List<LocalWhatsAppMessage>): String {
        return messages.joinToString(separator = "\n") { message ->
            val text = message.text?.takeIf { it.isNotBlank() } ?: "[No message text]"
            "- [${message.id}] ${message.conversationName}: $text"
        }
    }
}

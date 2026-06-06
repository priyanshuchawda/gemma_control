package com.example.gemmacontrol.ai.tools

internal const val DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT = 3

sealed interface WhatsAppToolAction {
    data class ReplyToLatestNotification(val replyText: String) : WhatsAppToolAction
    data class ReadLatestNotifications(
        val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
    ) : WhatsAppToolAction

    data class GetNotificationFrom(
        val senderName: String,
        val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
    ) : WhatsAppToolAction

    data class ListUnreadChats(val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT) : WhatsAppToolAction

    data class ReadMessages(
        val conversationName: String?,
        val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
    ) : WhatsAppToolAction

    data class SummarizeMessages(
        val conversationName: String? = null,
        val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
    ) : WhatsAppToolAction

    data class SearchMessages(
        val query: String,
        val conversationName: String?,
        val sinceMinutes: Int? = null,
        val priority: String? = null
    ) : WhatsAppToolAction

    data class GetChatMessages(
        val conversationName: String,
        val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
    ) : WhatsAppToolAction

    data class DraftReply(
        val conversationName: String,
        val messageText: String
    ) : WhatsAppToolAction

    data class ReplyActiveNotification(
        val notificationKey: String,
        val messageText: String
    ) : WhatsAppToolAction

    data class CreateFollowUp(
        val messageEventId: String,
        val followUpTitle: String,
        val dueAt: String? = null,
        val priority: String? = null
    ) : WhatsAppToolAction

    data class ListPendingFollowUps(
        val limit: Int = 10,
        val priority: String? = null
    ) : WhatsAppToolAction

    data class GetActionableInbox(
        val status: String? = "PENDING",
        val priority: String? = null,
        val limit: Int = 10
    ) : WhatsAppToolAction

    data class ScheduleReminder(
        val messageEventId: String,
        val remindAt: String,
        val reminderNote: String? = null
    ) : WhatsAppToolAction

    data class MarkMessagePriority(
        val messageEventId: String,
        val priority: String
    ) : WhatsAppToolAction

    data class MarkImportant(val messageEventId: String) : WhatsAppToolAction

    data object PauseCapture : WhatsAppToolAction
}

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

    data class SummarizeMessages(val limit: Int = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT) : WhatsAppToolAction

    data class SearchMessages(
        val query: String,
        val conversationName: String?
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
        val followUpTitle: String
    ) : WhatsAppToolAction

    data class MarkImportant(val messageEventId: String) : WhatsAppToolAction

    data object PauseCapture : WhatsAppToolAction
}

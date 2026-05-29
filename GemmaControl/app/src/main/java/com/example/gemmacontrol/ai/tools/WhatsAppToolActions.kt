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
}

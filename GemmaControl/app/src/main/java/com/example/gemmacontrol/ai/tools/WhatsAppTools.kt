package com.example.gemmacontrol.ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

class WhatsAppTools(
    onFunctionCalled: (WhatsAppToolAction) -> Unit
) : ToolSet {
    private val handler = WhatsAppToolActionHandler(onFunctionCalled)

    @Tool(description = "Prepare a reply to the latest active WhatsApp notification for user confirmation.")
    fun replyToLatestNotification(
        @ToolParam(description = "The reply text to send after explicit user confirmation.")
        replyText: String
    ): Map<String, String> {
        return handler.replyToLatestNotification(replyText)
    }

    @Tool(description = "Read the latest captured WhatsApp notifications from local storage.")
    fun readLatestNotifications(): Map<String, String> {
        return handler.readLatestNotifications()
    }

    @Tool(description = "Find recent captured WhatsApp notifications from a sender or group.")
    fun getNotificationFrom(
        @ToolParam(description = "The sender or group display name to search for.")
        senderName: String
    ): Map<String, String> {
        return handler.getNotificationFrom(senderName)
    }
}

package com.example.gemmacontrol.ai.tools

private const val MAX_REPLY_TEXT_CHARS = 1000

class WhatsAppToolActionHandler(
    private val onFunctionCalled: (WhatsAppToolAction) -> Unit
) {
    fun replyToLatestNotification(replyText: String): Map<String, String> {
        val cleanedReply = replyText.trim()
        if (cleanedReply.isEmpty()) {
            return errorResult("Reply text cannot be empty.")
        }
        if (cleanedReply.length > MAX_REPLY_TEXT_CHARS) {
            return errorResult("Reply text is too long (maximum $MAX_REPLY_TEXT_CHARS characters).")
        }

        onFunctionCalled(WhatsAppToolAction.ReplyToLatestNotification(cleanedReply))
        return successResult(
            action = "reply_to_latest_notification",
            values = mapOf("reply_text" to cleanedReply)
        )
    }

    fun readLatestNotifications(): Map<String, String> {
        val limit = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
        onFunctionCalled(WhatsAppToolAction.ReadLatestNotifications(limit = limit))
        return successResult(
            action = "read_latest_notifications",
            values = mapOf("limit" to limit.toString())
        )
    }

    fun getNotificationFrom(senderName: String): Map<String, String> {
        val cleanedSender = senderName.trim()
        if (cleanedSender.isEmpty()) {
            return errorResult("Sender name cannot be empty.")
        }

        val limit = DEFAULT_WHATSAPP_TOOL_NOTIFICATION_LIMIT
        onFunctionCalled(
            WhatsAppToolAction.GetNotificationFrom(
                senderName = cleanedSender,
                limit = limit
            )
        )
        return successResult(
            action = "get_notification_from",
            values = mapOf(
                "sender_name" to cleanedSender,
                "limit" to limit.toString()
            )
        )
    }

    private fun successResult(
        action: String,
        values: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return buildMap {
            put("result", "success")
            put("action", action)
            putAll(values)
        }
    }

    private fun errorResult(message: String): Map<String, String> {
        return mapOf(
            "result" to "error",
            "message" to message
        )
    }
}

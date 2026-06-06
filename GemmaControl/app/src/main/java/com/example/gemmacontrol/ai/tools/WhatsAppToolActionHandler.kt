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

    fun listUnreadChats(limit: Int): Map<String, String> {
        val safeLimit = safeLimit(limit)
        onFunctionCalled(WhatsAppToolAction.ListUnreadChats(limit = safeLimit))
        return successResult(
            action = "list_unread_chats",
            values = mapOf("limit" to safeLimit.toString())
        )
    }

    fun readMessages(conversationName: String, limit: Int): Map<String, String> {
        val cleanedConversation = conversationName.trim().ifBlank { null }
        val safeLimit = safeLimit(limit)
        onFunctionCalled(
            WhatsAppToolAction.ReadMessages(
                conversationName = cleanedConversation,
                limit = safeLimit
            )
        )
        return successResult(
            action = "read_messages",
            values = buildMap {
                put("limit", safeLimit.toString())
                cleanedConversation?.let { put("conversation_name", it) }
            }
        )
    }

    fun summarizeMessages(limit: Int): Map<String, String> {
        return summarizeMessages(conversationName = "", limit = limit)
    }

    fun summarizeMessages(conversationName: String, limit: Int): Map<String, String> {
        val cleanedConversation = conversationName.trim().ifBlank { null }
        val safeLimit = safeLimit(limit)
        onFunctionCalled(
            WhatsAppToolAction.SummarizeMessages(
                conversationName = cleanedConversation,
                limit = safeLimit
            )
        )
        return successResult(
            action = "summarize_messages",
            values = buildMap {
                put("limit", safeLimit.toString())
                cleanedConversation?.let { put("conversation_name", it) }
            }
        )
    }

    fun searchMessages(query: String, conversationName: String): Map<String, String> {
        val cleanedQuery = query.trim()
        if (cleanedQuery.isEmpty()) {
            return errorResult("Query cannot be empty.")
        }
        val cleanedConversation = conversationName.trim().ifBlank { null }
        onFunctionCalled(
            WhatsAppToolAction.SearchMessages(
                query = cleanedQuery,
                conversationName = cleanedConversation
            )
        )
        return successResult(
            action = "search_messages",
            values = buildMap {
                put("query", cleanedQuery)
                cleanedConversation?.let { put("conversation_name", it) }
            }
        )
    }

    fun getChatMessages(conversationName: String, limit: Int): Map<String, String> {
        val cleanedConversation = conversationName.trim()
        if (cleanedConversation.isEmpty()) {
            return errorResult("Conversation name cannot be empty.")
        }
        val safeLimit = safeLimit(limit)
        onFunctionCalled(
            WhatsAppToolAction.GetChatMessages(
                conversationName = cleanedConversation,
                limit = safeLimit
            )
        )
        return successResult(
            action = "get_chat_messages",
            values = mapOf(
                "conversation_name" to cleanedConversation,
                "limit" to safeLimit.toString()
            )
        )
    }

    fun draftReply(conversationName: String, messageText: String): Map<String, String> {
        val cleanedConversation = conversationName.trim()
        if (cleanedConversation.isEmpty()) {
            return errorResult("Conversation name cannot be empty.")
        }
        val cleanedMessage = messageText.trim()
        if (cleanedMessage.isEmpty()) {
            return errorResult("Message text cannot be empty.")
        }
        if (cleanedMessage.length > MAX_REPLY_TEXT_CHARS) {
            return errorResult("Message text is too long (maximum $MAX_REPLY_TEXT_CHARS characters).")
        }
        onFunctionCalled(
            WhatsAppToolAction.DraftReply(
                conversationName = cleanedConversation,
                messageText = cleanedMessage
            )
        )
        return successResult(
            action = "draft_reply",
            values = mapOf(
                "conversation_name" to cleanedConversation,
                "message_text" to cleanedMessage
            )
        )
    }

    fun replyActiveNotification(notificationKey: String, messageText: String): Map<String, String> {
        val cleanedKey = notificationKey.trim()
        if (cleanedKey.isEmpty()) {
            return errorResult("Notification key cannot be empty.")
        }
        val cleanedMessage = messageText.trim()
        if (cleanedMessage.isEmpty()) {
            return errorResult("Message text cannot be empty.")
        }
        if (cleanedMessage.length > MAX_REPLY_TEXT_CHARS) {
            return errorResult("Message text is too long (maximum $MAX_REPLY_TEXT_CHARS characters).")
        }
        onFunctionCalled(
            WhatsAppToolAction.ReplyActiveNotification(
                notificationKey = cleanedKey,
                messageText = cleanedMessage
            )
        )
        return successResult(
            action = "reply_active_notification",
            values = mapOf(
                "notification_key" to cleanedKey,
                "message_text" to cleanedMessage
            )
        )
    }

    fun createFollowUp(messageEventId: String, followUpTitle: String): Map<String, String> {
        val cleanedMessageEventId = messageEventId.trim()
        if (cleanedMessageEventId.isEmpty()) {
            return errorResult("Message event id cannot be empty.")
        }
        val cleanedTitle = followUpTitle.trim()
        if (cleanedTitle.isEmpty()) {
            return errorResult("Follow-up title cannot be empty.")
        }
        onFunctionCalled(
            WhatsAppToolAction.CreateFollowUp(
                messageEventId = cleanedMessageEventId,
                followUpTitle = cleanedTitle
            )
        )
        return successResult(
            action = "create_followup",
            values = mapOf(
                "message_event_id" to cleanedMessageEventId,
                "follow_up_title" to cleanedTitle
            )
        )
    }

    fun markImportant(messageEventId: String): Map<String, String> {
        val cleanedMessageEventId = messageEventId.trim()
        if (cleanedMessageEventId.isEmpty()) {
            return errorResult("Message event id cannot be empty.")
        }
        onFunctionCalled(WhatsAppToolAction.MarkImportant(cleanedMessageEventId))
        return successResult(
            action = "mark_important",
            values = mapOf("message_event_id" to cleanedMessageEventId)
        )
    }

    fun pauseCapture(): Map<String, String> {
        onFunctionCalled(WhatsAppToolAction.PauseCapture)
        return successResult(action = "pause_capture")
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

    private fun safeLimit(limit: Int): Int = limit.coerceIn(1, 100)
}

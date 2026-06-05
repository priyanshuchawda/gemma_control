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

    @Tool(description = "List active or recently captured WhatsApp chats with unread context.")
    fun listUnreadChats(
        @ToolParam(description = "Maximum number of chats to inspect.")
        limit: Int
    ): Map<String, String> {
        return handler.listUnreadChats(limit)
    }

    @Tool(description = "Read recent captured WhatsApp messages, optionally filtered to a sender or group.")
    fun readMessages(
        @ToolParam(description = "Optional sender or group display name. Pass an empty string for all chats.")
        conversationName: String,
        @ToolParam(description = "Maximum number of messages to inspect.")
        limit: Int
    ): Map<String, String> {
        return handler.readMessages(conversationName, limit)
    }

    @Tool(description = "Summarize recently captured WhatsApp messages from local notification context.")
    fun summarizeMessages(
        @ToolParam(description = "Maximum number of recent messages to summarize.")
        limit: Int
    ): Map<String, String> {
        return handler.summarizeMessages(limit)
    }

    @Tool(description = "Search locally captured WhatsApp notification messages by keyword or topic.")
    fun searchMessages(
        @ToolParam(description = "Keyword or topic to search for.")
        query: String,
        @ToolParam(description = "Optional sender or group display name. Pass an empty string for all chats.")
        conversationName: String
    ): Map<String, String> {
        return handler.searchMessages(query, conversationName)
    }

    @Tool(description = "Get recent captured WhatsApp messages from one sender or group.")
    fun getChatMessages(
        @ToolParam(description = "Sender or group display name.")
        conversationName: String,
        @ToolParam(description = "Maximum number of messages to inspect.")
        limit: Int
    ): Map<String, String> {
        return handler.getChatMessages(conversationName, limit)
    }

    @Tool(description = "Prepare a WhatsApp reply draft for a named chat without sending.")
    fun draftReply(
        @ToolParam(description = "Target sender or group display name.")
        conversationName: String,
        @ToolParam(description = "Draft message text.")
        messageText: String
    ): Map<String, String> {
        return handler.draftReply(conversationName, messageText)
    }

    @Tool(description = "Prepare a reply to a specific active WhatsApp notification for strict user confirmation.")
    fun replyActiveNotification(
        @ToolParam(description = "Active Android notification key.")
        notificationKey: String,
        @ToolParam(description = "Reply text to send after explicit user confirmation.")
        messageText: String
    ): Map<String, String> {
        return handler.replyActiveNotification(notificationKey, messageText)
    }

    @Tool(description = "Create a local follow-up task from a captured WhatsApp message.")
    fun createFollowUp(
        @ToolParam(description = "Stored message event id.")
        messageEventId: String,
        @ToolParam(description = "Short follow-up title.")
        followUpTitle: String
    ): Map<String, String> {
        return handler.createFollowUp(messageEventId, followUpTitle)
    }

    @Tool(description = "Mark a captured WhatsApp message as important in local app storage.")
    fun markImportant(
        @ToolParam(description = "Stored message event id.")
        messageEventId: String
    ): Map<String, String> {
        return handler.markImportant(messageEventId)
    }

    @Tool(description = "Pause future WhatsApp notification capture after app/user confirmation.")
    fun pauseCapture(): Map<String, String> {
        return handler.pauseCapture()
    }
}

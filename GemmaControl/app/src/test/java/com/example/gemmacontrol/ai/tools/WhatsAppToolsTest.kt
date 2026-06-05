package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppToolsTest {

    @Test
    fun replyToLatestNotificationCapturesReplyActionAndReturnsModelSafeResult() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        val result = tools.replyToLatestNotification("  I'm in a meeting  ")

        assertEquals(
            WhatsAppToolAction.ReplyToLatestNotification("I'm in a meeting"),
            actions.single()
        )
        assertEquals("success", result["result"])
        assertEquals("reply_to_latest_notification", result["action"])
        assertEquals("I'm in a meeting", result["reply_text"])
    }

    @Test
    fun replyToLatestNotificationRejectsBlankReplyWithoutCapturingAction() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        val result = tools.replyToLatestNotification("   ")

        assertTrue(actions.isEmpty())
        assertEquals("error", result["result"])
        assertEquals("Reply text cannot be empty.", result["message"])
    }

    @Test
    fun readLatestNotificationsCapturesReadAction() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        val result = tools.readLatestNotifications()

        assertEquals(WhatsAppToolAction.ReadLatestNotifications(limit = 3), actions.single())
        assertEquals("success", result["result"])
        assertEquals("read_latest_notifications", result["action"])
        assertEquals("3", result["limit"])
    }

    @Test
    fun getNotificationFromNormalizesSenderName() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        val result = tools.getNotificationFrom("  Mom  ")

        assertEquals(WhatsAppToolAction.GetNotificationFrom(senderName = "Mom", limit = 3), actions.single())
        assertEquals("success", result["result"])
        assertEquals("get_notification_from", result["action"])
        assertEquals("Mom", result["sender_name"])
    }

    @Test
    fun expandedNativeToolsCaptureStructuredActions() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        assertEquals("success", tools.listUnreadChats(limit = 5)["result"])
        assertEquals("success", tools.readMessages(conversationName = " Mom ", limit = 4)["result"])
        assertEquals("success", tools.summarizeMessages(limit = 8)["result"])
        assertEquals("success", tools.searchMessages(query = " payment ", conversationName = " Office ")["result"])
        assertEquals("success", tools.getChatMessages(conversationName = " Office ", limit = 6)["result"])
        assertEquals("success", tools.draftReply(conversationName = " Mom ", messageText = " On my way ")["result"])
        assertEquals("success", tools.replyActiveNotification(notificationKey = " active-key ", messageText = " Ok ")["result"])
        assertEquals("success", tools.createFollowUp(messageEventId = " msg-1 ", followUpTitle = " Call back ")["result"])
        assertEquals("success", tools.markImportant(messageEventId = " msg-1 ")["result"])
        assertEquals("success", tools.pauseCapture()["result"])

        assertEquals(
            listOf(
                WhatsAppToolAction.ListUnreadChats(limit = 5),
                WhatsAppToolAction.ReadMessages(conversationName = "Mom", limit = 4),
                WhatsAppToolAction.SummarizeMessages(limit = 8),
                WhatsAppToolAction.SearchMessages(query = "payment", conversationName = "Office"),
                WhatsAppToolAction.GetChatMessages(conversationName = "Office", limit = 6),
                WhatsAppToolAction.DraftReply(conversationName = "Mom", messageText = "On my way"),
                WhatsAppToolAction.ReplyActiveNotification(notificationKey = "active-key", messageText = "Ok"),
                WhatsAppToolAction.CreateFollowUp(messageEventId = "msg-1", followUpTitle = "Call back"),
                WhatsAppToolAction.MarkImportant(messageEventId = "msg-1"),
                WhatsAppToolAction.PauseCapture
            ),
            actions
        )
    }

    @Test
    fun expandedNativeToolsRejectBlankRequiredArguments() {
        val actions = mutableListOf<WhatsAppToolAction>()
        val tools = WhatsAppToolActionHandler(onFunctionCalled = actions::add)

        assertEquals("error", tools.searchMessages(query = " ", conversationName = "")["result"])
        assertEquals("Query cannot be empty.", tools.searchMessages(query = " ", conversationName = "")["message"])
        assertEquals("error", tools.draftReply(conversationName = "Mom", messageText = " ")["result"])
        assertEquals("Message text cannot be empty.", tools.draftReply(conversationName = "Mom", messageText = " ")["message"])
        assertEquals("error", tools.markImportant(messageEventId = " ")["result"])
        assertEquals("Message event id cannot be empty.", tools.markImportant(messageEventId = " ")["message"])
        assertTrue(actions.isEmpty())
    }
}

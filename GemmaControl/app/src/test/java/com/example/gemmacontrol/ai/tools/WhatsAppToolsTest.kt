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
}

package com.example.gemmacontrol.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplyActionAndroidTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        InMemoryActiveReplyActionRegistry.clear()
    }

    @Test
    fun testRegistry_DetectsReplyAction() {
        val intent = Intent("com.example.gemmacontrol.TEST_REPLY")
        intent.setClass(context, WhatsAppNotificationListener::class.java) // Make explicit to satisfy U+ security rules
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val remoteInput = RemoteInput.Builder("result_key")
            .setLabel("Reply Label")
            .build()

        val action = Notification.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply Text",
            pendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = Notification.Builder(context, "channel_id")
            .setContentTitle("Mom")
            .setContentText("Hello")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .addAction(action)
            .build()

        InMemoryActiveReplyActionRegistry.registerFromNotification(
            notificationKey = "test_key",
            packageName = "com.whatsapp",
            notification = notification,
            capturedAt = System.currentTimeMillis()
        )

        val handle = InMemoryActiveReplyActionRegistry.getReplyHandle("test_key")
        assertNotNull("Registry must detect reply-capable action", handle)
        assertEquals("com.whatsapp", handle?.packageName)
        assertEquals("test_key", handle?.notificationKey)
        assertNotNull(handle?.actionIntent)
        assertTrue(handle?.textRemoteInputs?.isNotEmpty() == true)
        assertEquals("result_key", handle?.textRemoteInputs?.first()?.resultKey)
    }

    @Test
    fun testExecutor_FailsWhenRegistryCleared() {
        val executor = ActiveNotificationReplyExecutor(InMemoryActiveReplyActionRegistry)
        val result = executor.sendConfirmedReply(context, "some_key", "hello")
        assertEquals(ReplySendResult.NoActiveReplyAction, result)
    }

    @Test
    fun testExecutor_RejectsBlankText() {
        val executor = ActiveNotificationReplyExecutor()
        
        // Blank text
        val result1 = executor.sendConfirmedReply(
            context = context,
            notificationKey = "test_key",
            text = "   "
        )
        assertEquals(ReplySendResult.EmptyText, result1)

        // Empty text
        val result2 = executor.sendConfirmedReply(
            context = context,
            notificationKey = "test_key",
            text = ""
        )
        assertEquals(ReplySendResult.EmptyText, result2)
    }

    @Test
    fun testExecutor_RejectsTooLongText() {
        val executor = ActiveNotificationReplyExecutor()
        val longText = "a".repeat(1001)
        val result = executor.sendConfirmedReply(
            context = context,
            notificationKey = "test_key",
            text = longText
        )
        assertEquals(ReplySendResult.FailedSafely, result)
    }

    @Test
    fun testExecutor_NoActiveReplyActionReturnsExpected() {
        val executor = ActiveNotificationReplyExecutor()
        val result = executor.sendConfirmedReply(
            context = context,
            notificationKey = "unknown_key",
            text = "valid reply"
        )
        assertEquals(ReplySendResult.NoActiveReplyAction, result)
    }
}
